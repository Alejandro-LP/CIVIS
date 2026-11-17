"""Create a concise two-page rehearsal guide for the ten-minute mobile update."""
from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak

root = Path(__file__).resolve().parents[1]
output = root / 'entrega/avance-movil/Guion_Avance_Movil_10min.pdf'
pdfmetrics.registerFont(TTFont('Arial', 'C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('ArialBold', 'C:/Windows/Fonts/arialbd.ttf'))
title = ParagraphStyle('Title', fontName='ArialBold', fontSize=22, leading=27, spaceAfter=13)
heading = ParagraphStyle('Heading', fontName='ArialBold', fontSize=11.2, leading=14, spaceBefore=10, spaceAfter=4)
body = ParagraphStyle('Body', fontName='Arial', fontSize=10.3, leading=13.5, spaceAfter=5)
small = ParagraphStyle('Small', parent=body, fontSize=8.9, leading=11.5, textColor=colors.HexColor('#555555'))
story = []


def paragraph(text, style=body):
    story.append(Paragraph(text, style))


paragraph('CIVIS Avance de la app móvil', title)
paragraph('Guion de diez minutos para la revisión de hoy', heading)
paragraph('Equipo Jorge Alejandro López Páez, David Santiago Guerrero Tolosa, Daniel Felipe Bejarano Perez y Juan Sebastian Romo Sanabria. Cargos pendientes de acordarse.', small)
schedule = [
    ('0:00 a 0:45  Presentación', '«Hoy presentamos el avance de la aplicación móvil CIVIS. Vamos a mostrar las pantallas implementadas y el recorrido de un funcionario». Presenta a los cuatro integrantes.'),
    ('0:45 a 1:30  Recorrido', '«La app organiza el registro, el acceso y la consulta ciudadana desde Android. Para la demostración usamos cuentas y registros ficticios». Muestra la pantalla inicial.'),
    ('1:30 a 3:00  Registro', 'Recorre los siete datos. Selecciona una fuerza y después un rango. Explica la validación de campos, la autorización previa y el envío de contraseña temporal. Comprueba antes la disponibilidad del documento si vas a enviar el registro.'),
    ('3:00 a 4:15  Login', 'Entra con identificación 1234567890 y contraseña Demo123. Muestra Hola Andrea. Explica la opción Recordar contraseña y que se verificó su cifrado y recuperación después de reiniciar la app.'),
    ('4:15 a 7:15  Consulta en vivo', 'Abre Consulta manual. Consulta 1000000011 para No requerido y 1000000012 para Requerido. Confirma el documento antes de enviar y muestra el nombre devuelto. Explica Sin datos para un documento ausente.'),
    ('7:15 a 8:15  Recuperación', 'Muestra el perfil, el cierre de sesión y Recuperar acceso. Explica que el documento permite solicitar una nueva clave temporal al correo registrado.'),
    ('8:15 a 9:15  Implementación móvil', 'Menciona Kotlin, Jetpack Compose, navegación, validación, estados de carga y errores. Recordar contraseña utiliza AES GCM y Android Keystore.'),
    ('9:15 a 10:00  Próximos pasos', '«Seguimos con el ensayo físico del escáner, la revisión en otros dispositivos y la repetición del recorrido de registro y recuperación con el equipo». Cierra mostrando el incremento móvil.')
]
for name, explanation in schedule:
    paragraph(name, heading)
    paragraph(explanation)

story.append(PageBreak())
paragraph('Preparación de la demostración', title)
for step in [
    '1. Conecta y desbloquea el teléfono. Comprueba el cable USB y la depuración.',
    '2. Inicia los servicios de prueba y pulsa Comprobar conexión en la app.',
    '3. El laboratorio preparado en este PC utiliza http://127.0.0.1:8082 y adb reverse tcp:8082 tcp:8082. El PC y los servicios deben permanecer encendidos.',
    '4. El script habitual iniciar-laboratorio.ps1 utiliza 8081. La URL y el reenvío USB deben usar el mismo puerto del servidor.',
    '5. Ensaya el login y las dos consultas ficticias antes de exponer. Deja el PDF de diapositivas abierto como respaldo.',
    '6. Usa Consulta manual para el recorrido ensayado. Presenta el escáner como implementación pendiente de comprobar físicamente.'
]:
    paragraph(step)
paragraph('Frases para acompañar las pantallas', heading)
paragraph('Registro. «El rango cambia según la fuerza seleccionada y se comprueban los datos obligatorios».')
paragraph('Login. «Con estas credenciales de laboratorio entramos al panel del usuario».')
paragraph('Consulta. «Confirmamos la identificación y mostramos el nombre y el estado devueltos por los registros de prueba».')
paragraph('Recuperación. «El funcionario solicita otra contraseña temporal usando el documento registrado».')
paragraph('Preguntas frecuentes', heading)
paragraph('¿Qué muestran hoy? El avance móvil y su recorrido de acceso, registro, consulta y recuperación.')
paragraph('¿Qué se verificó en el teléfono? Login, almacenamiento cifrado y restauración de credenciales tras reiniciar. El registro de esa prueba está en verificacion_android.json.')
paragraph('¿Los datos son oficiales? La demostración usa cuentas ficticias y un padrón académico de autorizaciones.')
paragraph('¿Ya está probado el escáner? Google Code Scanner está integrado. Falta el ensayo físico con cámara y un código compatible.')
paragraph('¿Quién es el líder? Los cargos están pendientes de acordarse. Los cuatro figuran como integrantes.')
paragraph('Si preguntan por los aportes individuales, describan el trabajo real de cada integrante y eviten atribuir responsabilidades sin acordarlas.', small)


def footer(canvas, document):
    canvas.saveState()
    canvas.setFont('Arial', 8)
    canvas.setFillColor(colors.HexColor('#666666'))
    canvas.drawString(45, 25, 'CIVIS  Avance móvil  17 de noviembre de 2026')
    canvas.drawRightString(567, 25, str(document.page))
    canvas.restoreState()


SimpleDocTemplate(str(output), pagesize=letter, rightMargin=45, leftMargin=45,
                  topMargin=40, bottomMargin=42,
                  title='Guion del avance móvil CIVIS de diez minutos',
                  author='Equipo CIVIS').build(story, onFirstPage=footer, onLaterPages=footer)
print(output.name)
