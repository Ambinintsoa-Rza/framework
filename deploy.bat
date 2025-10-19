@echo off
setlocal

REM === CONFIGURATION ===
set SERVLET_API=lib\servlet-api.jar
set SRC=src
set OUT=out
set JAR=framework.jar
set TEST_DIR=..\test\WEB-INF\lib
set TOMCAT_HOME=C:\Tomcat\apache-tomcat-10.1.48

echo.
echo === Compilation du framework ===
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

javac -cp "%SERVLET_API%" -d "%OUT%" "%SRC%\com\monframework\FrontServlet.java"
if errorlevel 1 (
    echo Erreur de compilation !
    exit /b 1
)

echo.
echo === Création du JAR ===
if exist "%JAR%" del "%JAR%"
jar cvf "%JAR%" -C "%OUT%" .

echo.
echo === Copie du JAR dans le projet test ===
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%"
copy /Y "%JAR%" "%TEST_DIR%"

echo.
echo === Déploiement dans Tomcat ===
xcopy /E /I /Y "..\test" "%TOMCAT_HOME%\webapps\test"

echo.
echo === Déploiement terminé ! ===
endlocal
pause
