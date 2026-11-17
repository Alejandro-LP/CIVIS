param([ValidateRange(1024,65535)][int]$Puerto = 8081, [switch]$OmitirCompilacion)
$ErrorActionPreference = 'Stop'
if (-not $env:DB_PASSWORD) {
    throw 'Configura $env:DB_PASSWORD con una contraseña propia para la base del laboratorio. Usa la misma al volver a iniciar una base ya creada.'
}
$javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$javaVersion = (& $javaCommand -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "26(?:\.|[-"])') { throw 'El backend requiere JDK 26. Configura JAVA_HOME y vuelve a ejecutar.' }
Push-Location $PSScriptRoot
try {
    & docker compose up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Docker no pudo iniciar PostgreSQL y Mailpit. Revisa Docker Desktop y los puertos configurados.' }
    $databasePort = if ($env:DB_PORT) { $env:DB_PORT } else { '5433' }
    $env:DB_URL = "jdbc:postgresql://localhost:$databasePort/civis"
    $env:DB_USER = 'civis'
    $env:SMTP_HOST = 'localhost'
    $env:SMTP_PORT = '1025'
    $env:SMTP_AUTH = 'false'
    $env:SMTP_TLS = 'false'
    $env:DDL_AUTO = 'validate'
    if (-not $OmitirCompilacion) {
        Push-Location backend
        try {
            & mvn test package
            if ($LASTEXITCODE -ne 0) { throw 'La compilación del backend falló.' }
        } finally { Pop-Location }
    }
    Write-Host "Portal web http://localhost:$Puerto"
    Write-Host 'Correo de laboratorio http://localhost:8025'
    & "$PSScriptRoot/iniciar-servidor.ps1" -Laboratorio -Puerto $Puerto
} finally { Pop-Location }
