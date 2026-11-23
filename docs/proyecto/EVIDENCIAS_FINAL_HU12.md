# CIVIS — Evidencias finales HU12

## Pruebas automatizadas
La acción `CIVIS - Verificacion HU12` tiene tres trabajos independientes:
1. Backend: pruebas JUnit contra H2. No sustituye PostgreSQL.
2. Integración PostgreSQL: ejecuta Spring Boot sobre PostgreSQL 17, crea un registro ficticio, verifica consulta privada, cambio de estado, auditoría e historial. Publica `hu12-postgresql.json` y el log del servidor.
3. Android: compilación de APK debug y Android Lint. No representa una prueba en teléfono.

Las evidencias están en el apartado **Artifacts** de cada ejecución aprobada de GitHub Actions. No se deben declarar casos aprobados hasta ver los registros de la ejecución correspondiente.

## Prueba manual en teléfono (pendiente de ejecución física)
1. En el PC: iniciar PostgreSQL, Mailpit y backend siguiendo el README. Abrir `http://localhost:8081/api/health` y verificar que muestre `UP`.
2. Abrir portal web `http://localhost:8081`; iniciar sesión con datos académicos ficticios en modo laboratorio. Crear un ciudadano ficticio con estado `Requerido`. Guardar captura **WEB-01**.
3. Conectar teléfono Android a la misma red y establecer `http://IP_DEL_PC:8081` en Configurar servidor API; nunca usar `10.0.2.2` en un teléfono físico. Guardar captura **MOV-01** de comprobación de conexión.
4. Iniciar sesión en la app y consultar el documento ficticio; la pantalla debe mostrar solo `Requerido`, sin nombre ni cédula. Guardar **MOV-02**.
5. Cambiar estado en portal a `No requerido`; guardar **WEB-02**. Repetir consulta en Android, guardar **MOV-03**. Revisar la sección Historial de estados y Auditoría en web, guardar **WEB-03** y **WEB-04**.
6. Comprobar documento inexistente, sesión inválida y permiso de funcionario. Guardar resultados reales con fecha, teléfono, versión del APK y resultado.
7. Escanear cédula **ficticia/de pruebas** con PDF417 si se cuenta con un documento de muestra; registrar compatibilidad del escáner. No utilizar cédulas de terceros.

| Caso | Evidencia requerida | Estado |
|---|---|---|
| Registro en web | WEB-01 | Pendiente dispositivo y entorno |
| Android conexión + login | MOV-01 | Pendiente dispositivo |
| Consulta requerida sin PII | MOV-02 | Pendiente dispositivo |
| Cambio en web | WEB-02 | Pendiente dispositivo y entorno |
| Consulta actualizada | MOV-03 | Pendiente dispositivo |
| Auditoría e historial | WEB-03 / WEB-04 | Pendiente entorno |
| Cámara PDF417 | MOV-04 | Pendiente teléfono |
| HTTPS y proveedor institucional real | No forma parte del laboratorio; pendiente si la rúbrica lo exige | No verificado |

## Criterios de cierre
No fusionar una rama si su comprobación CI falla. No usar informes o capturas de noviembre previos como evidencia de esta versión sin aclarar la fecha y commit. El sistema representa estados almacenados internamente, no certificados judiciales oficiales.
