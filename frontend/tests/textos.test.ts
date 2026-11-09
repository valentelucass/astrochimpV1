import { test } from 'node:test'; // Executa os testes das validações e da preparação de textos sem outra biblioteca.
import assert from 'node:assert/strict'; // Compara as respostas das funções com os valores esperados.
import {
  retirarBarraFinal,
  limparCodigoSala,
  codigoSalaValido,
  emailValido,
  enderecoPublicoValido,
  enderecoWebValido,
  nomeArquivoSeguro,
} from '../src/services/textos.ts'; // Testa as funções usadas pelos formulários, pela conexão e pelo download.

// O endereço continua igual, retirando somente a última barra quando ela existe.
test('prepara o endereço da API sem alterar o restante do texto', function () {
  assert.equal(retirarBarraFinal('http://localhost:8080/'), 'http://localhost:8080');
  assert.equal(retirarBarraFinal('https://exemplo.com/api'), 'https://exemplo.com/api');
  assert.equal(retirarBarraFinal('https://exemplo.com//'), 'https://exemplo.com/');
  assert.equal(retirarBarraFinal(''), '');
});

// A digitação continua aceitando apenas seis letras maiúsculas ou números.
test('limpa o código digitado e recusa códigos incompletos', function () {
  assert.equal(limparCodigoSala('ab-12 cd!789'), 'AB12CD');
  assert.equal(limparCodigoSala('á!b2'), 'B2');
  assert.equal(codigoSalaValido('AB12CD'), true);
  for (const codigo of ['', 'ABC', 'AB12CD7', 'ab12cd', 'AB 12!']) {
    assert.equal(codigoSalaValido(codigo), false);
  }
});

// Aceita formatos comuns de e-mail e recusa os erros que o formulário já verificava.
test('confere nome, arroba, domínio e ausência de espaços no e-mail', function () {
  for (const email of ['ana@exemplo.com', ' Ana.Silva+curso@exemplo.com.br ', 'ANA@EXEMPLO.COM']) {
    assert.equal(emailValido(email), true);
  }
  for (const email of ['', '@exemplo.com', 'ana@', 'ana@exemplo', 'ana@@exemplo.com',
    'ana @exemplo.com', 'ana@.com', 'ana@exemplo.']) {
    assert.equal(emailValido(email), false);
  }
});

// O nome público não pode incluir barras, espaços, acentos ou letras maiúsculas.
test('limita os caracteres do endereço público', function () {
  assert.equal(enderecoPublicoValido(' ana-silva-2 '), true);
  for (const endereco of ['', 'Ana', 'ana silva', 'ana/silva', 'aná', 'ana_silva']) {
    assert.equal(enderecoPublicoValido(endereco), false);
  }
});

// Mantém links http/https e recusa outros protocolos, prefixos incompletos e espaços internos.
test('confere o início e o preenchimento dos links e fotos', function () {
  for (const endereco of ['http://exemplo.com', ' HTTPS://exemplo.com/aula?turma=1 ',
    'https://exemplo.com/foto%20nova.png']) {
    assert.equal(enderecoWebValido(endereco), true);
  }
  for (const endereco of ['', 'exemplo.com', 'http://', 'https://', 'ftp://exemplo.com',
    'javascript:alert(1)', 'https://exemplo.com/foto nova.png']) {
    assert.equal(enderecoWebValido(endereco), false);
  }
});

// Barras e caracteres especiais não podem virar partes de pastas no nome do download.
test('prepara nomes de arquivo sem caminhos de pastas', function () {
  assert.equal(nomeArquivoSeguro('aula-1_v2.pdf'), 'aula-1_v2.pdf');
  assert.equal(nomeArquivoSeguro('minha aula.pdf'), 'minha_aula.pdf');
  assert.equal(nomeArquivoSeguro('../pasta/aula.txt'), '.._pasta_aula.txt');
  assert.equal(nomeArquivoSeguro('ação.txt'), 'a__o.txt');
  assert.equal(nomeArquivoSeguro(''), '');
});
