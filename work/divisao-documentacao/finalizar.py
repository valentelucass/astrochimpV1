from pathlib import Path
from datetime import datetime, timezone
import pymupdf

path = Path('AstroChimp-Documentacao.pdf')
document = pymupdf.open(path)
metadata = document.metadata.copy()
metadata['modDate'] = datetime.now(timezone.utc).strftime('D:%Y%m%d%H%M%SZ')
document.set_metadata(metadata)
document.saveIncr()
assert len(document) == 10
text = document[4].get_text()
for name in ['Miguel Conceição', 'Pedro Gabriel', 'Lucas Mateus Andrade', 'Guilherme Lettieri']:
    assert name in text
for name, count in [('Guilherme Lettieri', 6), ('Lucas Mateus Andrade', 5), ('Miguel Conceição', 3), ('Pedro Gabriel', 3)]:
    assert text.count(name) == count, (name, text.count(name))
document[4].get_pixmap(matrix=pymupdf.Matrix(2.4, 2.4), clip=pymupdf.Rect(50, 50, 545, 540)).save('AstroChimp-Responsabilidades.png')
document.close()
print('PDF final validado e imagem das responsabilidades salva.')
