// Retira uma barra no fim do endereço para não duplicá-la ao acrescentar a rota da API.
export function retirarBarraFinal(endereco: string) {
  if (endereco.endsWith('/')) return endereco.substring(0, endereco.length - 1);
  return endereco;
}

// Mantém até seis letras maiúsculas ou números no código digitado pelo aluno.
export function limparCodigoSala(texto: string) {
  const permitidos = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  const maiusculas = texto.toUpperCase();
  let codigo = '';
  for (let i = 0; i < maiusculas.length && codigo.length < 6; i++) {
    const caractere = maiusculas[i];
    if (permitidos.includes(caractere)) codigo += caractere;
  }
  return codigo;
}

// Um código pronto precisa ter seis caracteres e já estar no formato aceito na digitação.
export function codigoSalaValido(codigo: string) {
  return codigo.length === 6 && limparCodigoSala(codigo) === codigo;
}

// Confere um único @, um nome antes dele e um domínio com ponto, sem espaços no meio.
export function emailValido(texto: string) {
  const email = texto.trim();
  const arroba = email.indexOf('@');
  const ultimoPonto = email.lastIndexOf('.');
  if (arroba <= 0 || arroba !== email.lastIndexOf('@')) return false;
  if (ultimoPonto <= arroba + 1 || ultimoPonto === email.length - 1) return false;
  for (let i = 0; i < email.length; i++) {
    if (email[i].trim() === '') return false;
  }
  return true;
}

// O nome do endereço público aceita letras minúsculas, números e hífen.
export function enderecoPublicoValido(texto: string) {
  const endereco = texto.trim();
  const permitidos = 'abcdefghijklmnopqrstuvwxyz0123456789-';
  if (endereco === '') return false;
  for (let i = 0; i < endereco.length; i++) {
    if (!permitidos.includes(endereco[i])) return false;
  }
  return true;
}

// Aceita páginas http ou https com conteúdo após o prefixo e sem espaços no meio.
export function enderecoWebValido(texto: string) {
  const endereco = texto.trim().toLowerCase();
  let tamanhoDoInicio = 0;
  if (endereco.startsWith('http://')) tamanhoDoInicio = 7;
  else if (endereco.startsWith('https://')) tamanhoDoInicio = 8;
  else return false;
  if (endereco.length === tamanhoDoInicio) return false;
  for (let i = 0; i < endereco.length; i++) {
    if (endereco[i].trim() === '') return false;
  }
  return true;
}

// Troca caracteres fora da lista por sublinhado, impedindo barras no nome do arquivo baixado.
export function nomeArquivoSeguro(nome: string) {
  const permitidos = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-';
  let resultado = '';
  for (let i = 0; i < nome.length; i++) {
    const caractere = nome[i];
    if (permitidos.includes(caractere)) resultado += caractere;
    else resultado += '_';
  }
  return resultado;
}
