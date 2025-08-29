@echo off
setlocal enabledelayedexpansion
set BASEDIR=%~dp0
cd /d %BASEDIR%
echo =============================================
echo  TracerSoftwareApp - Build & Run Utility
echo =============================================
echo.
REM 1. Limpiar proyecto
echo [1/3] Limpiando proyecto...
call mvn clean
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Error al limpiar el proyecto. Abortando.
    pause
    exit /b %ERRORLEVEL%
)
echo.
REM 2. Compilar y empaquetar proyecto (si falla, elimina target)
echo [2/3] Compilando y empaquetando proyecto...
call mvn package
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Error en la compilacion. Eliminando carpeta target...
    rmdir /s /q target
    pause
    exit /b %ERRORLEVEL%
)
echo.
REM 3. Ejecutar la aplicación con JavaFX correctamente configurado
echo [3/3] Ejecutando la aplicación con mvn javafx:run ...
call mvn javafx:run
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Error al ejecutar la aplicación con JavaFX.
    pause
    exit /b %ERRORLEVEL%
)
echo.
echo Todas las tareas finalizadas. Puedes cerrar esta ventana.
pause
