# CIVIS Guion del líder y sustentación

Preparación de Jorge Alejandro López Páez y equipo

Este documento propone un discurso y preguntas de ensayo para defender el proyecto ante el profesor Carlos Andrés López. La distribución de roles es académica. Cada integrante debe comprobar las respuestas en el código y ensayar la demostración con sus compañeros.

## Presentación ejecutiva de diez minutos

Minuto 0 a 1. Jorge abre la presentación. «Somos el equipo CIVIS. Presentamos una solución para administrar la información ciudadana desde la web y facilitar su consulta por funcionarios desde Android. Como líder del proyecto, coordiné la propuesta de alcance y la integración que vamos a demostrar».

Minuto 1 a 2. Jorge explica el problema y el valor. «La entidad necesita un registro común y un proceso de consulta trazable. CIVIS permite mantener la ficha completa, habilitar al personal y conservar quién consultó cada documento. Nuestra propuesta para esta fase se evalúa con datos ficticios y un padrón institucional académico».

Minuto 2 a 4. Juan muestra el portal. «Entramos como administrador, buscamos una ficha, actualizamos un dato y generamos un PDF filtrado. La información procede del mismo backend que utiliza el teléfono. Los permisos administrativos se verifican en la API».

Minuto 4 a 6. David muestra Android. «El funcionario ingresa con su cuenta, confirma el documento y obtiene el estado registrado. La lectura de códigos está integrada mediante Google Code Scanner. Si no contamos con el ensayo físico del lector, mostramos la consulta manual y declaramos la prueba pendiente».

Minuto 6 a 8. Daniel explica backend y datos. «Spring MVC expone REST, Hibernate persiste en PostgreSQL y Tomcat ejecuta la aplicación. El registro coteja el padrón, genera una clave de seis caracteres y la entrega por SMTP. Las claves se almacenan como hashes BCrypt».

Minuto 8 a 10. Jorge cierra. «La entrega incluye código, JAR, APK, scripts, manuales y evidencias. Verificamos 27 casos integrales con PostgreSQL y SMTP, además de la compilación móvil y las pruebas del backend. Proponemos aceptar la primera fase académica y acordar como siguiente fase la conexión institucional autorizada y el despliegue con HTTPS».

## Demostración guiada de extremo a extremo

Antes de exponer, inicia el laboratorio y verifica web, correo y teléfono. Usa un documento de funcionario previamente autorizado y conserva abierta la bandeja de Mailpit. Ensaya el registro una vez por documento. Si ya existe, prepara otra autorización ficticia o utiliza recuperación.

1. Abre el portal y entra con el administrador de laboratorio.
2. Muestra una ficha completa y los filtros de ciudadanos.
3. Registra una autorización nueva para un funcionario ficticio, con fuerza, rango, ID y correo.
4. Abre Android, confirma la URL y completa los siete datos de registro.
5. Abre el correo recibido, lee la clave temporal e inicia sesión como funcionario.
6. Consulta un ciudadano no requerido y otro requerido. Explica de qué registro procede el resultado.
7. Muestra la auditoría desde la web y el historial desde el móvil.
8. Descarga un PDF filtrado y comprueba identificación, nombres y apellidos.
9. Explica cómo desactivar al funcionario y cómo se revoca su sesión.
10. Presenta el paquete y las instrucciones de despliegue.

Si falla la red, el líder expone el diagnóstico y utiliza las evidencias verificadas como respaldo. Si el lector físico no se ha ensayado, la demostración conserva ese límite y utiliza la consulta manual. La simulación institucional debe declararse antes de interpretar los resultados.

## Preguntas sobre arquitectura y datos

¿Por qué Spring MVC? La guía acepta SpringMVC. El proyecto utiliza sus controladores y el manejo de solicitudes HTTP dentro de Spring Boot. El portal estático se sirve en el mismo JAR y utiliza los endpoints REST.

¿Por qué PostgreSQL? Es una base relacional libre y soporta integridad, transacciones, índices y herramientas de respaldo. La entrega incluye esquema ejecutable y datos ficticios.

¿Dónde está el ORM? Las entidades Officer, Citizen, DefenseRecord, AuthSession y LookupAudit usan anotaciones JPA. Sus repositorios extienden JpaRepository. Hibernate implementa la persistencia.

¿Dónde corre Tomcat? Spring Boot incorpora Tomcat embebido. El JAR abre el puerto configurado y sirve tanto la API como el portal.

¿Cómo se comunica Android con el servidor? OkHttp envía y recibe JSON mediante HTTP REST. En un despliegue institucional se configura HTTPS. El token viaja en Authorization Bearer.

¿Por qué hay dos clientes? El administrador mantiene los datos y accesos desde el navegador. El funcionario consulta desde un dispositivo móvil. Ambos clientes comparten las reglas del backend.

