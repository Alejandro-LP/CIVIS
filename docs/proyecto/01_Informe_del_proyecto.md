# CIVIS Sistema de consulta ciudadana

Informe de dirección y diseño del proyecto

Fundación Universitaria Konrad Lorenz
Ingeniería de Sistemas · Ingeniería de Software I
Profesor Carlos Andrés López

Líder del proyecto y líder técnico Jorge Alejandro López Páez

Equipo David Santiago Guerrero Tolosa, Daniel Felipe Bejarano Perez y Juan Sebastian Romo Sanabria

Entrega académica del 17 de noviembre de 2026

Presentamos CIVIS para administrar la información general del ciudadano y permitir su consulta por funcionarios desde Android. El portal web y el móvil comparten una API REST con control de permisos y persistencia PostgreSQL. Esta entrega incluye software ejecutable, fuentes, scripts SQL, pruebas, manuales y presentación ejecutiva.

La organización del equipo, el cronograma y los escenarios de dirección descritos en este informe constituyen una simulación académica. Las verificaciones técnicas se respaldan con resultados obtenidos durante la preparación de la entrega.

## Dirección y alcance

Como líder del proyecto, Jorge Alejandro López Páez coordina el alcance, la integración y los criterios de aceptación. Nuestra propuesta para la Registraduría Konrad Lorenz reúne la administración web y la consulta móvil sobre un registro común. El cliente académico es el profesor Carlos Andrés López.

El problema consiste en mantener información ciudadana consistente y permitir al personal autorizado verificar un documento desde un teléfono. CIVIS centraliza la ficha, aplica permisos según el perfil y registra las consultas para su revisión posterior.

Los objetivos de la primera fase son gestionar los diez datos generales exigidos, habilitar registro y autenticación del funcionario, consultar el estado registrado y producir reportes PDF filtrados. La entrega técnica utiliza Kotlin y Jetpack Compose en Android, Spring MVC en la API y el portal, Hibernate como ORM, PostgreSQL como base y Tomcat embebido.

La aceptación se basa en ejecutar el flujo completo con datos ficticios, verificar permisos en el servidor, comprobar persistencia y entregar instrucciones reproducibles. El acceso a fuentes institucionales oficiales depende de un proveedor autorizado. Para el curso se propone el padrón de autorización como sustituto académico documentado, sujeto a la aceptación del profesor.

La versión de laboratorio funciona con HTTP local. Un despliegue institucional requiere HTTPS, revisión operativa, política de conservación, copias de seguridad y autorización del proveedor de datos.

## Equipo y responsabilidades

La asignación siguiente es una propuesta para la simulación. Cada integrante debe poder explicar el sistema completo porque la guía permite que el cliente seleccione a cualquiera como líder técnico durante la sustentación.

| Integrante | Rol propuesto | Responsabilidad y evidencia esperada |
| --- | --- | --- |
| Jorge Alejandro López Páez | Líder del proyecto y líder técnico | Alcance, decisiones, integración, aceptación y presentación ejecutiva |
| David Santiago Guerrero Tolosa | Desarrollo Android | Registro, login, consulta, escáner y almacenamiento cifrado |
| Daniel Felipe Bejarano Perez | Backend y datos | REST, permisos, PostgreSQL, ORM, SMTP y reportes PDF |
| Juan Sebastian Romo Sanabria | Portal web y calidad | Administración web, pruebas, manuales y evidencias |

Jorge es responsable final de la integración. Cada responsable revisa su componente con otro integrante antes de incorporarlo. Daniel revisa los contratos REST con David. Juan comprueba desde el navegador que los cambios de datos se reflejen en la API. Las decisiones que afectan el alcance o un supuesto institucional se presentan al cliente para su aceptación.

La definición de terminado propuesta exige compilar, ejecutar las pruebas correspondientes, revisar permisos, registrar la evidencia y actualizar el manual. Una función presentada como pendiente conserva ese estado hasta disponer de su prueba o aceptación.

## Metodología y planeación

Se propone Scrum adaptado al equipo académico, con cuatro iteraciones de una semana. El calendario es una planeación simulada y puede ajustarse al plazo real que indique el profesor. El trabajo existente sirve como incremento inicial y se registra por evidencia disponible.

