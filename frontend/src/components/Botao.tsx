import { TouchableOpacity, Text, ActivityIndicator, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import cores from '../styles/cores';

import type { ComponentProps } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

// As props escolhem o texto, a ação e a aparência do botão. O ponto de interrogação marca opções dispensáveis.
type BotaoProps = {
  titulo: string;
  onPress: () => void;
  cor?: 'verde' | 'azul' | 'laranja' | 'roxo' | 'claro';
  icone?: ComponentProps<typeof Ionicons>['name'];
  carregando?: boolean;
  desabilitado?: boolean;
  pequeno?: boolean;
  style?: StyleProp<ViewStyle>;
};

// Reúne o botão usado nas telas para manter iguais a borda, o texto e a espera durante um pedido.
export default function Botao({
  titulo,
  onPress,
  cor = 'verde',
  icone,
  carregando = false,
  desabilitado = false,
  pequeno = false,
  style,
}: BotaoProps) {
  // A opção cor, como verde, escolhe um fundo e uma borda mais escura para dar altura ao botão.
  const fundos = {
    verde: cores.verde,
    azul: cores.azul,
    laranja: cores.laranja,
    roxo: cores.roxo,
    claro: cores.azulClaro,
  };
  const sombras = {
    verde: cores.verdeEscuro,
    azul: cores.azulEscuro,
    laranja: cores.laranjaEscuro,
    roxo: cores.roxoEscuro,
    claro: cores.borda,
  };
  // O botão claro precisa de letras escuras para o título continuar legível.
  const corTexto = cor === 'claro' ? cores.azulEscuro : cores.branco;
  // Enquanto um pedido está carregando, recusamos outros toques no mesmo botão.
  const bloqueado = desabilitado || carregando;

  // TouchableOpacity executa onPress ao tocar. Os dados de acessibilidade descrevem o botão ao leitor de tela.
  return (
    <TouchableOpacity
      accessibilityRole="button"
      accessibilityLabel={titulo}
      accessibilityState={{ disabled: bloqueado, busy: carregando }}
      activeOpacity={0.8}
      onPress={onPress}
      disabled={bloqueado}
      style={[
        styles.botao,
        { backgroundColor: fundos[cor], borderBottomColor: sombras[cor] },
        pequeno && styles.pequeno,
        bloqueado && styles.bloqueado,
        style,
      ]}
    >
      {/* A espera ocupa o lugar do ícone, mas o título continua visível para identificar a ação. */}
      {carregando ? (
        <ActivityIndicator color={corTexto} />
      ) : icone ? (
        <Ionicons name={icone} size={pequeno ? 18 : 23} color={corTexto} />
      ) : null}
      <Text style={[styles.texto, { color: corTexto }, pequeno && styles.textoPequeno]}>
        {titulo}
      </Text>
    </TouchableOpacity>
  );
}

// StyleSheet.create reúne as medidas deste componente; pequeno reduz o botão e bloqueado diminui sua opacidade.
const styles = StyleSheet.create({
  botao: {
    minHeight: 58,
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderRadius: 25,
    borderBottomWidth: 6,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 9,
  },
  pequeno: {
    minHeight: 43,
    paddingVertical: 9,
    paddingHorizontal: 13,
    borderRadius: 18,
    borderBottomWidth: 4,
  },
  texto: { fontFamily: 'Nunito_800ExtraBold', fontSize: 17, textAlign: 'center' },
  textoPequeno: { fontSize: 12 },
  bloqueado: { opacity: 0.48 },
});
