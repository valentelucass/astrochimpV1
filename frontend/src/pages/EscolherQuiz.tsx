import { useEffect, useState } from 'react';
import { View, Text, FlatList, ActivityIndicator, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import api from '../services/api';
import type { Professor, Sala, AcessoProfessor, Quiz } from '../types/modelos';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

// Recebe o professor autenticado, a sala de destino e as ações do App para voltar, criar ou seguir após o envio.
type Props = {
  professor: Professor;
  acesso: AcessoProfessor;
  sala: Sala;
  onVoltar: () => void;
  onCriarQuiz: () => void;
  onEnviado: () => void;
};

// Permite escolher um quiz real do perfil para iniciar um desafio na sala atual.
export default function EscolherQuiz({
  professor,
  acesso,
  sala,
  onVoltar,
  onCriarQuiz,
  onEnviado,
}: Props) {
  // useState guarda a lista recebida; os setters renovam a tela conforme a consulta e o envio avançam.
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  // Acompanha consulta, id do quiz em envio, mensagem de falha e o pedido de nova tentativa.
  const [carregando, setCarregando] = useState(true);
  const [enviando, setEnviando] = useState<Quiz['id'] | null>(null);
  const [erro, setErro] = useState('');
  const [tentativa, setTentativa] = useState(0);

  // useEffect busca os quizzes ao abrir a tela ou mudar professor, acesso ou tentativa de atualização.
  useEffect(() => {
    let ativa = true;
    setCarregando(true);
    setErro('');
    // Substitui a lista pelos quizzes salvos que o servidor permite acessar com esta sessão.
    api
      .listarQuizzes(professor.id, acesso)
      .then((lista) => {
        if (ativa) setQuizzes(lista || []);
      })
      .catch((e: unknown) => {
        // Mostra a falha da consulta somente se esta ainda for a tela ativa.
        if (ativa) setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
      })
      .finally(() => {
        // Encerra o carregamento desta consulta sem interferir na próxima tela.
        if (ativa) setCarregando(false);
      });
    // Ignora a resposta pendente se a tela sair ou outra consulta começar.
    return () => {
      ativa = false;
    };
  }, [professor.id, acesso.email, acesso.senha, tentativa]);

  // Envia o id do quiz escolhido para a sala recebida e impede dois envios ao mesmo tempo.
  async function enviar(quiz: Quiz) {
    if (enviando !== null) return;
    setErro('');
    setEnviando(quiz.id);
    try {
      await api.enviarQuiz(sala.id, quiz.id, acesso);
      // O App segue para o quiz somente depois de o Spring confirmar o envio.
      onEnviado();
    } catch (e: unknown) {
      // Mantém a lista disponível e mostra a falha do envio.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Retira o id em envio para liberar uma nova escolha.
      setEnviando(null);
    }
  }

  return (
    <Pagina rolar={false}>
      <Cabecalho titulo="Escolher quiz" subtitulo={sala.nomeAula} onVoltar={onVoltar} />
      {/* A FlatList faz a rolagem e organiza cabeçalho, lista vazia e cartões dos quizzes salvos. */}
      <FlatList
        data={quizzes}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.conteudo}
        ListHeaderComponent={
          <>
            {/* Oferece a criação de um quiz e mostra erro, nova tentativa ou consulta em andamento. */}
            <Text style={estilos.titulo}>Um novo desafio</Text>
            <Text style={[estilos.texto, estilos.espaco]}>
              Escolha um quiz salvo para começar nesta sala.
            </Text>
            <Botao
              titulo="Criar quiz"
              icone="add-circle-outline"
              cor="laranja"
              onPress={onCriarQuiz}
              desabilitado={enviando !== null}
              style={estilos.espaco}
            />
            <Mensagem texto={erro} />
            {erro ? (
              <Botao
                titulo="Atualizar lista"
                cor="claro"
                pequeno
                desabilitado={carregando || enviando !== null}
                onPress={() => setTentativa(tentativa + 1)}
                style={estilos.espaco}
              />
            ) : null}
            {carregando ? (
              <ActivityIndicator color={cores.azul} accessibilityLabel="Carregando quizzes" />
            ) : null}
          </>
        }
        ListEmptyComponent={
          !carregando && !erro ? (
            <Cartao>
              {/* Orienta a criar o primeiro quiz quando a consulta terminou sem resultados. */}
              <Ionicons name="planet-outline" size={44} color={cores.roxo} />
              <Text style={estilos.subtitulo}>Seu primeiro desafio começa aqui</Text>
              <Text style={estilos.texto}>
                Você ainda não tem quizzes salvos. Use “Criar quiz” para preparar suas perguntas.
              </Text>
            </Cartao>
          ) : null
        }
        renderItem={({ item }) => (
          <Cartao>
            {/* Resume título e quantidade de perguntas antes de enviar o quiz escolhido. */}
            <View style={styles.linha}>
              <View style={styles.icone}>
                <Ionicons name="help-circle-outline" size={28} color={cores.roxoEscuro} />
              </View>
              <View style={styles.flex}>
                <Text style={estilos.subtitulo}>{item.titulo}</Text>
                <Text style={estilos.legenda}>{(item.perguntas || []).length} pergunta(s)</Text>
              </View>
            </View>
            <Botao
              titulo="Enviar para sala"
              cor="verde"
              icone="play-outline"
              onPress={() => enviar(item)}
              carregando={enviando === item.id}
              desabilitado={enviando !== null}
            />
          </Cartao>
        )}
      />
    </Pagina>
  );
}

// StyleSheet.create reúne a aparência desta tela: alinha os dados de cada quiz e destaca seu ícone em roxo.
const styles = StyleSheet.create({
  conteudo: { padding: 24, paddingBottom: 36, flexGrow: 1 },
  linha: { flexDirection: 'row', gap: 12, alignItems: 'center', marginBottom: 20 },
  flex: { flex: 1 },
  icone: { padding: 14, borderRadius: 20, backgroundColor: cores.roxoClaro },
});
