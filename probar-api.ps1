# Ejecutar después de levantar PostgreSQL/Mailpit y el backend.
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
Write-Host '1/4 Salud del servidor'
Invoke-RestMethod "$base/api/health" | Format-List
Write-Host '2/4 Inicio de sesión con cuenta DEMO'
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body '{"identification":"1234567890","password":"Demo123"}'
$headers = @{Authorization = "Bearer $($login.token)"}
Write-Host "Autenticado: $($login.officer.firstName) $($login.officer.lastName)"
Write-Host '3/4 Consulta de ciudadano no requerido'
Invoke-RestMethod -Headers $headers "$base/api/citizens/1000000001/status" | Format-List
Write-Host '4/4 Consulta de ciudadano requerido'
Invoke-RestMethod -Headers $headers "$base/api/citizens/1000000002/status" | Format-List
Write-Host 'Pruebas de HTTP completadas. TODOS LOS DATOS SON FICTICIOS.' -ForegroundColor Green
