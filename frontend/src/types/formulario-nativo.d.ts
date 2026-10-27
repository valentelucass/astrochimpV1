// No React Native, o arquivo escolhido tem uri, nome e tipo. No navegador, usamos File.
// Esta assinatura descreve o formato aceito pelo FormData nativo ao enviar um arquivo.
interface FormData {
  append(nome: string, arquivo: { uri: string; name: string; type: string }): void;
}
