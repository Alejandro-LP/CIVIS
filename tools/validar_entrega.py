"""Check artifact coverage, README references and deployment bundle contents."""
import json
import re
import zipfile
from pathlib import Path
from pypdf import PdfReader

root = Path(__file__).resolve().parents[1]
readme = (root / 'README.md').read_text(encoding='utf-8-sig')
references = re.findall(r'(?:src="|\]\()(docs/[^"\)]+)', readme)
missing = [name for name in references if not (root / name).exists()]
if missing:
    raise AssertionError(f'Referencias ausentes {missing}')
print(f'Referencias README verificadas {len(references)}')
checks = json.loads((root / 'docs/proyecto/verificacion.json').read_text(encoding='utf-8-sig'))
assert checks['casos_aprobados'] == 27
assert all(case['resultado'] == 'APROBADO' for case in checks['casos'])
for pdf in sorted((root / 'docs/entrega').glob('*.pdf')):
    reader = PdfReader(str(pdf))
    assert all(len(page.extract_text()) > 150 for page in reader.pages), f'Página vacía o huérfana en {pdf.name}'
    print(f'{pdf.name} {len(reader.pages)} páginas')
with zipfile.ZipFile(root / 'docs/entrega/04_Presentacion_CIVIS.pptx') as deck:
    slides = [name for name in deck.namelist() if re.fullmatch(r'ppt/slides/slide[0-9]+\.xml', name)]
    notes = [name for name in deck.namelist() if re.fullmatch(r'ppt/notesSlides/notesSlide[0-9]+\.xml', name)]
    assert len(slides) == len(notes) == 12
    print('PowerPoint verificado con doce diapositivas y doce notas')
jar = root / 'backend/target/civis-api-2.0.0.jar'
with zipfile.ZipFile(jar) as archive:
    for resource in ['index.html', 'app.js', 'styles.css']:
        assert 'BOOT-INF/classes/static/' + resource in archive.namelist()
    assert b'localhost:5433/civis' in archive.read('BOOT-INF/classes/application.properties')
print('JAR incluye portal y configuración definitiva')
bundle = root / 'entrega/CIVIS_Entrega_Completa.zip'
if bundle.exists():
    with zipfile.ZipFile(bundle) as archive:
        assert archive.testzip() is None
        for name in ['LEEME.md', 'SHA256SUMS.txt', 'aplicacion/backend/target/civis-api-2.0.0.jar',
                     'aplicacion/android/civis-debug.apk', 'aplicacion/database/01_schema.sql',
                     'documentacion/04_Presentacion_CIVIS.pptx', 'fuentes/CIVIS-fuentes.zip']:
            assert 'CIVIS_Entrega/' + name in archive.namelist()
    print('ZIP completo e íntegro')
