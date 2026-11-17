import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL, fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const runtimeModules = process.env.CIVIS_NODE_MODULES;
if (!runtimeModules) throw new Error('Configura CIVIS_NODE_MODULES con las dependencias del runtime de artefactos.');
process.env.RUNTIME_NODE_MODULES = runtimeModules;
const runtimeRequire = createRequire(path.join(runtimeModules, 'package.json'));
const { Presentation, PresentationFile } = await import(pathToFileURL(runtimeRequire.resolve('@oai/artifact-tool')).href);
const presentation = Presentation.create({ slideSize: { width: 1280, height: 720 } });
const ink = '#20271D', olive = '#4B5D3A', muted = '#65715E', paper = '#F8F9F5';
const output = path.join(root, 'entrega/documentacion');
const staging = path.join(root, 'tmp/delivery/slides');
await fs.mkdir(staging, { recursive: true });

function text(slide, value, x, y, width, height, size = 26, color = ink, bold = false) {
  const shape = slide.shapes.add({
    geometry: 'textbox', name: value.slice(0, 70),
    position: { left: x, top: y, width, height }, fill: 'none',
    line: { fill: 'none', width: 0, style: 'solid' }
  });
  shape.text = value;
  shape.text.style = { typeface: 'Arial', fontSize: size, color, bold, insets: 0,
    verticalAlignment: 'top', autoFit: 'none', wrap: 'square' };
  return shape;
}

function slide(title, notes, dark = false) {
  const page = presentation.slides.add();
  page.background.fill = dark ? '#34452B' : paper;
  if (title) text(page, title, 65, 55, 1150, 95, 46, dark ? '#FFFFFF' : ink, true);
  const number = presentation.slides.items.length;
  text(page, 'CIVIS  /  Ingeniería de Software I', 65, 672, 800, 25, 16, dark ? '#D6E0CD' : muted);
  text(page, String(number).padStart(2, '0'), 1170, 672, 55, 25, 16, dark ? '#D6E0CD' : muted);
  page.speakerNotes.text = notes;
  return page;
}

async function image(page, filename, x, y, width, height) {
  page.images.add({ blob: new Uint8Array(await fs.readFile(path.join(root, 'docs/evidencias', filename))),
    contentType: filename.endsWith('.png') ? 'image/png' : 'image/jpeg', fit: 'contain',
    position: { left: x, top: y, width, height }, alt: `Evidencia real de CIVIS ${filename}` });
}

let page = slide('', 'Jorge abre. Presentar a los cuatro integrantes y el objetivo de la fase académica. La organización del equipo y la planeación se proponen como simulación. Fuente de alcance: guía ProyectoII SistemaConsultaCiudadano, páginas 1 a 3.', true);
text(page, 'CIVIS', 70, 70, 1100, 125, 108, '#FFFFFF', true);
text(page, 'Sistema de consulta ciudadana', 76, 220, 1120, 78, 46, '#FFFFFF');
text(page, 'Propuesta ejecutiva para la Registraduría Konrad Lorenz', 76, 320, 1100, 70, 28, '#D6E0CD');
text(page, 'Jorge Alejandro López Páez', 76, 435, 1100, 45, 30, '#FFFFFF', true);
text(page, 'Líder del proyecto y líder técnico', 76, 483, 1100, 35, 23, '#D6E0CD');
text(page, 'David Santiago Guerrero Tolosa\nDaniel Felipe Bejarano Perez\nJuan Sebastian Romo Sanabria', 76, 550, 1100, 97, 22, '#FFFFFF');

page = slide('Necesidad del cliente', 'Jorge explica el problema. Evitar prometer porcentajes de ahorro o integración oficial que no se han medido. La guía pide administración sencilla de información y consulta móvil de estados. Fuente: guía, página 1.');
text(page, 'Un registro común para la administración y la consulta en campo', 70, 177, 1090, 130, 44, olive, true);
text(page, 'El administrador necesita mantener la ficha ciudadana y habilitar al personal.', 72, 350, 1070, 95, 30);
text(page, 'El funcionario necesita confirmar el documento, consultar el estado registrado y conservar la trazabilidad.', 72, 480, 1070, 120, 30);

page = slide('Propuesta web y móvil', 'Juan presenta el portal y David el cliente Android. Ambos usan la misma API. La captura corresponde a una ejecución observada con PostgreSQL y datos ficticios. Evidencia docs/evidencias/10-web-resumen.jpg.');
text(page, 'Portal administrativo', 70, 184, 420, 55, 32, olive, true);
text(page, 'Ciudadanos, funcionarios, autorizaciones y reportes desde el navegador.', 70, 260, 390, 150, 28);
text(page, 'App Android', 70, 442, 420, 55, 32, olive, true);
text(page, 'Registro, acceso y consulta conectados al mismo backend.', 70, 510, 390, 110, 28);
await image(page, '10-web-resumen.jpg', 500, 170, 705, 460);

