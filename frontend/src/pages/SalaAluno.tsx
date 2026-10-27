import { useState, useEffect } from 'react';
import { View, Text, FlatList, Modal, Switch, StyleSheet, ScrollView } from 'react-native';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Botao from '../components/Botao';
import Campo from '../components/Campo';
import Mensagem from '../components/Mensagem';
import api, { ErroApi } from '../services/api';
import { abrirMaterial } from '../services/arquivos';
import { formatarTempo, formatarData, iniciais } from '../services/formatacao';
import cores from '../styles/cores';
import estilos from '../styles/estilos';
import type { Sala, Aluno, Duvida, Material, Cronometro, PerguntaAluno } from '../types/modelos';

// Recebe a sala, o aluno com seu token e os caminhos de quiz, materiais e saída definidos pelo App.
type Props = {
  sala: Sala;
  aluno: Aluno;
  onQuiz: () => void;
  onMateriais: () => void;
  onSair: () => void;
};

// Mostra a aula em andamento e permite que o aluno acompanhe materiais e participe das dúvidas.
export default function SalaAluno({ sala, aluno, onQuiz, onMateriais, onSair }: Props) {
  // useState guarda dados que mudam na tela; este grupo recebe as consultas reais da sala.
  const [dadosSala, setDadosSala] = useState<Sala>(sala);
  const [duvidas, setDuvidas] = useState<Duvida[]>([]);
  const [materiais, setMateriais] = useState<Material[]>([]);
  const [cronometro, setCronometro] = useState<Cronometro | null>(null);
  const [quiz, setQuiz] = useState<PerguntaAluno | null>(null);
  // Separa a espera inicial, os erros das consultas e os avisos das ações do aluno.
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [erroConsulta, setErroConsulta] = useState('');
  const [feedback, setFeedback] = useState('');
  // Mantém o formulário da dúvida aberto com o texto e a opção de anonimato escolhidos.
  const [modal, setModal] = useState(false);
  const [texto, setTexto] = useState('');
  const [anonima, setAnonima] = useState(false);
  const [erroEnvio, setErroEnvio] = useState('');
  const [enviando, setEnviando] = useState(false);
  // Guarda o id da ação em andamento para indicar qual voto ou material está aguardando.
  const [votando, setVotando] = useState<number | null>(null);
  const [abrindo, setAbrindo] = useState<number | null>(null);
  // O token autentica as ações; o estado e a validade da sala determinam se ela já terminou.
  const acesso = { tokenAluno: aluno.tokenAluno };
  const encerrada =
    dadosSala.status.toLowerCase() === 'encerrada' ||
    new Date(dadosSala.expiraEm).getTime() <= Date.now();

  // useEffect inicia o acompanhamento ao entrar nesta sala e faz a limpeza ao sair ou trocar o acesso.
  useEffect(() => {
    let ativo = true;
    let consultando = false;
    // Consulta a sala antes do quiz: uma sala encerrada mantém o histórico e não recebe outra rodada.
    async function atualizar() {
      // Se a consulta anterior ainda não acabou, espera o próximo intervalo para evitar sobreposição.
      if (consultando) return;
      consultando = true;
      const acessoAluno = { tokenAluno: aluno.tokenAluno };
      try {
        // Confere primeiro o estado da sala e descarta o retorno se a tela já tiver sido desmontada.
        const novaSala = await api.buscarSala(sala.id, acessoAluno);
        if (!ativo) return;
        setDadosSala(novaSala);
        // Dúvidas e materiais são independentes e podem ser buscados juntos para montar o histórico.
        const [novasDuvidas, novosMateriais] = await Promise.all([
          api.listarDuvidas(sala.id, acessoAluno),
          api.listarMateriaisSala(sala.id, acessoAluno),
        ]);
        if (!ativo) return;
        setDuvidas(novasDuvidas);
        setMateriais(novosMateriais);
        // Uma sala encerrada ou expirada mantém o histórico, mas retira o cronômetro e o convite do quiz.
        const terminou =
          novaSala.status.toLowerCase() === 'encerrada' ||
          new Date(novaSala.expiraEm).getTime() <= Date.now();
        if (terminou) {
          setCronometro(null);
          setQuiz(null);
        } else {
          // Só consulta a contagem e a pergunta disponível quando a aula ainda está ativa.
          const [tempo, perguntaAtual] = await Promise.all([
            api.buscarCronometro(sala.id, acessoAluno),
            api.buscarPerguntaAtual(sala.id, acessoAluno),
          ]);
          if (!ativo) return;
          setCronometro(tempo);
          setQuiz(perguntaAtual);
        }
        setErroConsulta('');
      } catch (falha) {
        // Preserva o conteúdo já recebido e avisa por que esta atualização não terminou.
        if (ativo)
          setErroConsulta(
            falha instanceof Error ? falha.message : 'Não foi possível atualizar a sala.',
          );
      } finally {
        // Libera a próxima consulta e encerra a indicação de carregamento inicial.
        consultando = false;
        if (ativo) setCarregando(false);
      }
    }
    // Busca logo ao abrir e repete a cada três segundos para acompanhar o professor.
    atualizar();
    const intervalo = setInterval(atualizar, 3000);
    // Uma resposta atrasada não pode alterar a sala depois de o aluno sair dela.
    return () => {
      ativo = false;
      clearInterval(intervalo);
    };
  }, [sala.id, aluno.tokenAluno]);

  // Publica a dúvida escrita no modal usando o token do aluno e a opção de anonimato.
  async function enviar() {
    // Não permite um segundo envio simultâneo nem uma pergunta composta apenas de espaços.
    if (enviando) return;
    if (!texto.trim()) {
      setErroEnvio('Escreva sua dúvida antes de enviar.');
      return;
    }
    // Mantém o modal ocupado até saber se o servidor aceitou a pergunta.
    setEnviando(true);
    setErroEnvio('');
    try {
      // Acrescenta a dúvida devolvida sem duplicar seu id, depois limpa e fecha o formulário.
      const criada = await api.criarDuvida(sala.id, texto.trim(), anonima, acesso);
      if (criada)
        setDuvidas((atuais) => [criada, ...atuais.filter((item) => item.id !== criada.id)]);
      setTexto('');
      setAnonima(false);
      setModal(false);
      setFeedback('Sua dúvida foi enviada ao professor.');
    } catch (falha) {
      // Em caso de falha, o texto fica no modal para o aluno poder tentar novamente.
      setErroEnvio(falha instanceof Error ? falha.message : 'Não foi possível enviar sua dúvida.');
    } finally {
      // Devolve o controle dos botões do formulário após sucesso ou erro.
      setEnviando(false);
    }
  }

  // Registra um voto na dúvida selecionada e busca a contagem atualizada no servidor.
  async function votar(duvida: Duvida) {
    // Interrompe a ação se outro voto ainda está sendo enviado ou a sala já terminou.
    if (votando !== null || encerrada) return;
    setVotando(duvida.id);
    setErro('');
    setFeedback('');
    try {
      await api.votarDuvida(duvida.id, acesso);
      // O aviso confirma o registro, e a nova consulta traz os totais reais do feed.
      setFeedback('Voto registrado!');
      const lista = await api.listarDuvidas(sala.id, acesso);
      setDuvidas(lista || []);
    } catch (falha) {
      const conflito = falha instanceof ErroApi && falha.status === 409;
      // O status 409 explica a tentativa repetida; as demais falhas mantêm sua mensagem própria.
      setErro(
        conflito
          ? 'Você já votou nesta dúvida. Cada aluno pode votar uma vez.'
          : falha instanceof Error
            ? falha.message
            : 'Não foi possível registrar seu voto.',
      );
    } finally {
      // Retira o indicador do cartão e permite votar em outra dúvida.
      setVotando(null);
    }
  }

  // Usa o serviço de arquivos para copiar código, abrir link ou baixar o material autenticado.
  async function abrir(material: Material) {
    // Aguarda a abertura atual antes de aceitar outro material e limpa avisos anteriores.
    if (abrindo !== null) return;
    setAbrindo(material.id);
    setErro('');
    setFeedback('');
    try {
      await abrirMaterial(material, acesso, sala.id);
      // O aviso depende da ação feita pelo serviço, sem inventar dados do material.
      setFeedback(material.tipo === 'codigo' ? 'Código copiado!' : 'Material aberto.');
    } catch (falha) {
      // Exibe o motivo se não foi possível copiar, abrir ou baixar o conteúdo.
      setErro(falha instanceof Error ? falha.message : 'Não foi possível abrir o material.');
    } finally {
      // Libera os botões de materiais ao terminar a tentativa.
      setAbrindo(null);
    }
  }

  // Monta um item da FlatList com autor permitido, estado da dúvida e botão de voto.
  function cartaoDuvida({ item }: { item: Duvida }) {
    // No modo anônimo, usa um rótulo e um símbolo em vez de mostrar o nome do autor.
    const nome = item.anonima ? 'Anônimo' : item.autorNome || 'Aluno';
    return (
      <Cartao>
        {/* Autor, data e estado ajudam a reconhecer a dúvida sem expor quem pediu anonimato. */}
        <View style={styles.linha}>
          <Text style={styles.avatar}>{item.anonima ? '?' : iniciais(nome)}</Text>
          <View style={styles.autor}>
            <Text style={estilos.subtitulo}>{nome}</Text>
            <Text style={estilos.legenda}>{formatarData(item.criadaEm)}</Text>
          </View>
          <Text style={styles.estado}>{item.estado}</Text>
        </View>
        {/* A fixação destaca a pergunta; o voto é bloqueado durante o pedido ou após o fim da aula. */}
        {item.fixada ? <Text style={estilos.rotulo}>📌 DÚVIDA FIXADA</Text> : null}
        <Text style={[estilos.texto, styles.duvida]}>{item.texto}</Text>
        <Botao
          titulo={String(item.votos) + ' votos'}
          icone="star-outline"
          cor="claro"
          pequeno
          onPress={() => votar(item)}
          carregando={votando === item.id}
          desabilitado={encerrada || votando !== null}
          style={styles.voto}
        />
      </Cartao>
    );
  }

  return (
    <Pagina rolar={false}>
      {/* O cabeçalho identifica a aula e mantém os caminhos de saída e materiais. */}
      <Cabecalho
        titulo={dadosSala.nomeAula}
        subtitulo={'CÓDIGO: ' + dadosSala.codigo}
        onVoltar={onSair}
        onAcao={onMateriais}
        acaoIcone="folder-open-outline"
        acaoRotulo="Materiais da sala"
      />
      {/* A FlatList cuida da rolagem e recebe apenas dúvidas não arquivadas; vazia, explica a espera ou o fim da sala. */}
      <FlatList
        data={duvidas.filter((item) => String(item.estado).toLowerCase() !== 'arquivada')}
        keyExtractor={(item) => String(item.id)}
        renderItem={cartaoDuvida}
        contentContainerStyle={styles.feed}
        ListHeaderComponent={
          <>
            {/* Participantes e cronômetro vêm da API; os avisos explicam encerramento e falhas. */}
            <View style={styles.linha}>
              <Text style={estilos.legenda}>
                {dadosSala.totalAlunos == null
                  ? 'Consultando participantes…'
                  : dadosSala.totalAlunos + ' participantes'}
              </Text>
              <Text style={styles.relogio}>
                {cronometro && cronometro.ativo
                  ? '◷ ' + formatarTempo(cronometro.segundosRestantes)
                  : 'Sem cronômetro ativo'}
              </Text>
            </View>
            {encerrada ? (
              <Mensagem texto="Esta sala foi encerrada. Você pode voltar à entrada." />
            ) : null}
            <Mensagem texto={erroConsulta} />
            <Mensagem texto={erro} />
            <Mensagem texto={feedback} tipo="sucesso" />
            {/* map cria um cartão por material fixado, com seu id como chave e código em painel escuro. */}
            {materiais
              .filter((item) => item.fixado)
              .map((material) => (
                <Cartao key={material.id} style={material.tipo === 'codigo' ? styles.codigo : null}>
                  <Text style={[estilos.rotulo, material.tipo === 'codigo' && styles.textoCodigo]}>
                    📌 {material.titulo}
                  </Text>
                  {material.tipo === 'codigo' ? (
                    <Text selectable style={[styles.textoCodigo, styles.conteudoCodigo]}>
                      {material.conteudoOuUrl}
                    </Text>
                  ) : (
                    <Text style={estilos.texto}>
                      {material.detalhe || 'Material fixado pelo professor'}
                    </Text>
                  )}
                  <Botao
                    titulo={material.tipo === 'codigo' ? 'COPIAR' : 'ABRIR MATERIAL'}
                    pequeno
                    cor="claro"
                    onPress={() => abrir(material)}
                    carregando={abrindo === material.id}
                    desabilitado={abrindo !== null}
                  />
                </Cartao>
              ))}
            {/* O convite só aparece quando existe pergunta disponível em uma sala ainda ativa. */}
            {quiz && !encerrada ? (
              <Botao
                titulo="QUIZ DISPONÍVEL — PARTICIPAR"
                cor="roxo"
                icone="trophy-outline"
                onPress={onQuiz}
                style={styles.quiz}
              />
            ) : null}
            <Text style={[estilos.titulo, styles.titulo]}>✨ Feed da sala</Text>
          </>
        }
        ListEmptyComponent={
          <Text style={estilos.vazio}>
            {carregando
              ? 'Buscando as dúvidas…'
              : encerrada
                ? 'Não há dúvidas nesta lista. A sala foi encerrada.'
                : 'Nenhuma dúvida por aqui ainda. Envie a sua!'}
          </Text>
        }
      />
      {/* O rodapé mantém materiais e nova dúvida acessíveis mesmo quando o feed está vazio. */}
      <View style={styles.rodape}>
        <Botao
          titulo="MATERIAIS"
          pequeno
          cor="claro"
          onPress={onMateriais}
          icone="folder-outline"
        />
        <Botao
          titulo="NOVA DÚVIDA"
          pequeno
          cor="laranja"
          onPress={() => {
            setErroEnvio('');
            setModal(true);
          }}
          desabilitado={encerrada}
          icone="add"
        />
      </View>
      {/* O modal reúne a pergunta de até 280 caracteres e só fecha pelo retorno se não estiver enviando. */}
      <Modal
        visible={modal}
        transparent
        animationType="fade"
        onRequestClose={() => {
          if (!enviando) setModal(false);
        }}
      >
        <View style={styles.sobreposicao}>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={styles.modalRolagem}
          >
            <Cartao style={styles.modal}>
              <Text style={[estilos.titulo, styles.titulo]}>Qual é sua dúvida?</Text>
              <Campo
                rotulo="SUA PERGUNTA"
                valor={texto}
                onChangeText={setTexto}
                placeholder="Escreva sua dúvida…"
                multiline
                maxLength={280}
                editable={!enviando}
              />
              <Text style={estilos.legenda}>{texto.length}/280 caracteres</Text>
              {/* A opção de anonimato muda a identificação no feed, sem ocultá-la do professor. */}
              <View style={styles.linha}>
                <Text style={estilos.rotulo}>ENVIAR COMO ANÔNIMO</Text>
                <Switch
                  accessibilityLabel="Enviar como anônimo"
                  value={anonima}
                  onValueChange={setAnonima}
                  disabled={enviando}
                  trackColor={{ true: cores.azul }}
                />
              </View>
              <Text style={[estilos.legenda, styles.quiz]}>
                No modo anônimo, só o professor vê quem enviou.
              </Text>
              {/* Falhas ficam no formulário; enviar e cancelar respeitam a espera do pedido. */}
              <Mensagem texto={erroEnvio} />
              <Botao
                titulo="ENVIAR DÚVIDA"
                onPress={enviar}
                carregando={enviando}
                desabilitado={encerrada}
              />
              <Botao
                titulo="CANCELAR"
                cor="claro"
                onPress={() => setModal(false)}
                desabilitado={enviando}
                style={styles.cancelar}
              />
            </Cartao>
          </ScrollView>
        </View>
      </Modal>
    </Pagina>
  );
}

