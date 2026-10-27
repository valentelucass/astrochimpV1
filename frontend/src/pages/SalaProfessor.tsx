import { useEffect, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  Modal,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import api from '../services/api';
import { formatarTempo, formatarData, iniciais } from '../services/formatacao';
import cores from '../styles/cores';
import estilos from '../styles/estilos';
import type {
  AcessoProfessor,
  Cronometro,
  Duvida,
  PerguntaAluno,
  Professor,
  ResultadoQuiz,
  Sala,
} from '../types/modelos';

// O App entrega a sala, o professor e seu login. As funções on... abrem outras telas ou saem desta aula.
type Props = {
  sala: Sala;
  professor: Professor;
  acesso: AcessoProfessor;
  onMateriais: () => void;
  onQuizzes: () => void;
  onVoltar: () => void;
  onEncerrar: () => void;
};

// É a área de controle da aula: o professor acompanha dúvidas e respostas e controla quiz e cronômetro.
export default function SalaProfessor({
  sala,
  professor,
  acesso,
  onMateriais,
  onQuizzes,
  onVoltar,
  onEncerrar,
}: Props) {
  // useState guarda o valor atual e fornece uma função para mudá-lo e atualizar a tela.
  // dadosSala começa com a sala escolhida; duvidas recebe as perguntas que chegam dos alunos.
  const [dadosSala, setDadosSala] = useState(sala);
  const [duvidas, setDuvidas] = useState<Duvida[]>([]);
  // O cronômetro mede uma atividade da turma; pergunta e resultado pertencem ao quiz, com seu próprio tempo.
  const [cronometro, setCronometro] = useState<Cronometro | null>(null);
  const [pergunta, setPergunta] = useState<PerguntaAluno | null>(null);
  const [resultado, setResultado] = useState<ResultadoQuiz | null>(null);
  // O filtro escolhe a lista; carregando cuida da primeira busca e ocupado bloqueia ações durante um pedido.
  const [filtro, setFiltro] = useState('ativas');
  const [carregando, setCarregando] = useState(true);
  const [ocupado, setOcupado] = useState(false);
  // O erro da consulta fica separado do erro de um botão, para uma atualização não apagar a recusa de uma ação.
  const [erro, setErro] = useState('');
  const [erroConsulta, setErroConsulta] = useState('');
  const [sucesso, setSucesso] = useState('');
  // modal escolhe qual janela abrir; null fecha ambas. minutos guarda o texto digitado até sua validação.
  const [modal, setModal] = useState<'cronometro' | 'encerrar' | null>(null);
  const [minutos, setMinutos] = useState('15');
  const [erroModal, setErroModal] = useState('');
  // A aula também termina ao atingir expiraEm, mesmo sem o professor apertar o botão de encerrar.
  const encerrada =
    dadosSala.status.toLowerCase() === 'encerrada' ||
    new Date(dadosSala.expiraEm).getTime() <= Date.now();

  // useEffect acompanha a sala e o login atuais; o intervalo recebe dados novos sem recarregar a página inteira.
  useEffect(() => {
    // Espera a ação do professor terminar antes de buscar a lista novamente.
    if (ocupado) return;
    // ativo evita atualizar uma tela que já saiu; consultando impede duas buscas de dados ao mesmo tempo.
    let ativo = true;
    let consultando = false;
    // Lê primeiro o estado e as dúvidas; só uma aula aberta continua consultando os controles da atividade.
    async function atualizar() {
      if (consultando) return;
      consultando = true;
      try {
        const novaSala = await api.buscarSala(sala.id, acesso);
        const lista = await api.listarDuvidas(sala.id, acesso, true);
        if (!ativo) return;
        setDadosSala(novaSala);
        setDuvidas(lista);
        // A sala encerrada mantém o histórico, mas não recebe mais cronômetro ou quiz.
        const terminou =
          novaSala.status.toLowerCase() === 'encerrada' ||
          new Date(novaSala.expiraEm).getTime() <= Date.now();
        if (terminou) {
          setCronometro(null);
          setPergunta(null);
          setResultado(null);
        } else {
          // Promise.all espera estas duas consultas independentes; o resultado do quiz usa a pergunta obtida.
          const [tempo, atual] = await Promise.all([
            api.buscarCronometro(sala.id, acesso),
            api.buscarPerguntaAtual(sala.id, acesso),
          ]);
          const votos = atual ? await api.buscarResultadoQuiz(sala.id, acesso) : null;
          if (!ativo) return;
          setCronometro(tempo);
          setPergunta(atual);
          // Só junta contagens e pergunta com o mesmo id, para não mostrar respostas da pergunta anterior.
          setResultado(votos?.perguntaId === atual?.perguntaId ? votos : null);
        }
        setErroConsulta('');
      } catch (falha) {
        // A falha vira um aviso da consulta; o conteúdo que já chegou continua disponível na tela.
        if (ativo)
          setErroConsulta(
            falha instanceof Error ? falha.message : 'Não foi possível atualizar a sala.',
          );
      } finally {
        consultando = false;
        if (ativo) setCarregando(false);
      }
    }
    atualizar();
    // Busca dúvidas e respostas novas a cada três segundos. A limpeza para as consultas ao sair desta tela.
    const intervalo = setInterval(atualizar, 3000);
    return () => {
      ativo = false;
      clearInterval(intervalo);
    };
  }, [sala.id, acesso.email, acesso.senha, ocupado]);

  // Copia o código de seis caracteres que os alunos precisam digitar na entrada da sala.
  async function copiarCodigo() {
    setErro('');
    setSucesso('');
    try {
      const copiou = await Clipboard.setStringAsync(dadosSala.codigo);
      if (!copiou) throw new Error('Falha ao copiar.');
      setSucesso('Código da sala copiado.');
    } catch {
      setErro('Não foi possível copiar o código.');
    }
  }

  // Os três botões de um cartão usam o id da dúvida para pedir a mudança escolhida ao backend.
  async function alterarDuvida(duvida: Duvida, acao: 'fixar' | 'responder' | 'arquivar') {
    if (ocupado || encerrada) return;
    // Enquanto a API salva, bloqueamos outra ação. O finally libera os controles mesmo se houver erro.
    setOcupado(true);
    setErro('');
    setSucesso('');
    try {
      let atualizada;
      if (acao === 'fixar') atualizada = await api.fixarDuvida(duvida.id, !duvida.fixada, acesso);
      else {
        let estado = 'arquivada';
        if (acao === 'responder')
          estado = duvida.estado === 'respondida' ? 'pendente' : 'respondida';
        atualizada = await api.alterarEstadoDuvida(duvida.id, estado, acesso);
      }
      // map percorre a lista e troca somente a dúvida que veio atualizada, preservando as outras.
      setDuvidas((lista) => lista.map((item) => (item.id === atualizada.id ? atualizada : item)));
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : 'Não foi possível alterar a dúvida.');
    } finally {
      setOcupado(false);
    }
  }

  // false avança para a próxima pergunta; true interrompe o quiz sem fechar a aula.
  async function controlarQuiz(encerrar: boolean) {
    if (ocupado) return;
    setOcupado(true);
    setErro('');
    setSucesso('');
    try {
      if (encerrar) await api.encerrarQuiz(sala.id, acesso);
      else await api.proximaPergunta(sala.id, acesso);
      const atual = await api.buscarPerguntaAtual(sala.id, acesso);
      // Espera pergunta e contagens ficarem prontas antes de trocar o painel apresentado.
      const novosResultados = atual ? await api.buscarResultadoQuiz(sala.id, acesso) : null;
      setPergunta(atual);
      setResultado(novosResultados?.perguntaId === atual?.perguntaId ? novosResultados : null);
      setSucesso(atual ? 'Próxima pergunta enviada aos alunos.' : 'Quiz encerrado.');
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : 'Não foi possível controlar o quiz.');
    } finally {
      setOcupado(false);
    }
  }

  // O formulário recebe minutos como texto; Number converte e a validação aceita só minutos inteiros positivos.
  async function iniciarCronometro() {
    const duracao = Number(minutos);
    if (!Number.isInteger(duracao) || duracao <= 0 || duracao > 1440) {
      setErroModal('Informe de 1 a 1440 minutos.');
      return;
    }
    if (ocupado) return;
    setOcupado(true);
    setErroModal('');
    try {
      // A API recebe segundos, então cinco minutos, por exemplo, são enviados como 300.
      setCronometro(await api.iniciarCronometro(sala.id, duracao * 60, acesso));
      setModal(null);
      setSucesso('Cronômetro iniciado para toda a sala.');
    } catch (falha) {
      setErroModal(
        falha instanceof Error ? falha.message : 'Não foi possível iniciar o cronômetro.',
      );
    } finally {
      setOcupado(false);
    }
  }

  // A parada recebida do servidor retira a contagem para todos os participantes, não só desta tela.
  async function pararCronometro() {
    if (ocupado) return;
    setOcupado(true);
    setErroModal('');
    setSucesso('');
    try {
      setCronometro(await api.pararCronometro(sala.id, acesso));
      setModal(null);
      setSucesso('Cronômetro parado.');
    } catch (falha) {
      setErroModal(falha instanceof Error ? falha.message : 'Não foi possível parar o cronômetro.');
    } finally {
      setOcupado(false);
    }
  }

  // Só a confirmação da janela chama esta ação; após salvar o encerramento, onEncerrar volta ao painel.
  async function encerrarSala() {
    if (ocupado) return;
    setOcupado(true);
    setErroModal('');
    try {
      await api.encerrarSala(sala.id, acesso);
      onEncerrar();
    } catch (falha) {
      setErroModal(falha instanceof Error ? falha.message : 'Não foi possível encerrar a sala.');
    } finally {
      setOcupado(false);
    }
  }

  // FlatList chama esta função para cada dúvida. item é o registro que será apresentado nesse cartão.
  function cartaoDuvida({ item }: { item: Duvida }) {
    return (
      <Cartao
        corDaBorda={item.fixada ? cores.roxo : cores.borda}
        style={item.fixada && styles.fixada}
      >
        {/* O professor vê o autor mesmo quando a turma recebe a pergunta como anônima. */}
        <View style={styles.linha}>
          <Text style={styles.avatar}>{iniciais(item.autorNome)}</Text>
          <View style={styles.flex}>
            <Text style={estilos.subtitulo}>{item.autorNome || 'Aluno'}</Text>
            <Text style={estilos.legenda}>
              {formatarData(item.criadaEm)}
              {item.anonima ? ' • anônimo para a turma' : ''}
            </Text>
          </View>
          <Text style={styles.votos}>★ {item.votos}</Text>
        </View>
        {item.fixada ? <Text style={[estilos.rotulo, styles.roxo]}>FIXADA NO TOPO</Text> : null}
        <Text style={[estilos.texto, styles.espaco]}>{item.texto}</Text>
        <Text style={estilos.legenda}>{item.estado}</Text>
        {/* O estado atual decide entre fixar/desafixar e responder/reabrir; arquivar preserva o histórico. */}
        <View style={styles.acoes}>
          <Botao
            pequeno
            titulo={item.fixada ? 'Desafixar' : 'Fixar'}
            cor="claro"
            icone="pin-outline"
            onPress={() => alterarDuvida(item, 'fixar')}
            desabilitado={ocupado || encerrada}
          />
          <Botao
            pequeno
            titulo={item.estado === 'respondida' ? 'Reabrir' : 'Respondida'}
            cor={item.estado === 'respondida' ? 'claro' : 'verde'}
            icone="checkmark"
            onPress={() => alterarDuvida(item, 'responder')}
            desabilitado={ocupado || encerrada}
          />
          {item.estado !== 'arquivada' ? (
            <TouchableOpacity
              accessibilityRole="button"
              accessibilityLabel="Arquivar dúvida"
              onPress={() => alterarDuvida(item, 'arquivar')}
              disabled={ocupado || encerrada}
              style={styles.arquivar}
            >
              <Ionicons name="archive-outline" size={21} color={cores.textoSuave} />
            </TouchableOpacity>
          ) : null}
        </View>
      </Cartao>
    );
  }

  // filter cria uma lista com as pendentes/respondidas ou com as arquivadas, conforme a aba escolhida.
  const visiveis = duvidas.filter((item) =>
    filtro === 'arquivadas' ? item.estado === 'arquivada' : item.estado !== 'arquivada',
  );
  // A lista filtrada põe fixadas primeiro e depois as mais votadas; a lista duvidas não é reordenada aqui.
  visiveis.sort((a, b) => Number(b.fixada) - Number(a.fixada) || b.votos - a.votos);

  // Pagina deixa a rolagem por conta da FlatList, que apresenta um cartão por dúvida e um topo com os controles.
  return (
    <Pagina rolar={false}>
      <Cabecalho
        titulo={dadosSala.nomeAula}
        subtitulo="SALA DE CONTROLE"
        onVoltar={onVoltar}
        onAcao={() => {
          setErroModal('');
          setModal('encerrar');
        }}
        acaoIcone="power-outline"
        acaoRotulo="Encerrar sala"
      />
      {/* O id vira a chave de cada cartão, para o React reconhecer o mesmo item quando chegam atualizações. */}
      <FlatList
        data={visiveis}
        keyExtractor={(item) => String(item.id)}
        renderItem={cartaoDuvida}
        contentContainerStyle={styles.conteudo}
        ListHeaderComponent={
          <>
            {/* Este topo reúne código de entrada, quantidade de participantes e perguntas ainda pendentes. */}
            <View style={[styles.linha, styles.espaco]}>
              <View style={styles.flex}>
                <Text style={estilos.legenda}>Professor {professor.nome}</Text>
                <Text style={estilos.titulo}>{dadosSala.codigo}</Text>
              </View>
              <Botao
                titulo="COPIAR"
                pequeno
                cor="claro"
                icone="copy-outline"
                onPress={copiarCodigo}
              />
            </View>
            <View style={[styles.linha, styles.espaco]}>
              <Text style={styles.contagem}>● {dadosSala.totalAlunos} participantes</Text>
              <Text style={estilos.legenda}>
                {duvidas.filter((item) => item.estado === 'pendente').length} dúvidas pendentes
              </Text>
            </View>
            {encerrada ? (
              <Mensagem texto="Esta sala foi encerrada ou chegou ao fim das quatro horas de duração." />
            ) : null}
            <Mensagem texto={erroConsulta} />
            <Mensagem texto={erro} />
            <Mensagem texto={sucesso} tipo="sucesso" />
            {/* O painel do quiz aparece só enquanto há pergunta e a aula continua aberta. */}
            {pergunta && !encerrada ? (
              <Cartao corDaBorda={cores.roxo}>
                <Text style={[estilos.rotulo, styles.roxo]}>
                  QUIZ EM ANDAMENTO • {pergunta.numeroPergunta}/{pergunta.totalPerguntas}
                </Text>
                <Text style={[estilos.subtitulo, styles.espaco]}>{pergunta.enunciado}</Text>
                <Text style={estilos.legenda}>
                  {formatarTempo(pergunta.tempoRestanteSegundos)} para responder •{' '}
                  {resultado?.totalRespondentes || 0} respostas
                </Text>
                {/* Cada linha mostra uma alternativa e quantos alunos a escolheram nesta pergunta e rodada. */}
                {resultado?.resultados.map((item) => (
                  <View key={item.letra} style={styles.resultado}>
                    <Text style={[estilos.texto, styles.flex]}>
                      {item.letra}. {item.texto}
                    </Text>
                    <Text style={estilos.subtitulo}>{item.totalRespostas}</Text>
                  </View>
                ))}
                <View style={styles.acoes}>
                  <Botao
                    titulo={
                      pergunta.numeroPergunta === pergunta.totalPerguntas ? 'Concluir' : 'Próxima'
                    }
                    pequeno
                    cor="roxo"
                    onPress={() => controlarQuiz(false)}
                    desabilitado={ocupado}
                  />
                  <Botao
                    titulo="Encerrar quiz"
                    pequeno
                    cor="claro"
                    onPress={() => controlarQuiz(true)}
                    desabilitado={ocupado}
                  />
                </View>
              </Cartao>
            ) : null}
            {/* Os filtros mudam apenas quais dúvidas são exibidas; não apagam registros do servidor. */}
            <Text style={[estilos.titulo, styles.espaco]}>✨ Dúvidas da turma</Text>
            <View style={[styles.acoes, styles.espaco]}>
              <Botao
                titulo="Ativas"
                pequeno
                cor={filtro === 'ativas' ? 'azul' : 'claro'}
                onPress={() => setFiltro('ativas')}
              />
              <Botao
                titulo="Arquivadas"
                pequeno
                cor={filtro === 'arquivadas' ? 'azul' : 'claro'}
                onPress={() => setFiltro('arquivadas')}
              />
            </View>
          </>
        }
        ListEmptyComponent={
          <Text style={estilos.vazio}>
            {carregando
              ? 'Buscando as dúvidas…'
              : encerrada
                ? 'Não há dúvidas nesta lista. Consulte também as arquivadas.'
                : 'Nenhuma dúvida nesta lista. Compartilhe o código com a turma!'}
          </Text>
        }
      />
      {/* O rodapé fica fora da lista para as ações principais continuarem ao alcance durante a rolagem. */}
      <View style={styles.rodape}>
        <View style={styles.linha}>
          <Botao
            titulo="COMEÇAR QUIZ"
            pequeno
            icone="rocket-outline"
            onPress={onQuizzes}
            desabilitado={encerrada || !!pergunta}
            style={styles.flex}
          />
          <Botao
            titulo="MATERIAL"
            pequeno
            cor="azul"
            icone="folder-outline"
            onPress={onMateriais}
          />
        </View>
        <Botao
          titulo={
            cronometro?.ativo
              ? 'CRONÔMETRO: ' + formatarTempo(cronometro.segundosRestantes)
              : 'INICIAR CRONÔMETRO'
          }
          pequeno
          cor="roxo"
          icone="timer-outline"
          onPress={() => {
            setErroModal('');
            setModal('cronometro');
          }}
          desabilitado={encerrada}
        />
      </View>
      {/* Modal sobrepõe uma janela: configurar minutos ou confirmar o fim da aula. */}
      <Modal
        visible={modal !== null}
        transparent
        animationType="fade"
        onRequestClose={() => {
          if (!ocupado) setModal(null);
        }}
      >
        <View style={styles.sobreposicao}>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={styles.modalRolagem}
          >
            <Cartao style={styles.modal}>
              {/* A janela de cronômetro permite iniciar outra contagem ou parar a que está ativa. */}
              {modal === 'cronometro' ? (
                <>
                  <Text style={[estilos.titulo, styles.espaco]}>Tempo para a atividade</Text>
                  <Text style={[estilos.texto, styles.espaco]}>
                    A turma verá o mesmo cronômetro na sala.
                  </Text>
                  <Campo
                    rotulo="DURAÇÃO EM MINUTOS"
                    valor={minutos}
                    onChangeText={setMinutos}
                    keyboardType="number-pad"
                    maxLength={4}
                    editable={!ocupado}
                  />
                  <Mensagem texto={erroModal} />
                  <Botao
                    titulo="INICIAR"
                    cor="roxo"
                    onPress={iniciarCronometro}
                    carregando={ocupado}
                  />
                  {cronometro?.ativo ? (
                    <Botao
                      titulo="PARAR CRONÔMETRO"
                      cor="claro"
                      onPress={pararCronometro}
                      desabilitado={ocupado}
                      style={styles.espacoSuperior}
                    />
                  ) : null}
                </>
              ) : modal === 'encerrar' ? (
                <>
                  <Text style={[estilos.titulo, styles.espaco]}>Encerrar esta aula?</Text>
                  <Text style={[estilos.texto, styles.espaco]}>
                    Os alunos não poderão enviar novas dúvidas ou respostas nesta sala. O histórico
                    continua no seu painel.
                  </Text>
                  <Mensagem texto={erroModal} />
                  <Botao
                    titulo="ENCERRAR SALA"
                    cor="laranja"
                    onPress={encerrarSala}
                    carregando={ocupado}
                  />
                </>
              ) : null}
              <Botao
                titulo="VOLTAR"
                cor="claro"
                onPress={() => setModal(null)}
                desabilitado={ocupado}
                style={styles.espacoSuperior}
              />
            </Cartao>
          </ScrollView>
        </View>
      </Modal>
    </Pagina>
  );
}

