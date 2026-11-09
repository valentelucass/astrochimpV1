import { View, Text, TextInput, StyleSheet } from 'react-native'; // Importa os elementos do React Native usados para montar o rótulo, a entrada de texto e seus estilos.
import cores from '../styles/cores'; // Importa as cores do projeto para deixar texto, borda e fundo iguais aos demais campos.
import estilos from '../styles/estilos'; // Importa os estilos compartilhados para reutilizar a aparência padrão dos rótulos.

import type { StyleProp, ViewStyle } from 'react-native'; // Importa os tipos usados somente no estilo externo do campo.

// Lista as opções que os formulários usam; valor é o texto atual e onChangeText recebe a nova digitação.
type CampoProps = {
  rotulo: string;
  valor: string;
  onChangeText: (texto: string) => void;
  placeholder?: string;
  multiline?: boolean;
  maxLength?: number;
  secureTextEntry?: boolean;
  editable?: boolean;
  autoCorrect?: boolean;
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  autoComplete?: 'email' | 'new-password' | 'current-password' | 'off';
  keyboardType?: 'default' | 'email-address' | 'url' | 'number-pad';
  onSubmitEditing?: () => void;
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
  maxLength,
  secureTextEntry,
  editable,
  autoCorrect,
  autoCapitalize,
  autoComplete,
  keyboardType,
  onSubmitEditing,
}: CampoProps) {
  return (
    <View style={[styles.campo, style]}>
      <Text style={[estilos.rotulo, styles.rotulo]}>{rotulo}</Text>
      {/* value mostra valor; onChangeText entrega o novo texto à página para ela atualizar seu estado. */}
      {/* Cada opção abaixo é recebida do formulário e entregue diretamente ao campo do React Native. */}
      <TextInput
        accessibilityLabel={rotulo}
        value={valor}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#9BA8AF"
        multiline={multiline}
        style={[styles.input, multiline && styles.multiline]}
        maxLength={maxLength}
        secureTextEntry={secureTextEntry}
        editable={editable}
        autoCorrect={autoCorrect}
        autoCapitalize={autoCapitalize}
        autoComplete={autoComplete}
        keyboardType={keyboardType}
        onSubmitEditing={onSubmitEditing}
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
