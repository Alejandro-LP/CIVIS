"""Build the editable academic documents from the tracked Markdown sources."""
import re
from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "entrega/documentacion"


def inline(paragraph, text):
    for value in re.split(r"(`[^`]+`|\*\*[^*]+\*\*)", text):
        run = paragraph.add_run(value.strip('`*') if value.startswith(('`', '**')) else value)
        if value.startswith('**'):
            run.bold = True
        elif value.startswith('`'):
            run.font.name = 'Consolas'
            run.font.size = Pt(9)


def format_table(table, rows):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    column_count = len(rows[0])
    widths = {2: [2.0, 4.5], 3: [1.4, 1.7, 3.4], 4: [1.0, 1.35, 2.25, 1.9]}.get(column_count, [6.5 / column_count] * column_count)
    for index, column in enumerate(table.columns):
        column.width = Inches(widths[index])
    for row_index, values in enumerate(rows):
        cells = table.rows[0].cells if row_index == 0 else table.add_row().cells
        for column_index, value in enumerate(values):
            cell = cells[column_index]
            cell.width = Inches(widths[column_index])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            cell.text = value
            for paragraph in cell.paragraphs:
                paragraph.paragraph_format.space_after = Pt(4)
                paragraph.paragraph_format.space_before = Pt(4)
                paragraph.paragraph_format.line_spacing = 1.05
                for run in paragraph.runs:
                    run.font.size = Pt(9.5)
                    run.font.color.rgb = RGBColor.from_string('FFFFFF' if row_index == 0 else '202020')
                    run.bold = row_index == 0
            properties = cell._tc.get_or_add_tcPr()
            shading = OxmlElement('w:shd')
            shading.set(qn('w:fill'), '3C4240' if row_index == 0 else 'F1F3F1' if row_index % 2 else 'FFFFFF')
            properties.append(shading)
            margins = OxmlElement('w:tcMar')
            for side in ['top', 'left', 'bottom', 'right']:
                margin = OxmlElement('w:' + side)
                margin.set(qn('w:w'), '100')
                margin.set(qn('w:type'), 'dxa')
                margins.append(margin)
            properties.append(margins)
            borders = OxmlElement('w:tcBorders')
            for side in ['top', 'left', 'bottom', 'right']:
                border = OxmlElement('w:' + side)
                border.set(qn('w:val'), 'single')
                border.set(qn('w:sz'), '4')
                border.set(qn('w:color'), 'D9D9D9')
                borders.append(border)
            properties.append(borders)
        if row_index == 0:
            repeat = OxmlElement('w:tblHeader')
            table.rows[0]._tr.get_or_add_trPr().append(repeat)
        else:
            prevent_split = OxmlElement('w:cantSplit')
            table.rows[-1]._tr.get_or_add_trPr().append(prevent_split)


def create(source):
    document = Document()
    for border in document.styles.element.xpath('.//w:pBdr'):
        border.getparent().remove(border)
    section = document.sections[0]
    section.page_width, section.page_height = Inches(8.5), Inches(11)
    section.top_margin, section.bottom_margin = Inches(.75), Inches(.7)
    section.left_margin, section.right_margin = Inches(1), Inches(1)
    section.header_distance, section.footer_distance = Inches(.3), Inches(.3)
    for name in ['Normal', 'Title', 'Subtitle', 'Heading 1', 'Heading 2', 'Heading 3']:
        style = document.styles[name]
        style.font.name = 'Arial'
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.font.size = Pt(11)
    document.styles['Normal'].paragraph_format.line_spacing = 1.13
    document.styles['Normal'].paragraph_format.space_after = Pt(9)
    document.styles['Title'].font.size = Pt(30)
    document.styles['Heading 1'].font.size = Pt(21)
    document.styles['Heading 1'].paragraph_format.space_after = Pt(18)
    document.styles['Heading 2'].font.size = Pt(14)
    for name in ['Heading 1', 'Heading 2', 'Title']:
        document.styles[name].paragraph_format.keep_with_next = True
    header = section.header.paragraphs[0]
    header.text = 'CIVIS    Ingeniería de Software I'
    header.style = document.styles['Normal']
    header.runs[0].font.size = Pt(8)
    footer = section.footer.paragraphs[0]
    footer.text = 'Jorge Alejandro López Páez y equipo    |    Página '
    footer.runs[0].font.size = Pt(8)
    field = OxmlElement('w:fldSimple')
    field.set(qn('w:instr'), 'PAGE')
    footer._p.append(field)
    document.core_properties.author = 'Jorge Alejandro López Páez y equipo CIVIS'
    document.core_properties.subject = 'Entrega académica de Ingeniería de Software I'
    document.core_properties.title = source.stem.replace('_', ' ')
    lines = source.read_text(encoding='utf-8-sig').splitlines()
    position = 0
    code = False
    while position < len(lines):
        line = lines[position].strip()
        position += 1
        if not line:
            continue
        if line.startswith('```'):
            code = not code
            continue
        if code:
            paragraph = document.add_paragraph()
            paragraph.paragraph_format.space_after = Pt(2)
            run = paragraph.add_run(line)
            run.font.name = 'Consolas'
            run.font.size = Pt(9)
            continue
        if line.startswith('# '):
            document.add_paragraph(line[2:], 'Title')
        elif line.startswith('## '):
            document.add_page_break()
            document.add_paragraph(line[3:], 'Heading 1')
        elif line.startswith('|'):
            rows = []
            while True:
                if not re.match(r'^\|[\s:|\-]+\|$', line):
                    rows.append([cell.strip() for cell in line.strip('|').split('|')])
                if position >= len(lines) or not lines[position].strip().startswith('|'):
                    break
                line = lines[position].strip()
                position += 1
            table = document.add_table(rows=1, cols=len(rows[0]))
            format_table(table, rows)
            document.add_paragraph().paragraph_format.space_after = Pt(2)
        else:
            paragraph = document.add_paragraph()
            if re.match(r'^\d+\. ', line):
                paragraph.paragraph_format.left_indent = Inches(.1)
            inline(paragraph, line)
    target = OUTPUT / f'{source.stem}.docx'
    document.save(target)
    print(target.name)


if __name__ == '__main__':
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for file in sorted((ROOT / 'docs/proyecto').glob('0[123]_*.md')):
        create(file)
