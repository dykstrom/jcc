@echo off

setlocal

set SCRIPT_DIR=%~dp0

set FASM_BIN=%SCRIPT_DIR%..\fasm
set FASM_CMD=%FASM_BIN%\FASM.EXE
set INCLUDE=%FASM_BIN%\INCLUDE

set JCC_LIB=%SCRIPT_DIR%..\lib
set JCC_JAR=%JCC_LIB%\jcc-compiler-${project.version}.jar
set JAVA_ARGS=-jar %JCC_JAR% -assembler %FASM_CMD% --library-path %SCRIPT_DIR%

if "%JAVA_HOME%" == "" goto use_path
if not exist "%JAVA_HOME%\bin\java.exe" goto use_path

:use_java_home
set JAVA_CMD=%JAVA_HOME%\bin\java.exe
goto run_jcc

:use_path
set JAVA_CMD=java.exe

:run_jcc
%JAVA_CMD% %JAVA_ARGS% %*
