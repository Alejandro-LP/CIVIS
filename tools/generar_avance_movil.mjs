import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL, fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const modules = process.env.RUNTIME_NODE_MODULES;
const runtime = createRequire(path.join(modules, 'package.json'));
const { Presentation, PresentationFile } = await import(pathToFileURL(runtime.resolve('@oai/artifact-tool')).href);
const deck = Presentation.create({ slideSize: { width: 1280, height: 720 } });
const output = path.join(root, 'entrega/avance-movil');
const staging = path.join(root, 'tmp/avance-movil');
await fs.mkdir(output, { recursive: true });
await fs.mkdir(staging, { recursive: true });

function text(slide, value, x, y, w, h, size = 30, bold = false, color = '#20271D') {
  const box = slide.shapes.add({ geometry: 'textbox', name: value.slice(0, 60),
    position: { left: x, top: y, width: w, height: h }, fill: 'none',
    line: { fill: 'none', style: 'solid', width: 0 } });
  box.text = value;
  box.text.style = { typeface: 'Arial', fontSize: size, bold, color, insets: 0, autoFit: 'none', wrap: 'square' };
}

function page(title, notes, dark = false) {
  const slide = deck.slides.add();
  slide.background.fill = dark ? '#34452B' : '#F8F9F5';
  if (title) text(slide, title, 65, 55, 1150, 90, 46, true, dark ? '#FFFFFF' : '#20271D');
  slide.speakerNotes.text = notes;
  text(slide, 'CIVIS  /  Avance de la app móvil', 65, 673, 1050, 25, 16, false, dark ? '#D6E0CD' : '#65715E');
  return slide;
}

async function image(slide, filename, x = 865, y = 165, w = 290, h = 490) {
  slide.images.add({ blob: new Uint8Array(await fs.readFile(path.join(root, 'docs/evidencias', filename))),
    contentType: 'image/png', fit: 'contain', position: { left: x, top: y, width: w, height: h },
    alt: `Captura real de CIVIS ${filename}` });
}

let slide = page('', '0:00 a 0:45. Presentar el avance móvil y los cuatro integrantes. Los cargos del equipo están pendientes de acordarse. Fuente del guion docs/avance-movil/Guion_10_minutos.md.', true);
text(slide, 'CIVIS', 70, 75, 1120, 125, 106, true, '#FFFFFF');
text(slide, 'Avance de la aplicación móvil', 75, 240, 1100, 85, 48, false, '#FFFFFF');
text(slide, 'Ingeniería de Software I', 75, 350, 1100, 45, 28, false, '#D6E0CD');
text(slide, 'Jorge Alejandro López Páez\nDavid Santiago Guerrero Tolosa\nDaniel Felipe Bejarano Perez\nJuan Sebastian Romo Sanabria', 75, 452, 1100, 170, 26, false, '#FFFFFF');

slide = page('Recorrido del funcionario', '0:45 a 1:30. Presentamos el incremento Android. Explicar que la demostración emplea datos ficticios. Los formularios están implementados. La verificación directa en dispositivo de login y recuerdo cifrado consta en docs/proyecto/verificacion_android.json.');
text(slide, 'Acceso y registro', 75, 190, 1080, 60, 38, true, '#4B5D3A');
text(slide, 'Identificación del funcionario, validación de campos y recuperación de acceso.', 75, 280, 1080, 100, 32);
text(slide, 'Consulta ciudadana', 75, 440, 1080, 60, 38, true, '#4B5D3A');
text(slide, 'Confirmación del documento y presentación del nombre y el estado registrado.', 75, 530, 1080, 100, 32);

slide = page('Registro del funcionario', '1:30 a 3:00. Abrir Registrar funcionario y recorrer los siete campos. Seleccionar Policía y Patrullero para mostrar dependencia de rango. El registro exige autorización previa y entrega clave temporal por correo. Mostrar campos y validación. Si se envía, verificar previamente disponibilidad de la autorización ficticia. Evidencia 03-registro-formulario.png.');
text(slide, 'Siete datos de registro', 75, 185, 690, 70, 36, true, '#4B5D3A');
text(slide, 'Identificación, nombres, apellidos, fuerza, rango, ID institucional y correo.', 75, 290, 650, 145, 31);
text(slide, 'El rango depende de la fuerza seleccionada.', 75, 485, 650, 95, 31);
await image(slide, '03-registro-formulario.png');

slide = page('Inicio de sesión', '3:00 a 4:15. Ingresar con identificación ficticia 1234567890 y contraseña Demo123. Mostrar Hola Andrea. La cuenta del ensayo es administradora, pero la exposición recorre acceso, consulta y perfil móviles. Recordar contraseña cifra mediante Keystore. Se verificó restauración después de reiniciar el Redmi Note 14. Evidencia 11-android-recordar-acceso.png.');
text(slide, 'Identificación y contraseña', 75, 185, 710, 70, 36, true, '#4B5D3A');
text(slide, 'Ingreso con la cuenta de laboratorio y acceso al panel del usuario.', 75, 290, 650, 130, 31);
text(slide, 'Recordar contraseña con cifrado en el dispositivo.', 75, 485, 650, 100, 31);
await image(slide, '11-android-recordar-acceso.png');

