// Resume os módulos dos mapas de produção sem copiar o código completo para o relatório.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(process.argv[2] || path.join(__dirname, 'export-after'));
const output = path.resolve(process.argv[3] || path.join(__dirname, 'bundle-modules-after.json'));
const targets = ['node-forge', 'zod', 'deepmerge', 'minimatch', 'glob'];
function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(dir, entry.name);
    return entry.isDirectory() ? walk(file) : [file];
  });
}
function sources(map) {
  return [...(map.sources || []), ...(map.sections || []).flatMap(section => sources(section.map))];
}
const rows = walk(root).filter(file => file.endsWith('.map')).map(file => {
  const bytes = fs.readFileSync(file);
  const all = sources(JSON.parse(bytes));
  const matches = Object.fromEntries(targets.map(name => [name, all.filter(source => {
    const normalized = source.replaceAll('\\', '/');
    return normalized.includes('/node_modules/' + name + '/') || normalized.includes('/' + name + '@');
  })]));
  return { file: path.relative(root, file).replaceAll('\\', '/'), sha256: crypto.createHash('sha256').update(bytes).digest('hex'), sourceCount: all.length, matches };
});
fs.writeFileSync(output, JSON.stringify({ checkedAt: new Date().toISOString(), method: 'sources dos mapas Metro de produção; inclui sections recursivamente', targets, maps: rows }, null, 2));
console.log(JSON.stringify(rows, null, 2));
