import {
  ScrollView,
  View,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from 'react-native'; // Importa os elementos do React Native que cuidam da rolagem, teclado, conteúdo e estilos da página.
import { SafeAreaView } from 'react-native-safe-area-context'; // Importa a área segura que reserva espaço para as barras e recortes reais do dispositivo.
import cores from '../styles/cores'; // Importa as cores do projeto para aplicar o fundo padrão em todas as páginas.

import type { ReactNode } from 'react'; // Importa somente o tipo ReactNode para aceitar o conteúdo colocado dentro da página.
import type { StyleProp, ViewStyle } from 'react-native'; // Importa somente os tipos de estilo para permitir ajustes externos no conteúdo da página.

// children recebe a tela; rolar=false é usado quando ela já tem uma FlatList cuidando da rolagem.
type PaginaProps = { children: ReactNode; rolar?: boolean; style?: StyleProp<ViewStyle> };

// Prepara o fundo e a área útil de todas as telas, incluindo o espaço que o teclado ocupa no iPhone.
export default function Pagina({ children, rolar = true, style }: PaginaProps) {
  return (
    // A área segura afasta o conteúdo das barras do sistema; KeyboardAvoidingView ajusta o espaço do teclado.
    <SafeAreaView style={styles.fundo}>
      <KeyboardAvoidingView
        style={styles.pagina}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {/* Formulários usam ScrollView para alcançar campos abaixo da tela; listas recebem uma View comum. */}
        {rolar ? (
          <ScrollView
            contentContainerStyle={[styles.conteudo, style]}
            keyboardShouldPersistTaps="handled"
          >
            {children}
          </ScrollView>
        ) : (
          <View style={[styles.semRolagem, style]}>{children}</View>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

// StyleSheet.create centraliza a página e limita a largura da prévia dentro da área segura.
const styles = StyleSheet.create({
  fundo: { flex: 1, backgroundColor: '#EAF1F6' },
  pagina: {
    flex: 1,
    alignSelf: 'center',
    width: '100%',
    maxWidth: 480,
    backgroundColor: cores.fundo,
  },
  conteudo: { flexGrow: 1, padding: 24, paddingBottom: 36 },
  semRolagem: { flex: 1 },
});
