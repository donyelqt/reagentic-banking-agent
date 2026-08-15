@echo off
REM Self-bootstrapping Maven Wrapper.
REM Uses `mvn` from PATH if present, otherwise downloads Apache Maven 3.9.9
REM into .mvn\wrapper and uses that. No global Maven install required.
setlocal
set MVNW_DIR=%~dp0
set WRAP=%MVNW_DIR%.mvn\wrapper
set MVN_DIR=%WRAP%\apache-maven-3.9.9
set MVN_EXE=%MVN_DIR%\bin\mvn.cmd
set DIST=https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  call mvn %*
  exit /b %ERRORLEVEL%
)

if not exist "%MVN_EXE%" (
  echo Downloading Maven 3.9.9 into %WRAP% ...
  if not exist "%WRAP%" mkdir "%WRAP%"
  powershell -NoProfile -Command "Invoke-WebRequest -Uri '%DIST%' -OutFile '%WRAP%\mvn.zip'; Expand-Archive -Path '%WRAP%\mvn.zip' -DestinationPath '%WRAP%' -Force; Remove-Item '%WRAP%\mvn.zip'"
)

call "%MVN_EXE%" %*
exit /b %ERRORLEVEL%
