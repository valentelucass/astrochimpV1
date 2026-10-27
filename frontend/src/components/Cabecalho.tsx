import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

import type { ComponentProps } from 'react';

// Cada tela informa seus títulos e pode oferecer uma ação à esquerda e outra à direita.
type CabecalhoProps = {
  titulo: string;
  subtitulo?: string;
  onVoltar?: () => void;
  onAcao?: () => void;
  acaoIcone?: ComponentProps<typeof Ionicons>['name'];
  acaoRotulo?: string;
};

// Desenha o topo da página com o nome da tela e os botões de navegação recebidos por props.
export default function Cabecalho({
  titulo,
  subtitulo,
  onVoltar,
  onAcao,
  acaoIcone = 'settings-outline',
  acaoRotulo = 'Opções',
}: CabecalhoProps) {
  return (
    <View style={styles.cabecalho}>
      {/* A seta só aparece se a página informar para onde a pessoa pode voltar. */}
      {onVoltar ? (
        <TouchableOpacity
          accessibilityRole="button"
          accessibilityLabel="Voltar"
          style={styles.voltar}
          onPress={onVoltar}
        >
          <Ionicons name="arrow-back" size={22} color={cores.azulEscuro} />
        </TouchableOpacity>
      ) : null}
      {/* O subtítulo complementa o título quando existe, como o código ou o nome de uma sala. */}
      <View style={styles.titulos}>
        <Text style={[estilos.subtitulo, styles.titulo]}>{titulo}</Text>
        {subtitulo ? <Text style={estilos.legenda}>{subtitulo}</Text> : null}
      </View>
      {/* A ação da direita muda conforme a tela, como abrir o perfil ou os materiais da aula. */}
      {onAcao ? (
        <TouchableOpacity
          accessibilityRole="button"
          accessibilityLabel={acaoRotulo}
          onPress={onAcao}
          style={styles.voltar}
        >
          <Ionicons name={acaoIcone} size={23} color={cores.azulEscuro} />
        </TouchableOpacity>
      ) : null}
    </View>
  );
}

// StyleSheet.create organiza o topo em uma linha; titulos ocupa o espaço entre os botões.
const styles = StyleSheet.create({
  cabecalho: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 18,
    paddingTop: 22,
    backgroundColor: cores.branco,
    borderBottomWidth: 3,
    borderBottomColor: cores.borda,
  },
  voltar: {
    height: 42,
    width: 42,
    borderWidth: 1.5,
    borderBottomWidth: 4,
    borderColor: '#CFDDE3',
    borderRadius: 17,
    backgroundColor: cores.branco,
    alignItems: 'center',
    justifyContent: 'center',
  },
  titulos: { flex: 1 },
  titulo: { fontSize: 18 },
});
