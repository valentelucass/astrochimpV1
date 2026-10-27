import {
  SafeAreaView,
  ScrollView,
  View,
  KeyboardAvoidingView,
  Platform,
  StatusBar,
  StyleSheet,
} from 'react-native';
import cores from '../styles/cores';

import type { ReactNode } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

// children recebe a tela; rolar=false é usado quando ela já tem uma FlatList cuidando da rolagem.
type PaginaProps = { children: ReactNode; rolar?: boolean; style?: StyleProp<ViewStyle> };

// Prepara o fundo e a área útil de todas as telas, incluindo o espaço que o teclado ocupa no iPhone.
export default function Pagina({ children, rolar = true, style }: PaginaProps) {
  return (
    // SafeAreaView respeita a área de segurança do sistema; KeyboardAvoidingView ajusta o espaço do teclado.
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

// StyleSheet.create centraliza a página e limita a largura da prévia; no Android afasta o topo da barra do sistema.
const styles = StyleSheet.create({
  fundo: { flex: 1, backgroundColor: '#EAF1F6' },
  pagina: {
    flex: 1,
    alignSelf: 'center',
    width: '100%',
    maxWidth: 480,
    backgroundColor: cores.fundo,
    paddingTop: Platform.OS === 'android' ? StatusBar.currentHeight : 0,
  },
  conteudo: { flexGrow: 1, padding: 24, paddingBottom: 36 },
  semRolagem: { flex: 1 },
});
