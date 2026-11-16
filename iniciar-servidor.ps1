param([switch]$Laboratorio)
$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot 'backend\target\civis-api-2.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Primero compila el servidor con Maven: mvn package' }
$javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
if ($Laboratorio) {
    & $javaCommand -jar $jar '--spring.profiles.active=lab'
} else {
    & $javaCommand -jar $jar
}
exit $LASTEXITCODE
