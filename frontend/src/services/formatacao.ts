// Converte segundos da API em minutos:segundos: 125 vira 02:05 no cronômetro e no quiz.
export function formatarTempo(segundos: number | null | undefined) {
  // Sem duração usamos zero; Math.floor tira a parte decimal e Math.max evita mostrar tempo negativo.
  const total = Math.max(0, Math.floor(segundos || 0));
  const minutos = Math.floor(total / 60);
  // O resto da divisão por 60 dá os segundos; padStart completa números de um dígito com zero.
  return String(minutos).padStart(2, '0') + ':' + String(total % 60).padStart(2, '0');
}

// Mostra a data recebida no formato brasileiro, como 27/10/2026; sem data, deixa o espaço vazio.
export function formatarData(data: string | null | undefined) {
  return data ? new Date(data).toLocaleDateString('pt-BR') : '';
}

// Monta as letras do avatar sem foto: Ana Souza vira AS; um nome vazio aparece como interrogação.
export function iniciais(nome: string | null | undefined) {
  if (!nome || !nome.trim()) return '?';
  // Separa as palavras mesmo se houver vários espaços e pega a primeira letra do primeiro e do último nome.
  const partes = nome.trim().split(/\s+/);
  let letras = partes[0][0];
  if (partes.length > 1) letras += partes[partes.length - 1][0];
  return letras.toUpperCase();
}