page = slide('Registro del funcionario', 'Daniel explica el padrón académico. La API compara documento, fuerza, rango, ID y correo. Si coincide, genera seis caracteres y entrega correo SMTP antes de guardar el hash BCrypt. La fuente institucional oficial requiere acceso autorizado. Evidencia móvil 02-registro-ayuda.png y ApiService.java.');
text(page, 'Autorización institucional previa', 70, 185, 690, 55, 32, olive, true);
text(page, 'El administrador registra el documento, la fuerza, el rango, el ID y el correo autorizado.', 70, 260, 650, 130, 28);
text(page, 'La app recibe los datos del funcionario. El servidor valida la coincidencia y entrega una contraseña temporal por correo.', 70, 410, 650, 155, 28);
text(page, 'Padrón académico para la demostración', 70, 590, 680, 36, 23, muted);
await image(page, '02-registro-ayuda.png', 845, 150, 300, 500);

page = slide('Consulta desde Android', 'David demuestra el acceso y la consulta manual. Google Code Scanner admite PDF417, QR y Code128. El operador confirma identificación antes de consultar. El ensayo físico del lector debe completarse antes de la sustentación. Evidencia de sesión 06-sesion-iniciada.png.');
text(page, 'Una sesión identifica al funcionario', 70, 190, 700, 100, 35, olive, true);
text(page, 'El usuario confirma el documento y obtiene el nombre y el estado guardados en la base.', 70, 320, 640, 125, 29);
text(page, 'Cada consulta registra usuario, fecha, documento y resultado.', 70, 490, 640, 90, 29);
await image(page, '06-sesion-iniciada.png', 850, 155, 290, 495);

page = slide('Administración ciudadana', 'Juan muestra la ficha web. Sus diez datos generales son los requeridos por la guía. El estado es un campo adicional. Las fechas futuras y la expedición anterior al nacimiento se validan. Evidencia 08-web-ficha.jpg obtenida tras guardar y reabrir la ficha en PostgreSQL.');
text(page, 'Ficha completa y cambios persistidos', 70, 187, 495, 105, 35, olive, true);
text(page, 'Datos personales, nacimiento, expedición, RH, grupo sanguíneo y estatura.', 70, 325, 460, 130, 28);
text(page, 'Búsqueda, edición y eliminación con confirmación.', 70, 495, 460, 100, 28);
await image(page, '08-web-ficha.jpg', 590, 160, 620, 490);

page = slide('Arquitectura de la solución', 'Daniel explica cada capa. El portal y Android usan REST. Spring MVC y Tomcat manejan solicitudes, Hibernate comunica con PostgreSQL y SMTP entrega correo. Este diagrama es editable. Fuente: código del proyecto y figura lógica de la guía, página 3.');
function node(label, x, y, width, height) {
  const box = page.shapes.add({ geometry: 'rect', position: { left: x, top: y, width, height },
    fill: '#EEF2E7', line: { fill: '#BAC5AF', width: 1, style: 'solid' } });
  box.text = label;
  box.text.style = { typeface: 'Arial', fontSize: 27, color: ink, bold: true,
    alignment: 'center', verticalAlignment: 'middle', insets: 15 };
  return box;
}
const web = node('Portal web', 75, 230, 250, 90);
const mobile = node('Android', 75, 425, 250, 90);
const api = node('Spring MVC\nTomcat y REST', 435, 290, 300, 150);
const database = node('PostgreSQL\nHibernate JPA', 880, 230, 300, 110);
const smtp = node('Correo SMTP', 880, 440, 300, 90);
for (const [from, to] of [[web, api], [mobile, api], [api, database], [api, smtp]]) {
  page.shapes.connect(from, to, { kind: 'elbow', fromSide: 'right', toSide: 'left',
    line: { style: 'solid', fill: olive, width: 2 } });
}
text(page, 'Padrón académico, ciudadanos, sesiones y auditoría en la base compartida', 75, 580, 1130, 55, 25, muted);

page = slide('Seguridad y control de acceso', 'Daniel explica BCrypt coste 12, tokens aleatorios con hash SHA256 y expiración. David explica AES GCM con clave no exportable de Keystore para recordar contraseña. El registro asigna OFFICER. Los permisos se comprueban en la API. Fuentes ApiService.java, SecureCredentials.kt y WebConfiguration.java.');
const security = [
  ['Contraseñas del servidor', 'Hash BCrypt con sal y generación temporal mediante SecureRandom.'],
  ['Sesiones y permisos', 'ADMIN y OFFICER con revocación por cierre, recuperación o cambio de acceso.'],
  ['Contraseña recordada', 'Opción voluntaria cifrada con AES GCM y Android Keystore.'],
  ['Portal web', 'Token en memoria, contenido escapado y cabeceras CSP y no-store.']
];
security.forEach(([title, body], i) => { text(page, title, 75, 175 + i * 115, 390, 50, 28, olive, true); text(page, body, 485, 175 + i * 115, 700, 85, 26); });

