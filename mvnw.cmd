@echo off
@setlocal

if "%JAVA_HOME%"=="" (
  echo Error: JAVA_HOME not found in your environment. 1>&2
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Error: JAVA_HOME is set to an invalid directory: "%JAVA_HOME%" 1>&2
  exit /b 1
)

set "MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%"
if not "%MAVEN_PROJECTBASEDIR%"=="" goto endBaseDir

set "EXEC_DIR=%CD%"
set "WDIR=%EXEC_DIR%"
:findBaseDir
if exist "%WDIR%\.mvn" (
  set "MAVEN_PROJECTBASEDIR=%WDIR%"
  goto endBaseDir
)
cd ..
if "%WDIR%"=="%CD%" (
  set "MAVEN_PROJECTBASEDIR=%EXEC_DIR%"
  goto endBaseDir
)
set "WDIR=%CD%"
goto findBaseDir

:endBaseDir
cd "%EXEC_DIR%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"

for /F "tokens=1,2 delims==" %%A in ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") do (
  if "%%A"=="wrapperUrl" set "DOWNLOAD_URL=%%B"
)

if not exist "%WRAPPER_JAR%" (
  powershell -NoProfile -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%') }"
)

set "MAVEN_JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

"%MAVEN_JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
exit /b %ERRORLEVEL%
