// No celular, o Expo recebe uri, nome, tipo e uma função que lê os bytes do arquivo. No navegador, usamos File.
// A assinatura exige bytes para evitar enviar somente o endereço, formato antigo recusado pelo Expo 57.
interface FormData {
  append(nome: string, arquivo: { uri: string; name: string; type: string; bytes: () => Promise<Uint8Array> }): void;
}
