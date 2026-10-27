import type { Material, Acesso } from '../types/modelos';
import { Linking, Platform } from 'react-native';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import * as Clipboard from 'expo-clipboard';
import { ENDERECO_API, cabecalhosDeAcesso } from './api';

// Recebe o material escolhido e decide entre abrir um endereço, copiar código ou baixar um arquivo.
// salaId acompanha o download quando o acesso do aluno depende da participação naquela aula.
export async function abrirMaterial(material: Material, acesso: Acesso = null, salaId?: number) {
  if (material.tipo === 'link') {
    // Só abrimos endereços de páginas http/https; isso recusa outros formatos colados no cadastro.
    const url = material.conteudoOuUrl.trim();
    if (!/^https?:\/\//i.test(url))
      throw new Error('Este material precisa de um endereço http ou https válido.');
    await Linking.openURL(url);
    return;
  }
  if (material.tipo === 'codigo') {
    // A biblioteca de Clipboard copia o conteúdo para a área de transferência usada pelo botão colar.
    const copiou = await Clipboard.setStringAsync(material.conteudoOuUrl);
    if (!copiou) throw new Error('Não foi possível copiar o código. Tente novamente.');
    return;
  }
  // Arquivos privados precisam dos mesmos cabeçalhos de acesso usados nas consultas à sala.
  const url =
    ENDERECO_API +
    '/api/materiais/' +
    material.id +
    '/arquivo' +
    (salaId ? '?salaId=' + salaId : '');
  const headers = cabecalhosDeAcesso(acesso);
  // No upload, detalhe traz o nome original. Substituímos caracteres que não queremos no nome salvo no aparelho.
  const nome = (material.detalhe || material.titulo || 'material').replace(/[^a-zA-Z0-9._-]/g, '_');
  if (Platform.OS === 'web') {
    // Na prévia, fetch baixa os bytes com a identificação do usuário e blob guarda esses bytes como arquivo.
    const resposta = await fetch(url, { headers });
    if (!resposta.ok) throw new Error('Não foi possível baixar este arquivo.');
    const arquivo = await resposta.blob();
    // O endereço temporário permite ao navegador baixar o conteúdo recebido usando o nome do material.
    const endereco = URL.createObjectURL(arquivo);
    const link = document.createElement('a');
    link.href = endereco;
    link.download = nome;
    link.click();
    // Libera o endereço da memória depois de o navegador iniciar o download.
    setTimeout(() => URL.revokeObjectURL(endereco), 1000);
    return;
  }
  // No celular, salvamos no cache temporário do aplicativo antes de oferecer abrir ou compartilhar.
  if (!FileSystem.cacheDirectory)
    throw new Error('O aparelho não disponibilizou uma pasta para baixar o arquivo.');
  const destino = FileSystem.cacheDirectory + material.id + '_' + nome;
  const resultado = await FileSystem.downloadAsync(url, destino, { headers });
  if (resultado.status !== 200) throw new Error('Não foi possível baixar este arquivo.');
  // A folha de compartilhamento do sistema permite escolher um aplicativo que aceite o arquivo baixado.
  if (await Sharing.isAvailableAsync()) await Sharing.shareAsync(resultado.uri);
  else throw new Error('O aparelho não conseguiu abrir o arquivo baixado.');
}
