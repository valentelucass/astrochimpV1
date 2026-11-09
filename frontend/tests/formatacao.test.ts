import { test } from 'node:test'; // Executa os testes sem adicionar outra biblioteca ao projeto.
import assert from 'node:assert/strict'; // Compara o resultado recebido com o resultado esperado.
import { formatarTempo, formatarData, iniciais } from '../src/services/formatacao.ts'; // Usa as funções mostradas nas telas.

// Cada linha compara um tempo em segundos com a apresentação esperada no aplicativo.
test('formata tempo e trata valores ausentes, decimais e negativos', () => {
  const exemplos: { segundos: number | null | undefined; esperado: string }[] = [
    { segundos: 0, esperado: '00:00' },
    { segundos: 5, esperado: '00:05' },
    { segundos: 60, esperado: '01:00' },
    { segundos: 125, esperado: '02:05' },
    { segundos: 125.9, esperado: '02:05' },
    { segundos: -4, esperado: '00:00' },
    { segundos: null, esperado: '00:00' },
    { segundos: undefined, esperado: '00:00' },
  ];
  for (const exemplo of exemplos) assert.equal(formatarTempo(exemplo.segundos), exemplo.esperado);
});

// Testa nomes com um ou mais termos e também a ausência de nome.
test('mostra iniciais do primeiro e do último nome', () => {
  const exemplos: { nome: string | null | undefined; esperado: string }[] = [
    { nome: 'Ana Souza', esperado: 'AS' },
    { nome: '  maria   da silva  ', esperado: 'MS' },
    { nome: 'Ana Andrade', esperado: 'AA' },
    { nome: 'Lucas', esperado: 'L' },
    { nome: '', esperado: '?' },
    { nome: '   ', esperado: '?' },
    { nome: null, esperado: '?' },
    { nome: undefined, esperado: '?' },
  ];
  for (const exemplo of exemplos) assert.equal(iniciais(exemplo.nome), exemplo.esperado);
});

// A data vazia não deve exibir uma data inventada na interface.
test('deixa a data vazia quando ela não foi informada', () => {
  assert.equal(formatarData(null), '');
  assert.equal(formatarData(undefined), '');
  assert.equal(formatarData(''), '');
});