// StyleSheet reúne os estilos do feed, dos cartões e do modal sem misturá-los com as consultas da sala.
const styles = StyleSheet.create({
  feed: { padding: 20, paddingBottom: 12, flexGrow: 1 },
  linha: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
    marginBottom: 16,
    flexWrap: 'wrap',
  },
  autor: { flex: 1 },
  avatar: {
    backgroundColor: cores.azulClaro,
    borderRadius: 22,
    padding: 11,
    fontFamily: 'Nunito_800ExtraBold',
    color: cores.azulEscuro,
  },
  estado: {
    fontFamily: 'Nunito_700Bold',
    fontSize: 11,
    color: cores.verdeEscuro,
    backgroundColor: cores.verdeClaro,
    padding: 7,
    borderRadius: 12,
  },
  duvida: { marginTop: 8, marginBottom: 22 },
  voto: { alignSelf: 'flex-start' },
  relogio: {
    fontFamily: 'Nunito_800ExtraBold',
    fontSize: 13,
    color: cores.laranjaEscuro,
    backgroundColor: cores.laranjaClaro,
    padding: 9,
    borderRadius: 12,
  },
  codigo: { backgroundColor: '#002537', borderColor: cores.azulEscuro },
  textoCodigo: { color: '#BCDCEA' },
  conteudoCodigo: { fontFamily: 'monospace', fontSize: 13, lineHeight: 22, marginVertical: 20 },
  titulo: { marginBottom: 20 },
  quiz: { marginBottom: 20 },
  rodape: {
    padding: 16,
    borderTopWidth: 2,
    borderColor: cores.borda,
    backgroundColor: cores.branco,
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 8,
  },
  sobreposicao: { flex: 1, backgroundColor: '#07334599', justifyContent: 'center' },
  modalRolagem: { flexGrow: 1, justifyContent: 'center', padding: 20 },
  modal: { width: '100%', maxWidth: 440, alignSelf: 'center', marginBottom: 0 },
  cancelar: { marginTop: 12 },
});