¿Cómo evitan perder datos al reiniciar? PostgreSQL persiste en una instancia o volumen. El esquema y las semillas son idempotentes para conservar registros existentes. Las pruebas H2 temporales tienen un ciclo de vida separado del laboratorio PostgreSQL.

¿Qué diferencia hay entre RH y grupo sanguíneo? RH almacena el factor positivo o negativo. Grupo sanguíneo almacena A, B, AB u O. El formulario permite seleccionarlos de forma independiente.

## Preguntas sobre acceso y seguridad

¿Encriptan las contraseñas? En el servidor se usa BCrypt para conservar un hash con sal. La comprobación utiliza encoder.matches. La opción voluntaria de recordar en Android usa cifrado AES GCM con una clave de Android Keystore.

¿Cómo cumple la contraseña de seis caracteres? ApiService.newPassword usa SecureRandom, elige al menos una mayúscula, una minúscula y un número, agrega los restantes caracteres y mezcla el orden. La prueba SMTP verifica composición y longitud.

¿Qué impide que un funcionario se haga administrador? El registro asigna OFFICER. Las operaciones de permisos requieren una sesión ADMIN en el servidor. El cliente no decide su rol.

¿Qué ocurre si falla el correo? El registro devuelve error de servicio y no guarda una cuenta nueva. El usuario puede intentar otra vez cuando el administrador restablezca SMTP.

¿Qué ocurre al recuperar una contraseña? El servidor genera otra clave, entrega el correo, actualiza el hash y revoca las sesiones del funcionario. El mensaje de recuperación evita confirmar si existe una cuenta.

¿Qué guarda una sesión? Un token aleatorio se entrega al cliente. La base conserva su hash SHA256, el funcionario y la expiración. El cierre de sesión elimina esa sesión.

¿Qué ocurre al cambiar de servidor en Android? El cifrado de credenciales utiliza la URL como dato asociado. Las credenciales recordadas para un servidor no se recuperan para una URL diferente.

¿Puede el portal robar una clave desde un registro con HTML? El cliente escapa los valores mostrados y el servidor aplica una política CSP. Los tokens permanecen en memoria y se evitan cachés con información de sesión. Un despliegue real requiere revisión de seguridad adicional.

## Preguntas sobre alcance y liderazgo

¿Consultan realmente el Departamento de Defensa? El proyecto valida contra el padrón académico defense_registry. El equipo solicita al profesor aceptar ese supuesto para la primera fase porque la integración oficial exige contrato, documentación y credenciales autorizadas.

¿Un resultado no requerido acredita antecedentes oficiales? El resultado corresponde al estado del registro interno de la demostración. Su uso institucional exige una fuente autorizada y el marco operativo correspondiente.

¿Qué pasa si el documento no existe? La API responde SIN_DATOS. Así se distingue una ficha ausente de un ciudadano registrado como no requerido.

¿Qué entregó el líder? Coordinó la propuesta de alcance, asignó responsabilidades simuladas, identificó dependencias, fijó criterios de aceptación y reunió el incremento verificable y su documentación. El informe distingue los escenarios de dirección de los resultados técnicos observados.

¿Cómo se controla un cambio de alcance? El equipo registra motivo, impacto y esfuerzo. El líder propone prioridad y solicita decisión al cliente cuando cambia un criterio académico. Luego actualiza backlog, pruebas y documentación.

¿Por qué compraría el cliente esta primera fase? La entidad obtiene un flujo común entre administración y consulta, trazabilidad por funcionario, reportes filtrados y una entrega con fuentes y despliegue reproducible. La propuesta permite validar el proceso antes de financiar una integración institucional y operación externa.

¿Cuál es el principal pendiente? Ensayar el escáner con un documento físico y acordar la aceptación del padrón académico. El despliegue institucional requiere además HTTPS, credenciales de proveedores y políticas operativas.

## Lista de ensayo del equipo

Jorge debe poder dibujar la arquitectura y explicar por qué se eligieron las tecnologías. David debe localizar RegisterScreen, CivisApi y SecureCredentials, y explicar la confirmación del documento leído. Daniel debe localizar ApiService, los repositorios y ReportController. Juan debe demostrar los formularios web, los filtros y el resultado de verificacion.json.

Cada integrante ensaya una pregunta de un componente ajeno a su rol. Una respuesta completa identifica el requisito, el archivo que lo implementa y una evidencia de prueba. Cuando una integración está simulada o una prueba está pendiente, el integrante lo declara y explica el siguiente paso técnico.

Antes de la sustentación comprueben el paquete comprimido en otra carpeta, la versión de Java, el inicio del servidor, la conexión del teléfono y el correo. Conserven las capturas y el reporte PDF como respaldo de la ejecución observada. La aceptación final corresponde al cliente académico.