// StyleSheet.create guarda medidas e cores da sala de controle; os estilos comuns vêm de estilos.ts.
const styles = StyleSheet.create({
  conteudo: { padding: 22, paddingBottom: 10 },
  linha: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10 },
  flex: { flex: 1 },
  espaco: { marginBottom: 17 },
  espacoSuperior: { marginTop: 12 },
  contagem: { fontFamily: 'Nunito_700Bold', fontSize: 13, color: cores.verdeEscuro },
  // As iniciais identificam o autor do cartão; votos e fixada destacam a prioridade da pergunta.
  avatar: {
    width: 42,
    height: 42,
    borderRadius: 17,
    backgroundColor: cores.azulClaro,
    color: cores.azulEscuro,
    textAlign: 'center',
    lineHeight: 42,
    fontFamily: 'Nunito_800ExtraBold',
  },
  votos: { color: cores.laranja, fontFamily: 'Nunito_800ExtraBold', fontSize: 17 },
  fixada: { backgroundColor: cores.roxoClaro },
  roxo: { color: cores.roxoEscuro, marginVertical: 9 },
  acoes: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 12, alignItems: 'center' },
  arquivar: { padding: 10 },
  resultado: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 9,
    gap: 12,
    borderBottomWidth: 1,
    borderColor: cores.borda,
  },
  // A borda separa os comandos fixos da lista de perguntas que rola acima deles.
  rodape: {
    padding: 17,
    gap: 12,
    backgroundColor: cores.branco,
    borderTopWidth: 2,
    borderColor: cores.borda,
  },
  // O fundo escurecido destaca a janela, cujo conteúdo pode rolar quando o teclado estiver aberto.
  sobreposicao: { flex: 1, backgroundColor: '#07334588' },
  modalRolagem: { flexGrow: 1, justifyContent: 'center', alignItems: 'center', padding: 22 },
  modal: { width: '100%', maxWidth: 440, marginBottom: 0 },
});
