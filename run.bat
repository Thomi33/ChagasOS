@echo off
rem CHAGASOS - compilar y ejecutar (Windows)
cd /d %~dp0
if not exist out mkdir out
dir /s /b src\*.java > out\sources.txt
javac -encoding UTF-8 -cp lib\jlayer.jar -d out @out\sources.txt
if errorlevel 1 (
  echo Error compilando. Revisa que tengas instalado el JDK.
  pause
  exit /b 1
)
echo ChagasOS compilado con exito.
java -cp "out;lib\jlayer.jar" chagasos.ChagasOS
pause
