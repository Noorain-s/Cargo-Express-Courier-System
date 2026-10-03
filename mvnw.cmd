@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

if exist "%WRAPPER_JAR%" goto runMaven

echo Downloading Maven Wrapper...
for /f "tokens=2 delims==" %%A in ('findstr "wrapperUrl" "%WRAPPER_PROPERTIES%"') do set WRAPPER_URL=%%A

powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"

if not exist "%WRAPPER_JAR%" (
  echo ERROR: Could not download Maven Wrapper jar. Check your internet connection.
  exit /b 1
)

:runMaven
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java
)

"%JAVA_EXE%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

endlocal
