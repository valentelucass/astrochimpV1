from pathlib import Path
from io import BytesIO
from xml.sax.saxutils import escape
import re
from datetime import datetime, timezone
from collections import Counter

import pymupdf
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from PIL import Image, ImageOps, ImageDraw

root = Path.cwd()
task = root / 'work' / 'divisao-documentacao'
source = root / 'AstroChimp-Documentacao.pdf'
original = pymupdf.open(source)
markdown = (root / 'DOCUMENTACAO.md').read_text(encoding='utf-8')
section = markdown.split('## 5. ', 1)[1].split('<!-- quebra de página -->', 1)[0].strip()
heading, content = section.split('\n', 1)

def inline(text):
    return re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', escape(text))

body = ParagraphStyle('Body', fontName='Helvetica', fontSize=9, leading=12, spaceAfter=8, textColor=colors.HexColor('#222222'))
title = ParagraphStyle('Title', parent=body, fontName='Helvetica-Bold', fontSize=13, leading=17, spaceAfter=10)
cell = ParagraphStyle('Cell', parent=body, fontSize=8.5, leading=10.8, spaceAfter=0)
cell_header = ParagraphStyle('CellHeader', parent=cell, fontName='Helvetica-Bold')
story = [Paragraph('5. ' + inline(heading), title)]
lines = content.strip().splitlines()
row = 0
while row < len(lines):
    line = lines[row].strip()
    if not line:
        row += 1
        continue
    if line.startswith('|'):
        rows = []
        while row < len(lines) and lines[row].strip().startswith('|'):
            cells = [item.strip() for item in lines[row].strip().strip('|').split('|')]
            if not all(set(item) <= set('-: ') for item in cells):
                rows.append(cells)
            row += 1
        data = [[Paragraph(inline(item), cell_header if index == 0 else cell) for item in items] for index, items in enumerate(rows)]
        table = Table(data, colWidths=[80, 291, 108], hAlign='LEFT', repeatRows=1)
        table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#e8e8e8')),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#f7f7f7')]),
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('LEFTPADDING', (0, 0), (-1, -1), 7),
            ('RIGHTPADDING', (0, 0), (-1, -1), 7),
            ('TOPPADDING', (0, 0), (-1, -1), 4),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
            ('LINEBELOW', (0, 0), (-1, -1), 0.4, colors.HexColor('#dddddd')),
        ]))
        story.extend([table, Spacer(1, 9)])
        continue
    paragraph = [line]
    row += 1
    while row < len(lines) and lines[row].strip() and not lines[row].strip().startswith('|'):
        paragraph.append(lines[row].strip())
        row += 1
    story.append(Paragraph(inline(' '.join(paragraph)), body))

def decorate(canvas, document):
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(colors.HexColor('#666666'))
    canvas.drawString(52, 809, 'AstroChimp | Documentação do sistema')
    canvas.setStrokeColor(colors.HexColor('#dddddd'))
    canvas.setLineWidth(0.5)
    canvas.line(52, 38, 543, 38)
    canvas.drawString(52, 24, 'Projeto acadêmico | Organização da equipe: 13 de novembro de 2026')
    canvas.drawRightString(543, 24, 'Página 5')

buffer = BytesIO()
document = SimpleDocTemplate(buffer, pagesize=tuple(original[0].rect)[2:], leftMargin=58, rightMargin=58, topMargin=50, bottomMargin=48)
document.build(story, onFirstPage=decorate, onLaterPages=decorate)
addition = pymupdf.open(stream=buffer.getvalue(), filetype='pdf')
assert len(addition) == 1, 'A divisão precisa caber em uma página.'
result = pymupdf.open()
result.insert_pdf(original, from_page=0, to_page=0)
result.insert_pdf(original, from_page=2, to_page=4)
result.insert_pdf(addition)
result.insert_pdf(original, from_page=5)

# Atualiza somente o número do rodapé das páginas que foram deslocadas.
for index in range(1, 4):
    page = result[index]
    areas = page.search_for(f'Página {index + 2}')
    assert len(areas) == 1, (index, areas)
    bounds = areas[0]
    page.add_redact_annot(bounds + (-2, -1, 3, 1), fill=(1, 1, 1))
    page.apply_redactions()
    text = f'Página {index + 1}'
    font_size = 7.2
    width = pymupdf.get_text_length(text, fontname='helv', fontsize=font_size)
    page.insert_text((543 - width, bounds.y1 - 1.5), text, fontsize=font_size, color=(0.4, 0.4, 0.4))

# A nova seção 5 desloca a numeração dos títulos seguintes.
changed_headings = {}
for index in range(5, len(result)):
    page = result[index]
    headings = []
    for block in page.get_text('dict')['blocks']:
        for line in block.get('lines', []):
            for span in line['spans']:
                match = re.match(r'^(\d+)([. ].*)$', span['text'])
                if match and 'Bold' in span['font'] and int(match[1]) >= 5:
                    value = str(int(match[1]) + 1) + match[2]
                    headings.append((span, value))
                    changed_headings[span['text']] = value
                    page.add_redact_annot(pymupdf.Rect(span['bbox']), fill=(1, 1, 1))
    page.apply_redactions()
    for span, value in headings:
        page.insert_text(span['origin'], value, fontname='ArialResponsabilidades', fontfile='C:/Windows/Fonts/arialbd.ttf', fontsize=span['size'], color=(0.13, 0.13, 0.13))

metadata = original.metadata.copy()
metadata['modDate'] = datetime.now(timezone.utc).strftime('D:%Y%m%d%H%M%SZ')
result.set_metadata(metadata)
staged = task / 'AstroChimp-Documentacao-atualizada.pdf'
result.save(staged, garbage=4, deflate=True)
result.close()
checked = pymupdf.open(staged)
assert len(checked) == len(original)
added_text = checked[4].get_text()
for name in ['Miguel Conceição', 'Pedro Gabriel', 'Lucas Mateus Andrade', 'Guilherme Lettieri']:
    assert name in added_text, name
assert 'Plano de atribuição das tarefas.' in added_text
for old_index in range(len(original)):
    if old_index == 1:
        continue
    new_index = old_index if old_index == 0 or old_index >= 5 else old_index - 1
    clip = pymupdf.Rect(0, 0, original[old_index].rect.width, 799)
    expected = original[old_index].get_text(clip=clip)
    if old_index >= 5:
        expected = '\n'.join(changed_headings.get(line, line) for line in expected.splitlines())
    assert Counter(expected.replace('\xa0', ' ').splitlines()) == Counter(checked[new_index].get_text(clip=clip).replace('\xa0', ' ').splitlines()), f'Conteúdo alterado: {old_index + 1}'
for index, page in enumerate(checked):
    page.get_pixmap(matrix=pymupdf.Matrix(1.25, 1.25)).save(task / f'pagina-{index + 1}.png')
tiles = []
for index in range(len(checked)):
    thumb = Image.open(task / f'pagina-{index + 1}.png').convert('RGB')
    thumb.thumbnail((298, 421))
    tile = Image.new('RGB', (318, 451), '#dddddd')
    tile.paste(thumb, (10, 10))
    ImageDraw.Draw(tile).text((12, 433), f'Página {index + 1}', fill='black')
    tiles.append(tile)
contact = Image.new('RGB', (318 * 5, 451 * 2), 'white')
for index, tile in enumerate(tiles):
    contact.paste(tile, ((index % 5) * 318, (index // 5) * 451))
contact.save(task / 'revisao.png')
checked.close()
original.close()
print('PDF preparado: 10 páginas; nomes e conteúdo original verificados. Revisão visual pendente.')