page = slide('Planeación y responsabilidades', 'Jorge presenta una planeación simulada en cuatro iteraciones. La dedicación de 112 horas es una hipótesis de planeación del informe, no un dato de ejecución. Los roles son propuestos para el ensayo académico y todos deben prepararse para sustentar la arquitectura completa.');
const schedule = [['Semana 1', 'Alcance y datos'], ['Semana 2', 'Acceso y correo'], ['Semana 3', 'Gestión y consulta'], ['Semana 4', 'Pruebas y entrega']];
schedule.forEach(([week, subject], i) => { text(page, week, 75 + 295 * i, 190, 270, 55, 30, olive, true); text(page, subject, 75 + 295 * i, 260, 260, 100, 27); });
text(page, 'Roles propuestos para la simulación', 75, 390, 1100, 42, 26, muted);
text(page, 'Jorge Alejandro López Páez\nDirección e integración', 75, 465, 500, 95, 25, ink, true);
text(page, 'David Santiago Guerrero Tolosa\nAndroid', 675, 465, 500, 95, 25);
text(page, 'Daniel Felipe Bejarano Perez\nBackend y datos', 75, 570, 500, 85, 25);
text(page, 'Juan Sebastian Romo Sanabria\nPortal y calidad', 675, 570, 500, 85, 25);

page = slide('Pruebas de la entrega', 'Juan presenta resultados reales. docs/proyecto/verificacion.json contiene 27 casos aprobados sobre PostgreSQL 18 aislado y SMTP local de captura. Las dos pruebas JUnit y assembleDebug y lintDebug pasaron. La descarga PDF desde navegador también se comprobó. Fuente de la captura 09-web-reportes.jpg.');
text(page, '27', 70, 160, 420, 150, 126, olive, true);
text(page, 'casos integrales aprobados', 75, 325, 480, 80, 34, ink, true);
text(page, 'Registro con correo, permisos, datos, consulta, PDF y revocación de sesiones.', 75, 425, 460, 145, 28);
text(page, 'PostgreSQL 18 y SMTP local', 75, 592, 475, 35, 23, muted);
await image(page, '09-web-reportes.jpg', 580, 175, 630, 440);

page = slide('Alcance de la primera fase', 'Jorge explica los supuestos antes de solicitar aceptación. La guía permite sustentarlos en el documento. El padrón es académico y los estados son datos internos ficticios. La cámara física queda pendiente. La operación institucional requiere fuentes autorizadas y HTTPS. No afirmar una integración gubernamental existente.');
text(page, 'Producto verificado en laboratorio', 75, 190, 1080, 70, 36, olive, true);
text(page, 'Portal web, app Android, API REST, persistencia, correo de pruebas y reportes.', 75, 285, 1080, 100, 30);
text(page, 'Acuerdos con el cliente académico', 75, 435, 1080, 55, 34, olive, true);
text(page, 'Aceptar el padrón simulado, ensayar la lectura física y definir una fase de integración institucional autorizada.', 75, 520, 1070, 115, 30);

page = slide('Entrega y propuesta de aceptación', 'Jorge cierra. Entregamos fuentes, JAR, APK, scripts SQL, manuales editables y PDF, presentación y pruebas. Pedimos al cliente evaluar la primera fase académica y acordar las dependencias externas. La aceptación corresponde al profesor. Fuente: entregables de la guía página 3.');
text(page, 'Software y documentación para reproducir la demostración', 75, 180, 1100, 115, 42, olive, true);
text(page, 'Fuentes y ejecutables\nScripts de base de datos\nManuales y evidencias\nPreparación de la sustentación', 75, 335, 560, 250, 29);
text(page, 'Solicitamos evaluar la primera fase y acordar los siguientes pasos de integración.', 700, 350, 470, 225, 32, ink, true);

const candidatePath = path.join(staging, 'candidate-final.pptx');
await (await PresentationFile.exportPptx(presentation)).save(candidatePath);
const skill = process.env.CIVIS_PRESENTATION_SKILL;
const { finalizePresentation } = await import(pathToFileURL(path.join(skill, 'container_tools/artifact_tool_utils.mjs')).href);
const result = await finalizePresentation({
  workspaceDir: root, candidatePath,
  finalPath: path.join(output, '04_Presentacion_CIVIS.pptx'),
  pythonExecutable: process.env.CIVIS_PYTHON,
  integrityValidatorPath: path.join(skill, 'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath: path.join(skill, 'container_tools/inspect_presentation_layout_geometry.py'),
  explicitTotalSlideCount: 12,
  requiredNativeTableOwnerSlides: [], requiredNativeChartOwnerSlides: [],
  fontPolicy: { basis: 'design', families: ['Arial'] },
  layoutArgs: ['--expected-slide-size-emu', '12192000,6858000', '--validate-heading-fit'],
  verifyArtifactToolImport: true,
  receiptPath: path.join(staging, 'validation-final.json')
});
console.log(JSON.stringify({ complete: true, slides: 12, output: result.finalPath || output }));
