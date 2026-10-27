import { View, StyleSheet } from 'react-native';
import cores from '../styles/cores';

import type { ReactNode } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

// children recebe o conteúdo entre <Cartao> e </Cartao>, como o texto e os botões de uma dúvida.
type CartaoProps = { children: ReactNode; style?: StyleProp<ViewStyle>; corDaBorda?: string };

// A página pode mudar a borda para destacar um item fixado ou acrescentar medidas pelo style.
export default function Cartao({ children, style, corDaBorda = cores.borda }: CartaoProps) {
  return <View style={[styles.cartao, { borderColor: corDaBorda }, style]}>{children}</View>;
}

// StyleSheet.create define o fundo branco e os cantos arredondados; a borda inferior mais grossa dá relevo.
const styles = StyleSheet.create({
  cartao: {
    backgroundColor: cores.branco,
    borderWidth: 2,
    borderBottomWidth: 6,
    borderRadius: 27,
    padding: 22,
    marginBottom: 20,
  },
});