slide = page('Consulta en vivo', '4:15 a 7:15. Desde el panel, desplazar hasta Consulta manual. Consultar 1000000011 para No requerido y 1000000012 para Requerido. Explicar Sin datos para un documento ausente. Confirmar la identificación antes de enviar. La captura muestra el panel previo a la consulta. El escáner está integrado y su ensayo físico está pendiente. Evidencia 06-sesion-iniciada.png.');
text(slide, 'Confirmar el documento', 75, 190, 690, 65, 36, true, '#4B5D3A');
text(slide, '1000000011\nEjemplo de no requerido', 75, 300, 650, 110, 32);
text(slide, '1000000012\nEjemplo de requerido', 75, 455, 650, 110, 32);
text(slide, 'Registros ficticios para la demostración', 75, 600, 710, 35, 23, false, '#65715E');
await image(slide, '06-sesion-iniciada.png');

slide = page('Recuperación de acceso', '7:15 a 8:15. Mostrar perfil y cierre de sesión. Abrir Recuperar acceso y explicar que el documento inicia el envío de una nueva clave temporal al correo registrado. Si se decide enviar la recuperación, ensayar previamente el buzón y recordar que se revocan sesiones. Evidencia 04-recuperar-acceso.png.');
text(slide, 'Recuperación por identificación', 75, 190, 710, 100, 36, true, '#4B5D3A');
text(slide, 'Solicitud de una nueva contraseña temporal al correo registrado.', 75, 335, 650, 135, 31);
text(slide, 'Perfil y cierre de sesión desde el móvil.', 75, 515, 650, 90, 31);
await image(slide, '04-recuperar-acceso.png');

slide = page('Implementación en Android', '8:15 a 9:15. Explicar únicamente la parte móvil. Kotlin y Jetpack Compose, navegación entre pantallas, validación de entrada, carga y errores. Mencionar AES GCM y Keystore para el recuerdo opcional. El lector utiliza Google Code Scanner. Evitar declarar validado el ensayo de cámara. Fuentes MainActivity.kt, CivisApi.kt y SecureCredentials.kt.');
text(slide, 'Kotlin y Jetpack Compose', 75, 185, 1090, 60, 36, true, '#4B5D3A');
text(slide, 'Pantallas de acceso, registro, consulta y perfil con navegación de regreso.', 75, 270, 1090, 100, 31);
text(slide, 'Validación y respuesta al usuario', 75, 420, 1090, 60, 36, true, '#4B5D3A');
text(slide, 'Campos obligatorios, estados de carga, mensajes de error y credenciales cifradas.', 75, 505, 1090, 110, 31);

slide = page('Siguientes pasos del móvil', '9:15 a 10:00. Cerrar como avance. Pendientes concretos: ensayo físico de lectura con cámara, revisión de experiencia en otros dispositivos y repetición del registro y recuperación completos desde el teléfono. La fuente institucional se mantiene simulada. Todos figuran como integrantes sin cargos asignados.');
text(slide, 'Ensayo de lectura con la cámara', 75, 190, 1100, 75, 36, true, '#4B5D3A');
text(slide, 'Verificar un código compatible en un dispositivo físico.', 75, 285, 1080, 90, 31);
text(slide, 'Revisión del recorrido completo', 75, 440, 1100, 70, 36, true, '#4B5D3A');
text(slide, 'Repetir registro, recuperación y consulta con el equipo y ajustar la experiencia.', 75, 530, 1080, 105, 31);

const candidatePath = path.join(staging, 'candidate.pptx');
await (await PresentationFile.exportPptx(deck)).save(candidatePath);
const skill = process.env.CIVIS_PRESENTATION_SKILL;
const { finalizePresentation } = await import(pathToFileURL(path.join(skill, 'container_tools/artifact_tool_utils.mjs')).href);
await finalizePresentation({ workspaceDir: root, candidatePath,
  finalPath: path.join(output, 'CIVIS_Avance_Movil_10min.pptx'),
  pythonExecutable: process.env.CIVIS_PYTHON,
  integrityValidatorPath: path.join(skill, 'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath: path.join(skill, 'container_tools/inspect_presentation_layout_geometry.py'),
  explicitTotalSlideCount: 8, fontPolicy: { basis: 'design', families: ['Arial'] },
  layoutArgs: ['--expected-slide-size-emu', '12192000,6858000', '--validate-heading-fit'],
  requiredNativeTableOwnerSlides: [], requiredNativeChartOwnerSlides: [],
  verifyArtifactToolImport: true, receiptPath: path.join(staging, 'validation.json') });
console.log('Avance móvil de ocho diapositivas preparado para diez minutos.');
