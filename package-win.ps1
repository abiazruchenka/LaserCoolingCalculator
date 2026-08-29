# Builds a Windows app folder with LaserCooling.exe and a bundled Java runtime.
# Must be run on Windows (x64). jpackage cannot cross-compile from macOS.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if ($env:OS -ne "Windows_NT") {
    Write-Error "Сборку .exe нужно делать на Windows, не на Mac."
}

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\jpackage.exe"))) {
    Write-Error "Нужен JDK 21+ с jpackage. Установите Temurin и задайте JAVA_HOME."
}

$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

$AppName = "LaserCooling"
$Version = "1.0"
$RuntimeImage = "target\app"
$Dest = "target\dist"

Write-Host "JDK: $env:JAVA_HOME"
& java -version

& .\mvnw.cmd -DskipTests javafx:jlink
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Test-Path $RuntimeImage)) {
    Write-Error "jlink не создал $RuntimeImage"
}

if (Test-Path $Dest) {
    Remove-Item -Recurse -Force $Dest
}
New-Item -ItemType Directory -Path $Dest | Out-Null

& jpackage `
    --type app-image `
    --name $AppName `
    --app-version $Version `
    --vendor IPG `
    --dest $Dest `
    --runtime-image $RuntimeImage `
    --module ipg.cooling/ipg.cooling.Launcher

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Zip = Join-Path $Dest "$AppName-$Version-windows-x64.zip"
if (Test-Path $Zip) {
    Remove-Item -Force $Zip
}
Compress-Archive -Path (Join-Path $Dest $AppName) -DestinationPath $Zip

Write-Host ""
Write-Host "Готово:"
Write-Host "  $Dest\$AppName\$AppName.exe"
Write-Host "  $Zip"
Write-Host ""
Write-Host "Нужна вся папка $AppName, не один .exe."
Write-Host "Первый запуск: Windows может показать SmartScreen → Подробнее → Выполнить в любом случае."
