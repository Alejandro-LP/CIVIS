# CIVIS Manual de uso y despliegue

Guía de demostración para el equipo del proyecto

Líder Jorge Alejandro López Páez

Esta guía permite iniciar el laboratorio, administrar ciudadanos en el portal web y operar la app Android. Utiliza registros ficticios y servicios locales. Conserva la misma contraseña de PostgreSQL al reiniciar una base ya creada.

## Instalación del laboratorio

Requisitos. JDK 26, Maven 3.9 o superior, Android Studio con SDK 36, Docker Desktop activo y un teléfono Android 8 o superior. El lector Google Code Scanner requiere Google Play Services. Para la base se utiliza PostgreSQL 17 en Docker y para correo Mailpit.

Abre PowerShell en la carpeta del proyecto. Configura JAVA_HOME hacia tu JDK 26 y DB_PASSWORD con una contraseña elegida por ti. En esta misma terminal inicia el laboratorio.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-26.0.1'
$env:DB_PASSWORD='<elige una contraseña local>'
.\iniciar-laboratorio.ps1
```

El script verifica Java, inicia los contenedores, compila el backend y abre el servidor en el puerto 8081. El proceso permanece en la terminal. Usa Ctrl C para detener el backend. Los contenedores permanecen activos hasta ejecutar docker compose stop.

El portal está en http://localhost:8081 y el buzón de correo en http://localhost:8025. PostgreSQL del contenedor se publica en localhost:5433, separado del puerto habitual de una instalación local. Para cambiar ese puerto configura DB_PORT antes de iniciar el script.

En una base nueva, Docker ejecuta database/01_schema.sql. En una base preexistente, el operador debe aplicar el esquema con psql antes de usar DDL_AUTO=validate. El perfil lab carga datos ficticios al iniciar. El script 02_demo_citizens.sql permite cargarlos explícitamente y conserva los registros existentes.

## Compilación y configuración alternativas

Compilación del backend desde fuentes.

```powershell
cd backend
mvn test package
cd ..
```

El JAR queda en backend/target/civis-api-2.0.0.jar. Para usar PostgreSQL y correo ya instalados, configura las variables antes de ejecutar iniciar-servidor.ps1. Usa los valores reales de tu laboratorio.

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5433/civis'
$env:DB_USER='civis'
$env:DB_PASSWORD='<contraseña de tu base>'
$env:SMTP_HOST='localhost'
$env:SMTP_PORT='1025'
$env:DDL_AUTO='validate'
.\iniciar-servidor.ps1 -Laboratorio -Puerto 8081
```

Las variables SMTP_USER, SMTP_PASSWORD, SMTP_AUTH, SMTP_TLS y SMTP_FROM permiten configurar un proveedor real de correo. El operador debe proporcionar credenciales autorizadas mediante el entorno.

Para una base de uso interno vacía, omite Laboratorio y configura CIVIS_ADMIN_ID, CIVIS_ADMIN_EMAIL y CIVIS_ADMIN_PASSWORD. La clave inicial exige al menos doce caracteres, mayúscula, minúscula y número. El administrador inicial se crea cuando no existen funcionarios. Desactivar el laboratorio conserva los datos existentes, por lo que se recomienda una base separada para cada entorno.

## Portal web para el administrador

Entra en http://localhost:8081. En laboratorio utiliza documento 1234567890 y contraseña Demo123. La sesión devuelve el perfil administrador. Una cuenta de funcionario utiliza Android y recibe rechazo al intentar las operaciones administrativas.

Resumen muestra los ciudadanos, los requeridos, los funcionarios y las consultas existentes. Ciudadanos permite buscar por documento o nombre y filtrar por estado. Pulsa Nuevo ciudadano para abrir la ficha. Completa los diez campos, selecciona el estado y guarda. Las fechas futuras y una expedición anterior al nacimiento se rechazan.

Editar conserva la identificación y permite actualizar los demás datos. Eliminar abre una confirmación con el documento del ciudadano. La eliminación es definitiva en el registro actual y debe ejecutarse solamente cuando se haya confirmado el caso correcto.

Autorizaciones prepara el registro de un funcionario. Documento, fuerza, rango, ID institucional y correo deben coincidir exactamente con el registro móvil. Suspender una autorización impide nuevos registros con esa autorización. Para retirar acceso a una cuenta ya creada usa Funcionarios y cambia su estado.

Funcionarios permite asignar ADMIN u OFFICER y activar o desactivar una cuenta. Cambiar permisos revoca sus sesiones. El portal impide al administrador modificar sus propios permisos. Auditoría lista fecha, funcionario, ciudadano y resultado con paginación.

