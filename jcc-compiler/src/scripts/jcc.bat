@echo off

setlocal

set SCRIPT_DIR=%~dp0

set JCC_LIB=%SCRIPT_DIR%..\lib
set JCC_JAR=%JCC_LIB%\jcc-compiler-${project.version}.jar
set JAVA_ARGS=-jar %JCC_JAR% --library-path %SCRIPT_DIR%
set JAVA_CMD=%SCRIPT_DIR%..\runtime\bin\java.exe

"%JAVA_CMD%" %JAVA_ARGS% %*
