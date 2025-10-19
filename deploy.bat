@echo off
setlocal

REM === CONFIGURATION ===
set SERVLET_API=lib\servlet-api.jar
set SRC=src
set OUT=out
set JAR=framework.jar
set TEST_DIR=..\test\WEB-INF\lib
set TEST_SRC=..\test\com\test
set TEST_OUT=..\test\WEB-INF\classes
set TOMCAT_HOME=C:\Tomcat\apache-tomcat-10.1.48

echo.
echo =====================================
echo === Compilation du framework ===
echo =====================================
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

REM === Étape 1 : compiler d’abord les annotations ===
echo.
echo --- Compilation des annotations ---
for %%f in ("%SRC%\com\monframework\annotations\*.java") do (
    echo Compilation de %%~nxf
    javac -cp "%SERVLET_API%" -d "%OUT%" "%%~f"
    if errorlevel 1 (
        echo Erreur de compilation des annotations !
        exit /b 1
    )
)

REM === Étape 2 : compiler le reste du framework ===
echo.
echo --- Compilation des autres classes ---
for %%f in ("%SRC%\com\monframework\*.java") do (
    echo Compilation de %%~nxf
    javac -cp "%SERVLET_API%;%OUT%" -d "%OUT%" "%%~f"
    if errorlevel 1 (
        echo Erreur de compilation du framework !
        exit /b 1
    )
)

REM === Étape 3 : créer le JAR ===
echo.
echo --- Création du JAR ---
if exist "%JAR%" del "%JAR%"
jar cvf "%JAR%" -C "%OUT%" .

REM === Étape 4 : copier le JAR dans le projet TEST ===
echo.
echo --- Copie du framework.jar dans TEST ---
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%"
copy /Y "%JAR%" "%TEST_DIR%"

REM === Étape 5 : compiler le projet TEST ===
echo.
echo =====================================
echo === Compilation du projet TEST ===
echo =====================================
if exist "%TEST_OUT%" rmdir /s /q "%TEST_OUT%"
mkdir "%TEST_OUT%"

for %%f in ("%TEST_SRC%\*.java") do (
    echo Compilation de %%~nxf
    javac -cp "%SERVLET_API%;%OUT%;%TEST_DIR%\framework.jar" -d "%TEST_OUT%" "%%~f"
    if errorlevel 1 (
        echo Erreur de compilation dans le projet TEST !
        exit /b 1
    )
)

REM === Étape 6 : déploiement dans Tomcat ===
echo.
echo =====================================
echo === Déploiement dans Tomcat ===
echo =====================================
xcopy /E /I /Y "..\test" "%TOMCAT_HOME%\webapps\test" >nul

echo.
echo === Déploiement terminé ! ===
echo.
echo Démarre Tomcat, puis ouvre :
echo http://localhost:8080/test
echo.
endlocal
pause
