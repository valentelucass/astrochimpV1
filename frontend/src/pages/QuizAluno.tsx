import { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import api, { ErroApi } from '../services/api';
import { formatarTempo } from '../services/formatacao';
import cores from '../styles/cores';
import estilos from '../styles/estilos';
import type { Sala, Aluno, PerguntaAluno } from '../types/modelos';

// Recebe a sala, o aluno com seu token e o retorno à sala controlado pelo App.
type Props = {
  sala: Sala;
  aluno: Aluno;
  onVoltar: () => void;
};

// Exibe a pergunta atual do professor e envia a alternativa escolhida sem mostrar o gabarito.
export default function QuizAluno({ sala, aluno, onVoltar }: Props) {
  // useState mantém o que muda na tela; aqui guarda a pergunta recebida e o id escolhido para enviar.
  const [pergunta, setPergunta] = useState<PerguntaAluno | null>(null);
  const [escolha, setEscolha] = useState<number | null>(null);
  // As chaves ligam confirmação e bloqueio à rodada e pergunta certas, mesmo se o professor avançar.
  const [chaveRespondida, setChaveRespondida] = useState('');
  const [chaveBloqueada, setChaveBloqueada] = useState('');
  // Controla a contagem visível, a espera dos pedidos e os avisos de resposta ou consulta.
  const [tempo, setTempo] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [erroConsulta, setErroConsulta] = useState('');
  // Distingue o fim da aula e calcula se os registros locais pertencem à pergunta exibida.
  const [salaEncerrada, setSalaEncerrada] = useState(false);
  const chavePergunta = pergunta ? pergunta.rodadaId + ':' + pergunta.perguntaId : '';
  const respondida = chavePergunta !== '' && chaveRespondida === chavePergunta;
  const bloqueada = chavePergunta !== '' && chaveBloqueada === chavePergunta;

  // useEffect acompanha esta sala enquanto a tela está aberta e encerra os intervalos ao sair.
  useEffect(() => {
    let ativo = true;
    let consultando = false;
    let chaveAtual = '';
    // Busca a pergunta que o professor liberou para esta participação na sala.
    async function atualizar() {
      // Aguarda a consulta anterior para não empilhar pedidos quando a conexão fica lenta.
      if (consultando) return;
      consultando = true;
      try {
        // Busca só a pergunta liberada ao aluno e ignora a resposta se a tela já saiu.
        const atual = await api.buscarPerguntaAtual(sala.id, { tokenAluno: aluno.tokenAluno });
        if (!ativo) return;
        const chave = atual ? atual.rodadaId + ':' + atual.perguntaId : '';
        // Reconsultar a mesma pergunta preserva a escolha; outra rodada ou pergunta começa limpa.
        if (chave !== chaveAtual) {
          chaveAtual = chave;
          setEscolha(null);
          setChaveRespondida('');
          setChaveBloqueada('');
          setErro('');
        }
        // Atualiza a pergunta e corrige o relógio local com os segundos calculados pelo servidor.
        setPergunta(atual);
        setTempo(atual ? atual.tempoRestanteSegundos : 0);
        setErroConsulta('');
        setSalaEncerrada(false);
      } catch (falha) {
        if (!ativo) return;
        if (falha instanceof ErroApi && falha.status === 409) {
          // Neste endpoint, o conflito informa que a sala terminou e retira a pergunta antiga.
          setSalaEncerrada(true);
          setPergunta(null);
          setEscolha(null);
          setTempo(0);
          setErroConsulta('');
        } else
          // Outras falhas ficam visíveis sem apagar a pergunta que o aluno já estava lendo.
          setErroConsulta(
            falha instanceof Error ? falha.message : 'Não foi possível consultar o quiz.',
          );
      } finally {
        // Libera a próxima consulta e encerra a espera inicial somente se a tela continua aberta.
        consultando = false;
        if (ativo) setCarregando(false);
      }
    }
    // Consulta a cada três segundos e reduz a contagem entre consultas, sem passar de zero.
    atualizar();
    const consulta = setInterval(atualizar, 3000);
    const relogio = setInterval(() => setTempo((valor) => Math.max(0, valor - 1)), 1000);
    // A página saiu da tela: os relógios param e respostas atrasadas são ignoradas.
    return () => {
      ativo = false;
      clearInterval(consulta);
      clearInterval(relogio);
    };
  }, [sala.id, aluno.tokenAluno]);

  // Envia o id da alternativa selecionada e registra a confirmação para esta pergunta.
  async function confirmar() {
    // Exige escolha, tempo disponível e uma pergunta que ainda aceite tentativa nesta tela.
    if (escolha === null || enviando || respondida || bloqueada || tempo <= 0) return;
    // Guarda a identidade antes do pedido porque a consulta periódica pode trazer outra pergunta.
    const chaveEnviada = chavePergunta;
    setEnviando(true);
    setErro('');
    try {
      await api.responderQuiz(sala.id, escolha, { tokenAluno: aluno.tokenAluno });
      setChaveRespondida(chaveEnviada);
    } catch (falha) {
      // Resposta repetida ou fora do prazo bloqueia outra tentativa desta pergunta e explica o motivo.
      const conflito = falha instanceof ErroApi && falha.status === 409;
      if (conflito) setChaveBloqueada(chaveEnviada);
      setErro(
        conflito
          ? 'A resposta não pôde ser registrada: você já respondeu ou o tempo desta pergunta terminou. Aguarde a próxima.'
          : falha instanceof Error
            ? falha.message
            : 'Não foi possível enviar sua resposta.',
      );
    } finally {
      // Finaliza a espera do envio, mantendo confirmação ou conflito associados à sua chave.
      setEnviando(false);
    }
  }

  // A largura da barra usa a posição real da pergunta e nunca ultrapassa cem por cento.
  const progresso =
    pergunta && pergunta.totalPerguntas
      ? Math.min(100, (pergunta.numeroPergunta / pergunta.totalPerguntas) * 100)
      : 0;
  return (
    <Pagina>
      {/* O retorno fica disponível e a barra mostra o avanço da rodada com os dados recebidos. */}
      <Cabecalho titulo="Missão quiz" onVoltar={onVoltar} />
      <View style={styles.linha}>
        <View style={styles.trilho}>
          <View style={[styles.progresso, { width: `${progresso}%` }]} />
        </View>
        <Ionicons name="trophy-outline" size={27} color={cores.laranja} />
      </View>
      {/* Separa falhas de envio e consulta da confirmação de uma resposta registrada. */}
      <Mensagem texto={erro} />
      <Mensagem texto={erroConsulta} />
      <Mensagem
        texto={respondida ? 'Resposta enviada! Aguarde a próxima pergunta.' : ''}
        tipo="sucesso"
      />
      {/* Sem pergunta, explica a espera ou o encerramento e oferece o caminho de volta. */}
      {!pergunta ? (
        <Cartao>
          <Text style={estilos.titulo}>
            {carregando
              ? 'Buscando a missão…'
              : salaEncerrada
                ? 'Esta sala foi encerrada'
                : 'Aguarde a próxima missão'}
          </Text>
          <Text style={estilos.texto}>
            {salaEncerrada
              ? 'A aula terminou. Você pode voltar à entrada para participar de outra sala.'
              : 'O quiz pode ter terminado ou o professor ainda vai iniciar uma rodada.'}
          </Text>
          <Botao titulo="VOLTAR À SALA" cor="claro" onPress={onVoltar} style={styles.espaco} />
        </Cartao>
      ) : (
        <>
          {/* O relógio, a dificuldade e a posição contextualizam o enunciado atual. */}
          <Text style={styles.tempo}>◷ {formatarTempo(tempo)}</Text>
          <Cartao style={styles.pergunta}>
            <View style={styles.linha}>
              <Text style={styles.dificuldade}>☆ {pergunta.dificuldade}</Text>
              <Text style={estilos.legenda}>
                PERGUNTA {pergunta.numeroPergunta}/{pergunta.totalPerguntas}
              </Text>
            </View>
            <Text style={[estilos.titulo, styles.enunciado]}>{pergunta.enunciado}</Text>
          </Cartao>
          {/* map transforma as alternativas em opções selecionáveis, guardando o id de cada escolha. */}
          {(pergunta.alternativas || []).map((alternativa, indice) => (
            <TouchableOpacity
              key={alternativa.id}
              accessibilityRole="radio"
              aria-checked={escolha === alternativa.id}
              accessibilityState={{
                checked: escolha === alternativa.id,
                disabled: respondida || bloqueada || enviando || tempo <= 0,
              }}
              disabled={respondida || bloqueada || enviando || tempo <= 0}
              onPress={() => setEscolha(alternativa.id)}
              style={[styles.alternativa, escolha === alternativa.id && styles.selecionada]}
            >
              <Text style={styles.letra}>{alternativa.letra || 'ABCDEF'[indice]}</Text>
              <Text style={[estilos.texto, styles.resposta]}>{alternativa.texto}</Text>
            </TouchableOpacity>
          ))}
          {/* O fim do prazo é explicado, e confirmar respeita escolha, envio e bloqueio da pergunta. */}
          {!respondida && tempo <= 0 ? (
            <Text style={estilos.vazio}>Tempo esgotado. Aguarde o professor avançar.</Text>
          ) : null}
          <Botao
            titulo={respondida ? 'RESPOSTA ENVIADA' : 'CONFIRMAR RESPOSTA'}
            cor="laranja"
            icone="checkmark-circle-outline"
            onPress={confirmar}
            carregando={enviando}
            desabilitado={escolha === null || respondida || bloqueada || tempo <= 0}
          />
        </>
      )}
    </Pagina>
  );
}

// StyleSheet concentra a aparência do quiz: progresso laranja, tempo em destaque e borda da escolha.
const styles = StyleSheet.create({
  linha: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    marginBottom: 20,
  },
  trilho: {
    flex: 1,
    height: 16,
    borderRadius: 12,
    backgroundColor: cores.cinza,
    overflow: 'hidden',
    marginTop: 20,
  },
  progresso: {
    height: '100%',
    backgroundColor: cores.laranja,
    borderRadius: 12,
    borderBottomWidth: 4,
    borderBottomColor: cores.laranjaEscuro,
  },
  tempo: {
    alignSelf: 'center',
    fontFamily: 'Nunito_800ExtraBold',
    fontSize: 29,
    color: cores.azulEscuro,
    backgroundColor: cores.azulClaro,
    padding: 14,
    borderBottomWidth: 4,
    borderColor: cores.borda,
    marginBottom: 28,
  },
  pergunta: { minHeight: 220, borderRadius: 34, paddingTop: 25 },
  dificuldade: {
    fontFamily: 'Nunito_700Bold',
    color: cores.azulEscuro,
    backgroundColor: cores.azulClaro,
    padding: 7,
    borderRadius: 12,
  },
  enunciado: { marginTop: 10 },
  alternativa: {
    backgroundColor: cores.branco,
    borderWidth: 2,
    borderBottomWidth: 6,
    borderColor: cores.cinza,
    borderRadius: 25,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    marginBottom: 16,
  },
  selecionada: { borderColor: cores.laranja, backgroundColor: cores.laranjaClaro },
  letra: {
    fontFamily: 'Nunito_800ExtraBold',
    fontSize: 23,
    color: cores.azulEscuro,
    backgroundColor: cores.azulClaro,
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 9,
  },
  resposta: { flex: 1 },
  espaco: { marginTop: 20 },
});