Reportes PDF permite elegir todos, requeridos o no requeridos. Pulsa Descargar PDF y revisa la carpeta de descargas. El reporte incluye identificación, nombres, apellidos y estado, con encabezados repetidos y paginación.

## Registro y acceso desde Android

Abre en Android Studio únicamente la carpeta android, sincroniza Gradle y selecciona app. Compila o instala el APK de pruebas incluido. La compilación utiliza SDK 36 y genera bytecode JVM 17.

En la pantalla de acceso abre Configurar servidor API. En un teléfono dentro de la misma Wi Fi escribe http://IP_DEL_PC:8081. En el emulador usa http://10.0.2.2:8081. En una conexión USB se puede ejecutar adb reverse tcp:8081 tcp:8081 y usar http://127.0.0.1:8081. Esta conexión requiere el cable y el servidor activos.

Pulsa Comprobar conexión. El mensaje debe identificar el laboratorio. El administrador puede acceder con la cuenta demo. Para registrar al funcionario ficticio completa los siguientes datos.

| Campo | Ejemplo autorizado en laboratorio |
| --- | --- |
| Identificación | 1111111111 |
| Nombres y apellidos | Juan Carlos Pérez Gómez |
| Fuerza y rango | Policía y Patrullero |
| ID institucional | POL-101 |
| Correo | funcionario@civis.test |

Después del registro abre Mailpit en el PC. Copia la contraseña temporal de seis caracteres y entra con el documento del funcionario. Si ese documento ya existe, utiliza la recuperación. El buzón local captura mensajes de laboratorio y no entrega esos mensajes por Internet.

Recordar contraseña es una opción voluntaria para este dispositivo. Las credenciales se cifran con Android Keystore y permanecen asociadas al servidor configurado. Cerrar sesión y cambiar contraseña eliminan el contenido recordado. Si la clave cambia en el servidor, ingresa la nueva clave y vuelve a marcar la opción.

## Consulta y recuperación

Con una sesión activa, pulsa Consulta manual e introduce 1000000011 para un ciudadano ficticio no requerido o 1000000012 para un ciudadano ficticio requerido. Confirma el documento y consulta. El resultado incluye el nombre devuelto desde la base y el estado. Un documento ausente genera Sin datos.

Escanear documento abre Google Code Scanner. Apunta al código de barras de la cédula. Revisa el contenido leído y confirma el número antes de enviar la consulta, ya que PDF417 puede incluir varios datos. La aceptación física del lector requiere un ensayo con la cámara y un código compatible. Si el módulo no está disponible, utiliza la entrada manual.

Mis consultas muestra el historial del funcionario. Mi perfil permite revisar datos y cerrar sesión. La recuperación se solicita desde el acceso con el documento registrado y entrega una nueva clave por SMTP. Después de recuperar la cuenta, las sesiones anteriores pierden validez.

La opción Cambiar contraseña solicita la clave actual y una nueva de al menos diez caracteres, con mayúscula, minúscula y número. Después del cambio, inicia sesión con la nueva clave. Los cambios de rol o la desactivación realizada por un administrador también revocan sesiones.

## Operación y solución de problemas

| Síntoma | Causa a revisar | Acción |
| --- | --- | --- |
| No aparece app en Android Studio | Se abrió la carpeta padre | Abrir android y sincronizar Gradle |
| Error Java al iniciar el JAR | JAVA_HOME apunta a otra versión | Seleccionar JDK 26 |
| Error 404 al comprobar conexión | Otro servidor ocupa el puerto | Usar el puerto configurado de CIVIS |
| Error de conexión en el teléfono | IP, red, cable o backend | Verificar URL, Wi Fi y servidor |
| PostgreSQL rechaza contraseña | DB_PASSWORD diferente de la base | Usar la clave de la instancia creada |
| Registro devuelve 403 | Autorización ausente o diferente | Comparar los cinco datos institucionales |
| Registro devuelve 409 | Documento o correo ya registrado | Iniciar sesión o recuperar acceso |
| Registro devuelve 503 | SMTP no disponible | Revisar Mailpit y configuración de correo |
| Consulta administrativa devuelve 401 | Sesión revocada o expirada | Volver a iniciar sesión |
| Un funcionario recibe 403 en administración | Perfil sin permiso | Usar una cuenta ADMIN autorizada |

Para respaldo, usa pg_dump contra la base configurada y protege el archivo resultante. Para detener los contenedores conservando datos ejecuta docker compose stop. Eliminar un volumen borra datos y requiere una decisión explícita del operador.

Las pruebas repetibles están en tools/qa. verify_delivery.py requiere una base desechable en perfil lab y smtp_capture.py como receptor local. El archivo verificacion.json conserva resultados sin contraseñas ni tokens. Los mensajes capturados y las credenciales de prueba quedan excluidos de la entrega pública.
