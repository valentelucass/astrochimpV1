import { View, Text, Image, StyleSheet } from 'react-native';
import cores from '../styles/cores';

// A legenda é opcional e identifica a área do aplicativo, como o hub do professor.
type MarcaProps = { legenda?: string };

// Mostra a marca no fim da página usando a imagem do mascote que fica na pasta assets.
export default function Marca({ legenda }: MarcaProps) {
  return (
    <View style={styles.marca}>
      <View style={styles.linha}>
        <Image source={require('../../assets/mascote.png')} style={styles.imagem} />
        <Text style={styles.nome}>AstroChimp</Text>
      </View>
      {/* Sem legenda, o rodapé fica apenas com o mascote e o nome AstroChimp. */}
      {legenda ? <Text style={styles.legenda}>{legenda}</Text> : null}
    </View>
  );
}

// StyleSheet.create deixa o rodapé centralizado, com imagem pequena e menos destaque que os botões da tela.
const styles = StyleSheet.create({
  marca: { alignItems: 'center', paddingTop: 26, paddingBottom: 8, opacity: 0.55 },
  linha: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  imagem: { width: 29, height: 29, resizeMode: 'contain' },
  nome: { fontFamily: 'Nunito_800ExtraBold', fontSize: 18, color: cores.azulEscuro },
  legenda: {
    fontFamily: 'Nunito_700Bold',
    fontSize: 10,
    letterSpacing: 1.6,
    color: cores.textoSuave,
    marginTop: 5,
  },
});
