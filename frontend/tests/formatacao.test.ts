import { test } from 'node:test'; // Executa os testes sem adicionar outra biblioteca ao projeto.
import assert from 'node:assert/strict'; // Compara o resultado recebido com o resultado esperado.
import { formatarTempo, formatarData, iniciais } from '../src/services/formatacao.ts'; // Usa as funções mostradas nas telas.

// Cada linha compara um tempo em segundos com a apresentação esperada no aplicativo.
test('formata tempo e trata valores ausentes, decimais e negativos', () => {
  const exemplos: Array<[number | null | undefined, string]> = [
    [0, '00:00'], [5, '00:05'], [60, '01:00'], [125, '02:05'],
    [125.9, '02:05'], [-4, '00:00'], [null, '00:00'], [undefined, '00:00'],
  ];
  for (const [segundos, esperado] of exemplos) assert.equal(formatarTempo(segundos), esperado);
});

// Testa nomes com um ou mais termos e também a ausência de nome.
test('mostra iniciais do primeiro e do último nome', () => {
  const exemplos: Array<[string | null | undefined, string]> = [
    ['Ana Souza', 'AS'], ['  maria   da silva  ', 'MS'], ['Lucas', 'L'],
    ['', '?'], ['   ', '?'], [null, '?'], [undefined, '?'],
  ];
  for (const [nome, esperado] of exemplos) assert.equal(iniciais(nome), esperado);
});

// A data vazia não deve exibir uma data inventada na interface.
test('deixa a data vazia quando ela não foi informada', () => {
  assert.equal(formatarData(null), '');
  assert.equal(formatarData(undefined), '');
  assert.equal(formatarData(''), '');
});
