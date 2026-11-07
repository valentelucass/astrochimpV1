// Declara apenas as funções do Node usadas pelos testes de formatação.
declare module 'node:test' {
  export function test(nome: string, executar: () => void): void;
}

declare module 'node:assert/strict' {
  const assert: { equal(recebido: unknown, esperado: unknown): void };
  export default assert;
}
