from pathlib import Path
import re

path = Path('DOCUMENTACAO.md')
text = path.read_text(encoding='utf-8')
start = text.index('### 1.4 Responsabilidades de Cada Integrante')
end = text.index('## 2. Telas e navegação', start)
section = text[start:end].replace('### 1.4 ', '## 5. ', 1)
text = text[:start] + text[end:]
text = re.sub(r'^(#{2,3} )(\d+)([. ].*)$', lambda match: match[1] + str(int(match[2]) + 1) + match[3] if int(match[2]) >= 5 else match[0], text, flags=re.MULTILINE)
index = text.index('## 6. Arquitetura e organização')
text = text[:index] + section + text[index:]
path.write_text(text, encoding='utf-8')
