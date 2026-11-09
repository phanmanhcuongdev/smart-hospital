@echo off
setlocal
set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.11"
set "MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip' -OutFile '%BASE_DIR%.mvn\maven.zip'; Expand-Archive -Force '%BASE_DIR%.mvn\maven.zip' '%BASE_DIR%.mvn'; Remove-Item '%BASE_DIR%.mvn\maven.zip'"
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
