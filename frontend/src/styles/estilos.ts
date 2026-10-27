import { StyleSheet } from 'react-native';
import cores from './cores';

// StyleSheet.create reúne estilos que várias telas reutilizam, como títulos e textos de formulário.
const estilos = StyleSheet.create({
  // A fonte comum, o título maior e o subtítulo definem a ordem de leitura do conteúdo.
  texto: { fontFamily: 'Nunito_400Regular', fontSize: 16, color: cores.texto, lineHeight: 24 },
  titulo: { fontFamily: 'Nunito_800ExtraBold', fontSize: 26, color: cores.texto, lineHeight: 34 },
  subtitulo: { fontFamily: 'Nunito_700Bold', fontSize: 19, color: cores.texto, lineHeight: 27 },
  // Legendas mostram informações secundárias, como a data de um material.
  legenda: {
    fontFamily: 'Nunito_400Regular',
    fontSize: 13,
    color: cores.textoSuave,
    lineHeight: 19,
  },
  // Rótulos identificam campos e pequenas seções usando letras mais fortes e espaçadas.
  rotulo: {
    fontFamily: 'Nunito_800ExtraBold',
    fontSize: 12,
    letterSpacing: 1,
    color: cores.azulEscuro,
  },
  // Estes dois estilos alinham itens lado a lado e deixam um intervalo abaixo de um bloco.
  linha: { flexDirection: 'row', alignItems: 'center' },
  espaco: { marginBottom: 20 },
  // Quando uma lista não tem registros, a explicação fica centralizada e com menos destaque que um título.
  vazio: {
    fontFamily: 'Nunito_400Regular',
    fontSize: 16,
    color: cores.textoSuave,
    textAlign: 'center',
    padding: 28,
  },
});

export default estilos;
