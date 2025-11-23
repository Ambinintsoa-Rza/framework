@echo off
setlocal enabledelayedexpansion

REM =============================================
REM       DEPLOY.BAT - VERSION FINALE & SOLIDE
REM =============================================
set "SERVLET_API=lib\servlet-api.jar"
set "SRC=src"
set "OUT=out"
set "JAR=framework.jar"
set "TEST_DIR=..\test\WEB-INF\lib"
set "TEST_SRC=..\test\com\test"
set "TEST_OUT=..\test\WEB-INF\classes"
set "TOMCAT_HOME=C:\Tomcat\apache-tomcat-10.1.48"

echo.
echo ================================================
echo       COMPILATION & DEPLOIEMENT FRAMEWORK
echo ================================================

REM ------------------------------------------------
REM 1. Compilation du framework (tous les packages)
REM ------------------------------------------------
echo.
echo --- Nettoyage et création du dossier out ---
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo.
echo --- Compilation du framework (récursif) ---
set "SOURCES="
for /r "%SRC%\com\monframework" %%F in (*.java) do (
    set "SOURCES=!SOURCES! "%%F""
)

if "!SOURCES!"=="" (
    echo [ERREUR] Aucun fichier .java trouvé dans %SRC%\com\monframework\
    pause
    exit /b 1
)

javac -encoding UTF-8 -cp "%SERVLET_API%" -d "%OUT%" !SOURCES!
if errorlevel 1 (
    echo.
    echo [ERREUR] Échec de la compilation du framework !
    pause
    exit /b 1
)
echo Framework compilé avec succès !

REM ------------------------------------------------
REM 2. Création du JAR
REM ------------------------------------------------
echo.
echo --- Création de framework.jar ---
if exist "%JAR%" del "%JAR%"
jar cvf "%JAR%" -C "%OUT%" . >nul
echo framework.jar créé

REM ------------------------------------------------
REM 3. Copie du JAR dans le projet test
REM ------------------------------------------------
echo.
echo --- Copie du JAR dans le projet test ---
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%"
copy /Y "%JAR%" "%TEST_DIR%" >nul
echo framework.jar copié dans ..\test\WEB-INF\lib

REM ------------------------------------------------
REM 4. Compilation du projet TEST
REM ------------------------------------------------
echo.
echo ================================================
echo       COMPILATION DU PROJET TEST
echo ================================================
if exist "%TEST_OUT%" rmdir /s /q "%TEST_OUT%"
mkdir "%TEST_OUT%"

set "TEST_SOURCES="
for %%F in ("%TEST_SRC%\*.java") do (
    set "TEST_SOURCES=!TEST_SOURCES! "%%F""
)

if "!TEST_SOURCES!"=="" (
    echo [INFO] Aucun contrôleur trouvé dans %TEST_SRC%
    echo       (c'est normal si tu n'en as pas encore créé)
    echo.
) else (
    echo Compilation des contrôleurs...
    javac -encoding UTF-8 -cp "%SERVLET_API%;%TEST_DIR%\framework.jar" -d "%TEST_OUT%" !TEST_SOURCES!
    if errorlevel 1 (
        echo.
        echo [ERREUR] Échec de la compilation du projet test !
        pause
        exit /b 1
    )
    echo Projet test compilé avec succès !
)

REM ------------------------------------------------
REM 5. Déploiement dans Tomcat
REM ------------------------------------------------
echo.
echo ================================================
echo           DEPLOIEMENT SUR TOMCAT
echo ================================================
echo Suppression de l'ancienne application...
if exist "%TOMCAT_HOME%\webapps\test" rmdir /s /q "%TOMCAT_HOME%\webapps\test"
if exist "%TOMCAT_HOME%\webapps\test.war" del "%TOMCAT_HOME%\webapps\test.war"

echo Copie des fichiers dans Tomcat...
xcopy /E /I /Y "..\test" "%TOMCAT_HOME%\webapps\test" >nul

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║                                                  ║
echo ║     TOUT EST PRÊT - DÉPLOIEMENT TERMINÉ !     ║
echo ║                                                  ║
echo ╚══════════════════════════════════════════════════╝
echo.
echo Étapes suivantes :
echo   1. Démarre Tomcat → %TOMCAT_HOME%\bin\startup.bat
echo   2. Ouvre ton navigateur :
echo          http://localhost:8080/test
echo.
echo Exemples à tester après avoir créé un contrôleur :
echo   → http://localhost:8080/test/salama
echo   → http://localhost:8080/test/texte
echo.
echo Appuie sur une touche pour fermer cette fenêtre...
pause
endlocal