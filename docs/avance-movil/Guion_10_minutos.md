# Avance de la app móvil CIVIS

Exposición de diez minutos del 17 de noviembre de 2026.

Integrantes Jorge Alejandro López Páez, David Santiago Guerrero Tolosa, Daniel Felipe Bejarano Perez y Juan Sebastian Romo Sanabria. Los cargos del equipo están pendientes de acordarse.

## Orden de la exposición

### De 0 a 45 segundos Presentación

«Hoy presentamos el avance de la aplicación móvil CIVIS. El equipo está conformado por Jorge Alejandro López Páez, David Santiago Guerrero Tolosa, Daniel Felipe Bejarano Perez y Juan Sebastian Romo Sanabria. Vamos a mostrar las pantallas implementadas y el recorrido de un funcionario».

### De 45 segundos a 1 minuto 30 Alcance del avance

«La app organiza el acceso del funcionario y la consulta de un ciudadano desde Android. Ya tenemos las pantallas de registro, login, recuperación, panel principal y consulta. En esta demostración utilizamos cuentas y registros ficticios».

### De 1 minuto 30 a 3 minutos Registro

Abre Registrar funcionario y recorre los siete campos. «Solicitamos identificación, nombres, apellidos, fuerza, rango, ID institucional y correo. Las opciones de rango cambian según la fuerza seleccionada. El formulario comprueba que los datos obligatorios estén completos. El registro exige una autorización previa y entrega una contraseña temporal por correo cuando los datos coinciden».

Para el avance basta mostrar los campos, seleccionar una fuerza y su rango y explicar el recorrido. Si vas a enviar el registro, utiliza una autorización ficticia disponible y comprueba antes que el documento todavía no esté registrado. Así evitas detener la exposición por un duplicado.

### De 3 a 4 minutos 15 Inicio de sesión

«El funcionario ingresa su identificación y su contraseña. La opción de recordar contraseña guarda las credenciales cifradas en el dispositivo. Ya verificamos que la app puede recuperarlas después de reiniciarse. También presentamos mensajes para errores de datos o conexión».

Entra con la cuenta de laboratorio 1234567890 y Demo123. Muestra el saludo Hola Andrea. La cuenta utilizada para el ensayo tiene permisos de administrador. En esta exposición recorre las opciones de consulta y perfil del móvil.

### De 4 minutos 15 a 7 minutos 15 Consulta en vivo

1. En el panel desplázate hasta Consulta manual.
2. Introduce 1000000011, confirma y pulsa Consultar estado. Muestra el nombre y No requerido.
3. Pulsa Nueva consulta e introduce 1000000012. Muestra Requerido.
4. Explica que un documento ausente devuelve Sin datos.

«La consulta solicita confirmar el documento antes de enviar la petición. El resultado muestra el nombre y el estado devueltos por el registro de pruebas. El móvil también incluye el acceso al lector de códigos. La comprobación física del escáner está programada como siguiente ensayo».

### De 7 minutos 15 a 8 minutos 15 Recuperación y perfil

Muestra el perfil y explica el cierre de sesión. Vuelve al acceso y abre Recuperar acceso. «El funcionario puede solicitar una nueva contraseña temporal usando su documento. El flujo de recuperación utiliza su correo registrado. Además, desde una sesión puede cambiar la contraseña».

### De 8 minutos 15 a 9 minutos 15 Implementación móvil

«La aplicación utiliza Kotlin y Jetpack Compose. Organiza el acceso, el registro y la consulta en pantallas con navegación de regreso. Incluye validación de campos, estados de carga y mensajes de error. Para recordar la contraseña utiliza AES GCM y Android Keystore. La conexión permite consultar los datos de prueba desde el teléfono».

### De 9 minutos 15 a 10 minutos Próximos pasos

«El siguiente paso es ensayar la lectura de un código compatible con la cámara, revisar la experiencia con más usuarios y repetir el recorrido completo de registro y recuperación desde el móvil. Este avance deja preparada la navegación y las funciones de acceso y consulta que acabamos de mostrar».

## Preparación antes de exponer

- Conecta y desbloquea el teléfono. Si usas USB, comprueba el cable y la depuración.
- Inicia el laboratorio y utiliza la URL que le corresponda. El laboratorio preparado en este equipo utiliza http://127.0.0.1:8082 con adb reverse tcp:8082 tcp:8082. Requiere que el PC y el servicio sigan encendidos.
- Con el script habitual iniciar-laboratorio.ps1, el puerto es 8081. Ajusta la URL del móvil y el reenvío USB al mismo puerto.
- Pulsa Comprobar conexión antes de empezar. Ensaya el login y las dos consultas ficticias.
- Deja abiertas las capturas o el PDF de diapositivas como respaldo si se interrumpe la conexión.
- Utiliza la consulta manual para el recorrido ensayado. Presenta el escáner como implementación pendiente de comprobación física.

## Respuestas breves para la revisión

¿Qué muestran hoy? El incremento de la aplicación móvil y su recorrido de registro, acceso, consulta y recuperación.

¿Qué se ha probado en el teléfono? El login con la cuenta de laboratorio, el almacenamiento cifrado y la restauración de credenciales después del reinicio. La evidencia está en docs/proyecto/verificacion_android.json.

¿Los datos son oficiales? La demostración utiliza registros ficticios y un padrón académico de autorizaciones.

¿El escáner ya está validado? Está integrado mediante Google Code Scanner. Falta completar el ensayo físico con la cámara y un código compatible.

¿Quién es el líder? Los cargos del equipo están pendientes de acordarse. Los cuatro integrantes presentan el avance.

¿Cómo se distribuye el trabajo? Describan los aportes reales de cada integrante. La presentación de hoy identifica al equipo sin asignar responsabilidades que todavía no hayan acordado.