| Iteración | Objetivo | Actividades | Criterio de salida |
| --- | --- | --- | --- |
| Semana 1 | Acordar alcance y datos | Historias, supuestos, arquitectura, esquema y prototipos | Requisitos revisados y esquema ejecutable |
| Semana 2 | Habilitar acceso | Padrón, registro, correo, login y perfiles | Funcionario autorizado recibe clave y accede |
| Semana 3 | Gestionar y consultar | CRUD web, móvil, historial y reporte | Cambios persistidos visibles desde REST |
| Semana 4 | Integrar y entregar | Pruebas, correcciones, manuales, paquete y ensayo | Entrega reproducible y sustentación preparada |

Cada semana incluye una revisión del incremento y una retrospectiva breve. La coordinación diaria propuesta dura diez minutos y se centra en lo completado, el siguiente objetivo y los impedimentos. Git registra cambios del producto. Las tareas se consideran terminadas por su prueba y revisión, no únicamente por existir en el repositorio.

Como estimación académica, se reserva una dedicación de siete horas por integrante y semana, equivalente a 112 horas de equipo en cuatro semanas. Es una hipótesis de planeación y no representa horas efectivamente trabajadas. Si el plazo se reduce, se prioriza el flujo registro, login, consulta y reporte, conservando los controles de permisos.

## Requisitos y trazabilidad

La referencia de alcance es la guía suministrada ProyectoII SistemaConsultaCiudadano, páginas 1 a 3. La siguiente matriz vincula sus criterios con la implementación y las evidencias de esta entrega.

| Código | Criterio | Implementación y evidencia |
| --- | --- | --- |
| RF01 | Entorno web y móvil | Portal Spring MVC en la raíz y Android Compose |
| RF02 | Registro de siete datos | RegisterScreen y API auth/register |
| RF03 | Validar fuerza y rango | Padrón defense_registry y listas dependientes |
| RF04 | Clave automática de seis caracteres | ApiService.newPassword y verificación de composición |
| RF05 | Confirmación por correo | JavaMailSender y captura SMTP en prueba integral |
| RF06 | Login y recordar contraseña | REST y AES GCM con Android Keystore |
| RF07 | Lectura del código de cédula | Google Code Scanner PDF417 y confirmación del documento |
| RF08 | Estado requerido o no requerido | Endpoint citizens/{id}/status con persistencia |
| RF09 | Gestión de diez datos ciudadanos | Portal, ManagementController y entidad Citizen |
| RF10 | Reporte PDF filtrado | ReportController y descarga desde el portal |
| RNF01 | Framework solicitado | Spring MVC mediante Spring Boot |
| RNF02 | Base libre y ORM | PostgreSQL e Hibernate JPA |
| RNF03 | Servidor e integración | Tomcat embebido y REST JSON |
| RNF04 | Seguridad en ambos clientes | Roles ADMIN y OFFICER validados por la API |
| RNF05 | Contraseñas protegidas | Hash BCrypt en servidor y cifrado opcional en Android |

## Arquitectura y flujo de información

El sistema sigue la arquitectura lógica de la guía. El administrador utiliza el portal web, el funcionario usa Android y ambos acceden al backend REST. Hibernate comunica el modelo de dominio con PostgreSQL. El servicio SMTP entrega las contraseñas temporales.

| Capa | Tecnología | Responsabilidad |
| --- | --- | --- |
| Presentación móvil | Kotlin y Jetpack Compose | Registro, login, captura y consulta |
| Presentación web | HTML CSS JavaScript | Administración de ciudadanos y accesos |
| Aplicación | Spring MVC y Tomcat | Contratos REST, reglas y permisos |
| Persistencia | Hibernate JPA y PostgreSQL | Usuarios, padrón, ciudadanos, sesiones y auditoría |
| Correo | JavaMailSender y SMTP | Registro y recuperación de contraseña |

En un registro válido, Android envía los siete datos. ApiService verifica la autorización, genera una clave y entrega el correo. La transacción guarda el funcionario con el hash de su clave. Si SMTP falla, el registro responde con error y no crea la cuenta.

Durante el login se comprueba BCrypt y el estado activo. El servidor genera un token aleatorio, conserva su hash y devuelve el perfil. Los clientes envían el token en Authorization Bearer. Cada consulta valida la sesión, busca el documento y guarda el resultado en lookup_audit.

Los diagramas editables de contexto, secuencia y datos están en docs/proyecto/diagramas.md. El portal se sirve desde el mismo JAR y origen de la API, lo que simplifica el despliegue y evita una configuración CORS adicional.

## Modelo de datos

