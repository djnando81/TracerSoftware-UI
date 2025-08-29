package com.tracersoftware.bootstrap;

import com.tracersoftware.apiinfo.ApiService;

public class BootstrapManager {
    private final ApiService apiService;

    public BootstrapManager(ApiService apiService) {
        this.apiService = apiService;
    }

    public void initializeDatabase() {
        // Aquí iría la lógica para crear la base de datos y cargar datos demo si es necesario
        // Ejemplo: apiService.createDatabase(); apiService.installBootstrap();
    }
}
