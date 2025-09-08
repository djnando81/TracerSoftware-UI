package com.tracersoftware.common.controls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracersoftware.api.ApiClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Reusable export bar with Excel/CSV/TXT/PDF actions that posts items to
 * /api/Export/universal/{format} and saves returned bytes.
 *
 * Supports:
 *  - Solo seleccionados (checkbox)
 *  - Todo (servidor) vía un callback opcional para obtener todas las páginas
 */
public class ExportBar extends HBox {
    private final Button btnExcel = new Button("Excel");
    private final Button btnCsv = new Button("CSV");
    private final Button btnTxt = new Button("TXT");
    private final Button btnPdf = new Button("PDF");
    private final javafx.scene.control.CheckBox chkOnlySelected = new javafx.scene.control.CheckBox("Solo seleccionados");
    private final javafx.scene.control.CheckBox chkAllServer = new javafx.scene.control.CheckBox("Todo (servidor)");
    private final TableView<?> table;
    private final String baseName;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiClient api = new ApiClient();
    private final Callable<List<?>> fetchAllCallback; // may be null

    public ExportBar(TableView<?> table, String fileBaseName) {
        this(table, fileBaseName, null);
    }

    public ExportBar(TableView<?> table, String fileBaseName, Callable<List<?>> fetchAllCallback) {
        this.table = table;
        this.baseName = (fileBaseName == null || fileBaseName.isBlank()) ? "export" : fileBaseName;
        this.fetchAllCallback = fetchAllCallback;
        getChildren().addAll(chkOnlySelected, chkAllServer, btnExcel, btnCsv, btnTxt, btnPdf);
        setSpacing(6);
        setPadding(new Insets(0, 0, 0, 0));
        try { getStyleClass().add("export-bar"); } catch (Exception ignored) {}
        styleButtons();
        wireActions();
    }

    private void styleButtons() {
        btnExcel.getStyleClass().addAll("button-green", "btn-small");
        btnCsv.getStyleClass().addAll("button-blue", "btn-small");
        btnTxt.getStyleClass().addAll("button-gray", "btn-small");
        btnPdf.getStyleClass().addAll("button-purple", "btn-small");
        try { chkOnlySelected.getStyleClass().add("btn-small"); } catch (Exception ignored) {}
        try { chkAllServer.getStyleClass().add("btn-small"); } catch (Exception ignored) {}
        btnExcel.setTooltip(new Tooltip("Exportar a Excel"));
        btnCsv.setTooltip(new Tooltip("Exportar a CSV"));
        btnTxt.setTooltip(new Tooltip("Exportar a TXT (CSV)"));
        btnPdf.setTooltip(new Tooltip("Exportar a PDF"));
        chkOnlySelected.setTooltip(new Tooltip("Exporta solo las filas seleccionadas"));
        chkAllServer.setTooltip(new Tooltip("Descarga todos los registros desde el servidor"));
    }

    private void wireActions() {
        btnExcel.setOnAction(e -> export("excel", ".xlsx"));
        btnCsv.setOnAction(e -> export("csv", ".csv"));
        // TXT: usa CSV del backend, solo renombra la extensión
        btnTxt.setOnAction(e -> export("csv", ".txt"));
        btnPdf.setOnAction(e -> export("pdf", ".pdf"));
    }

    private void export(String format, String ext) {
        try {
            if (chkOnlySelected.isSelected() && chkAllServer.isSelected()) {
                MessageToast.show(null, "Seleccione 'Solo seleccionados' o 'Todo (servidor)', no ambos.", MessageToast.ToastType.WARNING);
                return;
            }

            List<?> items = table.getItems();
            boolean serverAll = chkAllServer.isSelected();
            if (serverAll) {
                if (fetchAllCallback == null) {
                    MessageToast.show(null, "No hay proveedor para 'Todo (servidor)'.", MessageToast.ToastType.ERROR);
                    return;
                }
                // se obtendrá dentro del Task
            } else if (chkOnlySelected.isSelected()) {
                var selModel = table.getSelectionModel();
                try { selModel.setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE); } catch (Exception ignored) {}
                List<?> sel = selModel.getSelectedItems();
                if (sel == null || sel.isEmpty()) {
                    MessageToast.show(null, "Seleccione al menos una fila", MessageToast.ToastType.INFO);
                    return;
                }
                items = List.copyOf(sel);
            } else {
                if (items == null || items.isEmpty()) {
                    MessageToast.show(null, "No hay datos para exportar", MessageToast.ToastType.INFO);
                    return;
                }
            }

            String suffix = chkOnlySelected.isSelected() ? "-seleccion" : "";
            String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyyHHmm"));
            String suggested = (baseName + suffix + ts + ext).replaceAll("[^A-Za-z0-9_.-]", "");
            String title = Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
            String qs = "?fileName=" + URLEncoder.encode(suggested, StandardCharsets.UTF_8);
            if ("excel".equalsIgnoreCase(format)) {
                qs += "&sheetName=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
            }
            qs += "&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
            String path = "/api/Export/universal/" + format + qs;

            Window win = table.getScene() != null ? table.getScene().getWindow() : null;
            FileChooser fc = new FileChooser();
            fc.setInitialFileName(suggested);
            if (".xlsx".equalsIgnoreCase(ext)) fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            else if (".csv".equalsIgnoreCase(ext)) fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
            else if (".pdf".equalsIgnoreCase(ext)) fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            else fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
            File out = fc.showSaveDialog(win);
            if (out == null) return;

            final boolean serverAllFinal = serverAll;
            final List<?> itemsFinal = items;
            final String pathFinal = path;
            final File outFinal = out;
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<?> toSend = serverAllFinal && fetchAllCallback != null ? fetchAllCallback.call() : itemsFinal;
                    String json = mapper.writeValueAsString(toSend);
                    byte[] bytes = api.postBinary(pathFinal, json);
                    try (FileOutputStream fos = new FileOutputStream(outFinal)) { fos.write(bytes); }
                    return null;
                }
            };
            task.setOnSucceeded(ev -> MessageToast.show(null, "Exportación generada", MessageToast.ToastType.SUCCESS));
            task.setOnFailed(ev -> {
                Throwable ex = task.getException(); if (ex != null) ex.printStackTrace();
                MessageToast.show(null, "Error exportando: " + (ex == null ? "desconocido" : ex.getMessage()), MessageToast.ToastType.ERROR);
            });
            new Thread(task, "export-task").start();
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> MessageToast.show(null, "Error preparando exportación: " + ex.getMessage(), MessageToast.ToastType.ERROR));
        }
    }
}
