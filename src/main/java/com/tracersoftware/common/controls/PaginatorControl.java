package com.tracersoftware.common.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * PaginatorControl
 *
 * Reusable, stylesheetable JavaFX control for paging list results.
 * Exposes JavaFX properties so parent controllers can bind or listen for changes.
 *
 * Public API (examples):
 *  - pageIndexProperty(), pageSizeProperty(), totalItemsProperty()
 *  - setTotalItems(int)
 *  - setOnPageChanged(ChangeListener&lt;Number&gt;) to be notified when page or size changes
 */
public class PaginatorControl extends HBox {

    @FXML private ComboBox<Integer> cboPageSize;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;

    private final SimpleIntegerProperty pageIndex = new SimpleIntegerProperty(this, "pageIndex", 0);
    private final SimpleIntegerProperty pageSize = new SimpleIntegerProperty(this, "pageSize", 10);
    private final ReadOnlyIntegerWrapper totalItems = new ReadOnlyIntegerWrapper(this, "totalItems", 0);

    // Optional listener for parent convenience
    private final SimpleObjectProperty<ChangeListener<Number>> onPageChanged = new SimpleObjectProperty<>();

    public PaginatorControl() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/components/paginator.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
            // load default stylesheet for paginator if present
            try {
                String css = getClass().getResource("/com/tracersoftware/common/controls/paginator.css").toExternalForm();
                if (!getStylesheets().contains(css)) getStylesheets().add(css);
            } catch (Exception ignored) {}
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    public void initialize() {
        cboPageSize.getItems().addAll(5,10,25,50,100);
        cboPageSize.getSelectionModel().select(Integer.valueOf(pageSize.get()));
        cboPageSize.setOnAction(e -> {
            pageSize.set(cboPageSize.getValue());
            pageIndex.set(0);
            firePageChanged();
        });
        btnPrev.setOnAction(e -> {
            if (pageIndex.get() > 0) {
                pageIndex.set(pageIndex.get()-1);
                firePageChanged();
            }
        });
        btnNext.setOnAction(e -> {
            int pages = getPageCount();
            if (pageIndex.get() < pages-1) {
                pageIndex.set(pageIndex.get()+1);
                firePageChanged();
            }
        });

        // Enable/disable prev/next depending on page/total
        ChangeListener<Number> enableDisable = (obs, o, n) -> {
            int pages = getPageCount();
            btnPrev.setDisable(pageIndex.get() <= 0);
            btnNext.setDisable(pageIndex.get() >= pages - 1);
        };
        pageIndex.addListener(enableDisable);
        totalItems.addListener((obs,o,n) -> enableDisable.changed(null,null, null));

        // update label when properties change
        ChangeListener<Number> updater = (obs, o, n) -> updateLabel();
        pageIndex.addListener(updater);
        pageSize.addListener(updater);
        totalItems.addListener((obs, o, n) -> updateLabel());
    }

    private void updateLabel() {
        int pages = getPageCount();
        int cur = Math.max(0, Math.min(pageIndex.get(), pages - 1));
        lblPage.setText((cur + 1) + " / " + pages);
    }

    private void firePageChanged() {
        updateLabel();
        ChangeListener<Number> l = onPageChanged.get();
        if (l != null) {
            // notify with pageIndex (first argument) and pageSize (second via binding style)
            l.changed(pageIndex, null, pageIndex.get());
            l.changed(pageSize, null, pageSize.get());
        }
    }

    public IntegerProperty pageIndexProperty() { return pageIndex; }
    public IntegerProperty pageSizeProperty() { return pageSize; }
    public javafx.beans.property.ReadOnlyIntegerProperty totalItemsProperty() { return totalItems.getReadOnlyProperty(); }

    public int getPageIndex() { return pageIndex.get(); }
    public void setPageIndex(int idx) { this.pageIndex.set(idx); }
    public int getPageSize() { return pageSize.get(); }
    public void setPageSize(int size) { this.pageSize.set(size); cboPageSize.getSelectionModel().select(Integer.valueOf(size)); }

    public int getTotalItems() { return totalItems.get(); }
    public void setTotalItems(int total) { this.totalItems.set(total); updateLabel(); }

    public int getPageCount() {
        int size = Math.max(1, pageSize.get());
        return Math.max(1, (int)Math.ceil((double)totalItems.get() / size));
    }

    /**
     * Convenience: allow parent to be notified when page or size change.
     * Parent can also bind to pageIndex/pageSize properties directly.
     */
    public void setOnPageChanged(ChangeListener<Number> listener) { this.onPageChanged.set(listener); }
    public ChangeListener<Number> getOnPageChanged() { return this.onPageChanged.get(); }
}