El archivo database/01_schema.sql crea las tablas y los índices. El archivo 02_demo_citizens.sql carga diez ciudadanos ficticios sin sobrescribir registros existentes. En laboratorio, DemoData crea la cuenta y la autorización ficticias usando BCrypt para la contraseña.

| Entidad | Clave | Propósito y relación |
| --- | --- | --- |
| officers | id | Funcionario, datos, hash de clave, rol y estado |
| defense_registry | document_id | Autorización previa por datos institucionales |
| citizens | document_id | Información general y estado registrado |
| auth_sessions | token_hash | Sesión vinculada al funcionario y expiración |
| lookup_audit | id | Documento, usuario, resultado y fecha de consulta |

La ficha del ciudadano conserva identificación, nombres, apellidos, fecha y lugar de nacimiento, fecha y lugar de expedición, RH, grupo sanguíneo y estatura en centímetros. El estado requerido es un campo adicional para la consulta y el reporte.

Los identificadores se almacenan como texto para conservar su representación. Las fechas usan el tipo date y los instantes de auditoría usan timestamp con zona horaria. Los índices favorecen la revisión de consultas por funcionario, la expiración de sesiones y el filtro de ciudadanos por estado.

El esquema se probó en PostgreSQL 18 con ddl-auto=validate. Docker Compose propone PostgreSQL 17. La sintaxis SQL utilizada es compatible con ambas versiones, aunque la imagen de Docker necesita verificarse cuando Docker Desktop esté disponible. Un volumen ya existente requiere ejecutar el script de esquema de forma explícita o utilizar una base nueva.

## Seguridad y decisiones técnicas

Las contraseñas del servidor se protegen mediante BCrypt con factor de coste 12. El algoritmo calcula un hash con sal y permite comprobar una contraseña sin almacenarla de forma recuperable. La generación temporal utiliza SecureRandom, impone seis caracteres y garantiza al menos una mayúscula, una minúscula y un número.

Las sesiones contienen un token aleatorio de 32 bytes. La base conserva su hash SHA256 y una expiración de ocho horas por configuración. El cierre de sesión, la recuperación, el cambio de contraseña y el cambio de permisos revocan sesiones. El administrador no puede retirarse sus propios permisos desde el módulo de gestión.

La opción Recordar contraseña en Android es voluntaria. La app cifra las credenciales con AES de 256 bits en modo GCM. La clave permanece en Android Keystore y el cifrado se vincula a la URL del servidor mediante datos asociados. Cambiar de servidor impide recuperar esas credenciales para otro destino. Cerrar sesión o cambiar contraseña elimina el contenido recordado.

El portal conserva el token en memoria y solicita una nueva autenticación al recargarse. Los permisos se comprueban en la API aun cuando el cliente oculte una opción. El contenido de registros se escapa antes de insertarse en HTML. Las respuestas incluyen no-store, nosniff y una política CSP que limita scripts y estilos al mismo origen.

La autorización institucional se simula con defense_registry porque el equipo carece de un contrato y credenciales para el Departamento de Defensa. SMTP local permite evaluar la entrega de correo usando direcciones ficticias. Estos supuestos deben exponerse durante la sustentación y acordarse con el cliente académico.

## Pruebas y evidencias

La entrega conserva el resultado de 27 comprobaciones integrales en docs/proyecto/verificacion.json. Se ejecutaron contra un PostgreSQL 18 aislado y un receptor SMTP local. Las dos pruebas JUnit del backend también finalizaron correctamente. Android completó assembleDebug y lintDebug.

| Área | Comprobación | Resultado observado |
| --- | --- | --- |
| Datos | Crear esquema, cargar y validar con ORM | Aprobado en PostgreSQL |
| Registro | Autorización, correo y clave de seis caracteres | Aprobado con SMTP local |
| Login | Cuenta nueva, credenciales inválidas y duplicado | Aprobado |
| Permisos | Funcionario sin acceso administrativo | Aprobado con HTTP 403 |
| Ciudadanos | Alta completa, edición y eliminación | Aprobado |
| Consulta | Requerido, no requerido y sin datos | Aprobado |
| Reportes | Filtro, nombres largos y caracteres españoles | Aprobado |
| Sesiones | Recuperación, cambio de clave y desactivación | Aprobado con revocación |
| Portal | Alta y edición desde navegador | Aprobado con persistencia |
| Dispositivo | Escaneo físico de PDF417 | Pendiente de ensayo físico |

