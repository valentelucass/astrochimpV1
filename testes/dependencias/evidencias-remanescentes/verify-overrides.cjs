// Exercita os consumidores reais do Expo e compara padrões usados na seleção de arquivos.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
function load(root) {
  const project = createRequire(path.join(root, 'package.json'));
  const expo = createRequire(project.resolve('expo/package.json'));
  const entry = expo.resolve('@expo/cli');
  const cli = createRequire(entry);
  const cliBase = path.resolve(path.dirname(entry), '..', 'src');
  const fp = expo.resolve('@expo/fingerprint');
  const fingerprint = createRequire(fp);
  return { project, expo, cli, cliBase, fingerprint, glob: cli('glob'), minimatch: fingerprint('minimatch'), fpPaths: fingerprint('./utils/Path.js') };
}
const current = load(process.cwd());
const baselineRoot = process.argv[2];
const baseline = baselineRoot ? load(baselineRoot) : null;
const logs = [];
function passed(text) { logs.push(text); console.log('PASS: ' + text); }
(async () => {
  assert.equal(current.cli('zod/package.json').version, '4.6.7');
  assert.equal(current.cli('glob/package.json').version, '14.0.3');
  assert.equal(current.fingerprint('minimatch/package.json').version, '11.0.4');
  passed('Versões corrigidas resolvidas pelos consumidores do Expo.');

  const { PluginSchema, DevToolsPluginOutputSchema } = require(path.join(current.cliBase, 'start/server/DevToolsPlugin.schema.js'));
  const plugin = { packageName: 'astrochimp-fixture', packageRoot: process.cwd(), cliExtensions: { description: 'Verificacao local', entryPoint: 'local.cjs', commands: [{ name: 'inspect', title: 'Inspecionar', environments: ['cli', 'mcp'], parameters: [{ name: 'title', type: 'text' }, { name: 'count', type: 'number' }, { name: 'ok', type: 'confirm' }] }] } };
  const output = [{ type: 'text', text: 'ok', level: 'info' }, { type: 'image', url: 'https://example.invalid/image.png' }, { type: 'audio', url: 'https://example.invalid/audio.wav' }];
  assert.equal(PluginSchema.safeParse(plugin).success, true);
  assert.equal(PluginSchema.safeParse({ packageName: 1 }).success, false);
  assert.equal(DevToolsPluginOutputSchema.safeParse(output).success, true);
  assert.equal(DevToolsPluginOutputSchema.safeParse([{ type: 'text', text: 1 }]).success, false);
  const { DevToolsPlugin } = require(path.join(current.cliBase, 'start/server/DevToolsPlugin.js'));
  assert.equal(new DevToolsPlugin(plugin, process.cwd()).packageName, plugin.packageName);
  assert.throws(() => new DevToolsPlugin({ packageName: 1 }, process.cwd()), /Invalid plugin/);
  const { DevToolsPluginCliExtensionResults } = require(path.join(current.cliBase, 'start/server/DevToolsPluginCliExtensionResults.js'));
  const results = new DevToolsPluginCliExtensionResults();
  assert.deepEqual(results.parseOutputText(JSON.stringify(output)), output);
  assert.equal(results.parseOutputText('[{"type":"text","text":1}]')[0].level, 'error');
  const { createMCPDevToolsExtensionSchema } = require(path.join(current.cliBase, 'start/server/createMCPDevToolsExtensionSchema.js'));
  const schema = createMCPDevToolsExtensionSchema(plugin);
  assert.equal(schema.safeParse({ command: 'inspect', title: 'a', count: 2, ok: true }).success, true);
  assert.equal(schema.safeParse({ command: 'missing' }).success, false);
  assert.equal(schema.safeParse({ command: 'inspect', count: 'wrong' }).success, false);
  assert.equal(schema.safeParse({ command: 'inspect', extra: 1 }).success, false);
  if (baseline) {
    const old = require(path.join(baseline.cliBase, 'start/server/DevToolsPlugin.schema.js'));
    assert.deepEqual(PluginSchema.parse(plugin), old.PluginSchema.parse(plugin));
    assert.deepEqual(DevToolsPluginOutputSchema.parse(output), old.DevToolsPluginOutputSchema.parse(output));
  }
  passed('Schemas, construtor e resultados reais de plugins Expo e parâmetros MCP aceitam/rejeitam os casos esperados.');

  // A correção oficial deve recusar uma coleção acima do limite antes de visitar os elementos.
  const { z } = current.cli('zod');
  let visited = 0;
  const bounded = z.array(z.preprocess(value => { visited++; return value; }, z.string())).max(10);
  const rejected = bounded.safeParse(Array(1000).fill(0));
  assert.equal(rejected.success, false);
  assert.equal(visited, 0);
  assert.equal(rejected.error.issues.length, 1);
  passed('Zod rejeita array limitado excessivo sem percorrer elementos.');

  const fixture = path.join(__dirname, 'glob-fixture');
  const names = ['app.json', 'index.ts', 'App.tsx', '.hidden', 'src/a.ts', 'src/nested/b.tsx', 'src/c.js', 'assets/icon.png', 'node_modules/pkg/a.js', 'android/app/build.gradle', 'ios/App.xcodeproj/project.pbxproj'];
  for (const name of names) { const file = path.join(fixture, name); fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, 'fixture'); }
  const patterns = ['**/*', '**/*.{ts,tsx}', 'src/**', 'assets/**/*', 'ios/*.xcodeproj', '**/build.gradle'];
  let checks = 0;
  for (const pattern of patterns) {
    const options = { cwd: fixture, nodir: true, dot: true, ignore: ['node_modules/**'] };
    const nowSync = current.glob.globSync(pattern, options).sort();
    const nowAsync = (await current.glob.glob(pattern, options)).sort();
    assert.deepEqual(nowSync, nowAsync);
    if (baseline) assert.deepEqual(nowSync, baseline.glob.globSync(pattern, options).sort());
    checks++;
  }
  const ignores = [['**/node_modules/**'], ['src/**', '!src/a.ts'], ['**/*.js'], ['assets/**/*'], ['android/**', 'ios/**']];
  for (const ignore of ignores) for (const name of names) {
    const matches = current.fpPaths.isIgnoredPath(name, ignore);
    if (baseline) assert.equal(matches, baseline.fpPaths.isIgnoredPath(name, ignore));
    checks++;
  }
  passed(`${checks} comparações de glob/fingerprint preservam seleção de arquivos; caminhos sync/async conferem.`);

  const controller = new AbortController(); controller.abort();
  let reads = 0;
  const wrappedFs = { ...fs, readdir(...args) { reads++; return fs.readdir(...args); }, readdirSync(...args) { reads++; return fs.readdirSync(...args); } };
  await assert.rejects(current.glob.glob('**/*', { cwd: fixture, signal: controller.signal, fs: wrappedFs }));
  await new Promise(resolve => setTimeout(resolve, 50));
  assert.equal(reads, 0);
  const { anyMatchAsync } = require(path.join(current.cliBase, 'utils/glob.js'));
  assert.equal((await anyMatchAsync('**/*.ts', { cwd: fixture })).length, 1);
  passed('Glob respeita sinal já cancelado sem readdir; utilitário real anyMatchAsync do Expo retorna um arquivo.');
  fs.writeFileSync(path.join(__dirname, 'override-checks.json'), JSON.stringify({ checkedAt: new Date().toISOString(), comparisonBaseline: baselineRoot || null, passed: logs }, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
