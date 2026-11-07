import { View, Text, TextInput, StyleSheet } from 'react-native'; // Importa os elementos do React Native usados para montar o rótulo, a entrada de texto e seus estilos.
import cores from '../styles/cores'; // Importa as cores do projeto para deixar texto, borda e fundo iguais aos demais campos.
import estilos from '../styles/estilos'; // Importa os estilos compartilhados para reutilizar a aparência padrão dos rótulos.

import type { TextInputProps, StyleProp, ViewStyle } from 'react-native'; // Importa somente os tipos do React Native para descrever as opções e o estilo externo do campo.

// Omit reaproveita as opções do TextInput, como teclado e senha, retirando os nomes que definimos aqui.
// valor é o texto atual; style muda a caixa externa do campo, sem substituir o estilo do texto digitado.
type CampoProps = Omit<TextInputProps, 'style' | 'value'> & {
  rotulo: string;
  valor: string;
  style?: StyleProp<ViewStyle>;
};

// Junta o rótulo e a área de digitação usados nos formulários de login, dúvidas e conteúdos.
export default function Campo({
  rotulo,
  valor,
  onChangeText,
  placeholder,
  multiline = false,
  style,
  ...outrasPropriedades
}: CampoProps) {
  return (
    <View style={[styles.campo, style]}>
      <Text style={[estilos.rotulo, styles.rotulo]}>{rotulo}</Text>
      {/* value mostra valor; onChangeText entrega o novo texto à página para ela atualizar seu estado. */}
      {/* As outras opções incluem maxLength e secureTextEntry, escolhidas por cada formulário. */}
      <TextInput
        accessibilityLabel={rotulo}
        value={valor}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#9BA8AF"
        multiline={multiline}
        style={[styles.input, multiline && styles.multiline]}
        {...outrasPropriedades}
      />
    </View>
  );
}

// StyleSheet.create guarda as medidas do campo; multiline aumenta sua altura e começa o texto no topo.
const styles = StyleSheet.create({
  campo: { marginBottom: 18 },
  rotulo: { marginBottom: 8 },
  input: {
    fontFamily: 'Nunito_400Regular',
    fontSize: 16,
    color: cores.texto,
    minHeight: 51,
    borderWidth: 2,
    borderColor: cores.borda,
    backgroundColor: '#FBFDFF',
    borderRadius: 15,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  multiline: { minHeight: 115, textAlignVertical: 'top' },
});
