import { View, Text, StyleSheet } from 'react-native'; // Importa os elementos do React Native usados para montar a caixa e o texto do aviso.
import { Ionicons } from '@expo/vector-icons'; // Importa os ícones do Expo para diferenciar visualmente mensagens de sucesso e de erro.
import cores from '../styles/cores'; // Importa as cores do projeto para aplicar os tons de confirmação e de falha.

// A página fornece o aviso e informa sucesso quando quer confirmar uma ação concluída.
type MensagemProps = { texto: string; tipo?: 'erro' | 'sucesso' };

// Centraliza os avisos dos formulários; uma mensagem vazia não ocupa lugar na página.
export default function Mensagem({ texto, tipo = 'erro' }: MensagemProps) {
  if (!texto) return null;
  // A mesma condição escolhe o ícone e a cor verde de confirmação ou vermelha de erro.
  const sucesso = tipo === 'sucesso';
  return (
    <View accessibilityRole="alert" style={[styles.caixa, sucesso && styles.sucesso]}>
      <Ionicons
        name={sucesso ? 'checkmark-circle-outline' : 'alert-circle-outline'}
        size={20}
        color={sucesso ? cores.verdeEscuro : cores.erro}
      />
      <Text style={[styles.texto, sucesso && { color: cores.verdeEscuro }]}>{texto}</Text>
    </View>
  );
}

// StyleSheet.create mantém ícone e texto lado a lado, permitindo que avisos longos quebrem a linha.
const styles = StyleSheet.create({
  caixa: {
    padding: 13,
    marginBottom: 16,
    borderRadius: 13,
    backgroundColor: cores.erroClaro,
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 9,
  },
  sucesso: { backgroundColor: cores.verdeClaro },
  texto: { flex: 1, fontFamily: 'Nunito_700Bold', fontSize: 14, color: cores.erro, lineHeight: 21 },
});