Las capturas del portal están en docs/evidencias/07 a 10 y las capturas previas de Android en 01 a 06. El reporte generado se incluye en entrega/evidencias. Los mensajes SMTP y contraseñas de las pruebas permanecen en directorios temporales excluidos del repositorio y del paquete.

## Riesgos y gestión de cambios

| Riesgo | Impacto | Respuesta del líder |
| --- | --- | --- |
| Fuente institucional sin acceso autorizado | Impide validar contra la entidad real | Declarar padrón simulado y solicitar aceptación del supuesto |
| Escáner o módulo Google no disponible | Interrumpe la captura automática | Mantener consulta manual y ensayar con teléfono antes de exponer |
| Puertos 8080 y 5432 ocupados | Impide iniciar servicios locales | Usar 8081 para la API y 5433 para PostgreSQL de Docker |
| SMTP fuera de servicio | Bloquea registro y recuperación | Rechazar la operación y revisar el servicio de correo |
| Divergencia de datos entre clientes | Consulta incorrecta | Centralizar reglas y persistencia en la misma API |
| Pérdida del equipo de laboratorio | Pérdida de datos de prueba | Respaldar con pg_dump y conservar fuentes en Git |
| Cambio tardío de requisitos | Afecta plazo y calidad | Estimar impacto, acordar prioridad y actualizar trazabilidad |

Una solicitud de cambio se registra con su motivo, requisito afectado, esfuerzo estimado y efecto en pruebas. Jorge revisa la propuesta con el responsable técnico y solicita al profesor la decisión cuando modifica el alcance académico. La programación del cambio se refleja en el backlog y en la versión de entrega.

La simulación contempla que, si el cliente exige la integración institucional durante la última iteración, el líder presenta la dependencia externa, propone un adaptador futuro y solicita formalmente la aceptación del padrón académico para la demostración. La respuesta conserva el criterio original en la matriz, con su supuesto y evidencia diferenciados.

## Decisiones del líder y aceptación

Esta sección describe escenarios académicos propuestos para ensayar la dirección del proyecto. Las actas siguientes son modelos de simulación y requieren validación de los integrantes antes de utilizarse como registro de una reunión real.

Acta simulada de inicio. Jorge presenta el problema, confirma los cuatro roles propuestos y establece como prioridad el flujo de registro, login y consulta. El equipo acuerda Spring MVC y PostgreSQL para compartir el backend entre web y Android. Queda por consultar con el profesor la aceptación del padrón institucional simulado.

Acta simulada de revisión. El responsable móvil plantea conservar la contraseña para comodidad del funcionario. El equipo acuerda una opción explícita protegida con Keystore y elimina el contenido al cerrar sesión o cambiar la contraseña. El responsable de calidad incorpora el caso de revisión al manual.

Acta simulada de cierre. El líder presenta el código, los scripts, el JAR, el APK, los resultados de prueba y las evidencias. Declara el escaneo físico pendiente de ensayo y solicita la revisión de la solución por el cliente. La entrega del paquete es una actividad distinta de la firma de aceptación.

Para aceptar la demostración, el cliente debe poder iniciar la solución, registrar un funcionario previamente autorizado, consultar ciudadanos ficticios, administrar su ficha desde la web y descargar un PDF filtrado. Los manuales describen los datos y pasos. La presentación ofrece beneficios observables y el guion prepara a todos los integrantes para preguntas técnicas.

## Referencias y archivos de entrega

Referencia académica principal. ProyectoII SistemaConsultaCiudadano, guía proporcionada por el usuario, páginas 1 a 3. La página 1 fija las tecnologías y el entorno web y móvil. La página 2 contiene acceso y consulta. La página 3 define la ficha restante, el reporte, la arquitectura y los entregables.

Referencia de implementación. Repositorio https://github.com/Alejandro-LP/CIVIS con fuentes Android y Java, portal web y scripts. Las rutas de cada criterio se identifican en la matriz de trazabilidad.

Referencia de prueba. docs/proyecto/verificacion.json y reportes JUnit y Android Lint generados durante la compilación. Las capturas documentan pantallas y operaciones realmente observadas durante la preparación de esta entrega.

El paquete comprimido contiene documentación editable y PDF, presentación ejecutiva, código fuente, base de datos con scripts, JAR ejecutable y APK debug para pruebas. El APK debug tiene finalidad académica. Para distribución institucional debe generarse una versión firmada y configurar HTTPS y los servicios autorizados.
