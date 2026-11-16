# CIVIS - prepara el Gradle Wrapper oficial 9.4.1. Ejecutar en PowerShell.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$base = 'https://raw.githubusercontent.com/gradle/gradle/v9.4.1'
$targets = @(
  @{Url="$base/gradlew"; Path='gradlew'},
  @{Url="$base/gradlew.bat"; Path='gradlew.bat'},
  @{Url="$base/gradle/wrapper/gradle-wrapper.jar"; Path='gradle/wrapper/gradle-wrapper.jar'}
)
foreach ($target in $targets) {
  $destination = Join-Path $root $target.Path
  Write-Host "Descargando $($target.Path) desde Gradle oficial..."
  Invoke-WebRequest -Uri $target.Url -OutFile $destination -UseBasicParsing
  if ((Get-Item $destination).Length -lt 1000) { throw "Descarga incompleta: $destination" }
}
Write-Host "Wrapper 9.4.1 disponible. Ejecuta .\gradlew.bat --version y verifica JVM 26." -ForegroundColor Green
