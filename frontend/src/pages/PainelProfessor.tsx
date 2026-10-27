import { useEffect, useState } from 'react';
import { View, Text, Image, Modal, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api from '../services/api';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

import type { ComponentProps } from 'react';
import type { Professor, AcessoProfessor, Sala, Material, Quiz } from '../types/modelos';

// Recebe o professor, as credenciais da sessão e as ações de navegação controladas pelo App.
type Props = {
  professor: Professor;
  acesso: AcessoProfessor;
  onCriarSala: (sala: Sala) => void;
  onAbrirSala: (sala: Sala) => void;
  onPerfil: () => void;
  onCriarQuiz: () => void;
  onPerfilPublico: () => void;
  onSair: () => void;
};

// Mostra o resumo do professor e permite criar ou retomar suas salas reais.
export default function PainelProfessor({
  professor,
  acesso,
  onCriarSala,
  onAbrirSala,
  onPerfil,
  onCriarQuiz,
  onPerfilPublico,
  onSair,
}: Props) {
  // useState guarda as listas recebidas e atualiza a tela quando uma consulta termina.
  const [salas, setSalas] = useState<Sala[]>([]);
  const [materiais, setMateriais] = useState<Material[]>([]);
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  // Diferencia a consulta em andamento de uma lista já carregada, inclusive quando vazia.
  const [carregando, setCarregando] = useState(true);
  const [carregado, setCarregado] = useState(false);
  const [erro, setErro] = useState('');
  // Mantém o nome da nova aula e os avisos da criação separados dos avisos do painel.
  const [modal, setModal] = useState(false);
  const [nomeAula, setNomeAula] = useState('');
  const [criando, setCriando] = useState(false);
  const [erroSala, setErroSala] = useState('');

  async function carregar() {
    // Limpa o erro anterior e consulta as três listas em paralelo para montar o resumo.
    setCarregando(true);
    setErro('');
    try {
      const listas = await Promise.all([
        api.listarSalas(professor.id, acesso),
        api.listarMateriaisProfessor(professor.id, acesso),
        api.listarQuizzes(professor.id, acesso),
      ]);
      // Só substitui as listas quando todas as consultas foram concluídas.
      setSalas(listas[0]);
      setMateriais(listas[1]);
      setQuizzes(listas[2]);
      setCarregado(true);
    } catch (e) {
      // Informa a falha sem substituir dados já carregados por contagens inventadas.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Retira a indicação de espera para permitir outra atualização manual.
      setCarregando(false);
    }
  }
  // useEffect consulta ao montar a tela e repete a consulta se o id do professor mudar.
  useEffect(() => {
    carregar();
  }, [professor.id]);

  async function criar() {
    // Evita dois envios e exige o nome que identificará a aula na lista de salas.
    if (criando) return;
    if (!nomeAula.trim()) {
      setErroSala('Dê um nome para a aula.');
      return;
    }
    // Mantém o modal aberto e bloqueado enquanto o Spring cria a sala.
    setCriando(true);
    setErroSala('');
    try {
      // A API gera os dados da sala; o App recebe esse resultado para abri-la.
      const sala = await api.criarSala(professor.id, nomeAula.trim(), acesso);
      setModal(false);
      setNomeAula('');
      onCriarSala(sala);
    } catch (e) {
      // Avisa dentro do formulário, preservando o nome da aula para uma nova tentativa.
      setErroSala(e instanceof Error ? e.message : 'Não foi possível criar a sala.');
    } finally {
      // Libera o formulário independentemente do resultado da criação.
      setCriando(false);
    }
  }
  // Soma as participações das salas; materiais e quizzes são contados pelas suas listas.
  let participantes = 0;
  for (const sala of salas) participantes += sala.totalAlunos || 0;
  const contagens: {
    titulo: string;
    valor: number;
    icone: ComponentProps<typeof Ionicons>['name'];
    cor: string;
  }[] = [
    { titulo: 'PARTICIPANTES', valor: participantes, icone: 'people-outline', cor: cores.roxo },
    { titulo: 'MATERIAIS', valor: materiais.length, icone: 'book-outline', cor: cores.laranja },
    { titulo: 'QUIZZES', valor: quizzes.length, icone: 'calendar-outline', cor: cores.verde },
  ];

  return (
    <Pagina>
      {/* A saudação usa o nome da sessão e o cartão apresenta o convite para criar uma aula. */}
      <Cabecalho
        titulo={professor.nome}
        subtitulo="OLÁ DE NOVO,"
        onAcao={onPerfil}
        acaoRotulo="Meu perfil"
      />
      <Cartao style={s.hero}>
        <View style={{ flex: 1 }}>
          <Text style={estilos.rotulo}>COMECE AGORA</Text>
          <Text style={[estilos.titulo, { marginVertical: 12 }]}>
            Preparado para criar uma sala?
          </Text>
          <Text style={estilos.texto}>Convide sua turma para aprender junto!</Text>
        </View>
        <Image source={require('../../assets/mascote.png')} style={s.mascote} />
      </Cartao>
      {/* Abre um formulário limpo de erros; a sala só será criada ao confirmar no modal. */}
      <Botao
        titulo="CRIAR NOVA SALA"
        icone="rocket-outline"
        onPress={() => {
          setErroSala('');
          setModal(true);
        }}
        style={estilos.espaco}
      />
      <Mensagem texto={erro} />
      {/* map transforma cada resumo em um bloco visual, identificado pelo título. */}
      <Cartao style={s.contagens}>
        {contagens.map((item) => (
          <View key={item.titulo} style={s.contagem}>
            <Ionicons name={item.icone} size={27} color={item.cor} />
            <Text style={estilos.titulo}>{carregado ? item.valor : '—'}</Text>
            <Text style={[estilos.legenda, { fontSize: 10 }]}>{item.titulo}</Text>
          </View>
        ))}
      </Cartao>
      {/* Os atalhos chamam o App para abrir criação de quiz, conteúdos ou perfil público. */}
      <Text style={[estilos.subtitulo, estilos.espaco]}>Ações rápidas</Text>
      <View style={s.acoes}>
        <Botao
          titulo="CRIAR QUIZ"
          icone="create-outline"
          cor="laranja"
          pequeno
          onPress={onCriarQuiz}
          style={s.acao}
        />
        <Botao
          titulo="CONTEÚDOS"
          icone="book-outline"
          cor="roxo"
          pequeno
          onPress={onPerfil}
          style={s.acao}
        />
        <Botao
          titulo="PERFIL PÚBLICO"
          icone="globe-outline"
          cor="azul"
          pequeno
          onPress={onPerfilPublico}
          style={s.acao}
        />
      </View>
      {/* A atualização manual renova as salas e diferencia espera de ausência de registros. */}
      <Text style={[estilos.subtitulo, { marginVertical: 20 }]}>Minhas salas</Text>
      <Botao
        titulo="ATUALIZAR"
        pequeno
        cor="claro"
        onPress={carregar}
        carregando={carregando}
        style={estilos.espaco}
      />
      {carregando ? (
        <Text style={estilos.vazio}>Carregando suas salas…</Text>
      ) : carregado && salas.length === 0 ? (
        <Text style={estilos.vazio}>Você ainda não criou uma sala.</Text>
      ) : null}
      {/* Cada sala mantém seu código e estado; o id permite ao React reconhecer seu cartão. */}
      {salas.map((sala) => (
        <Cartao key={sala.id}>
          <Text style={estilos.subtitulo}>{sala.nomeAula}</Text>
          <Text style={estilos.texto}>Código: {sala.codigo}</Text>
          <Text style={[estilos.legenda, estilos.espaco]}>
            {sala.status} · {sala.totalAlunos || 0} participantes
          </Text>
          <Botao
            titulo={sala.status === 'ativa' ? 'RETOMAR SALA' : 'VER SALA'}
            cor="azul"
            pequeno
            onPress={() => onAbrirSala(sala)}
          />
        </Cartao>
      ))}
      {/* A saída entrega ao App a responsabilidade de encerrar a sessão em memória. */}
      <Botao titulo="SAIR DA CONTA" cor="claro" pequeno onPress={onSair} />
      <Marca legenda="VERSÃO DOS PROFESSORES" />
      {/* O modal recolhe o nome da aula e impede o fechamento durante a criação. */}
      <Modal
        visible={modal}
        animationType="slide"
        onRequestClose={() => {
          if (!criando) setModal(false);
        }}
      >
        <Pagina>
          <Cabecalho titulo="Nova sala" onVoltar={criando ? undefined : () => setModal(false)} />
          <Cartao style={{ marginTop: 24 }}>
            <Mensagem texto={erroSala} />
            <Campo
              rotulo="Nome da aula"
              valor={nomeAula}
              onChangeText={setNomeAula}
              maxLength={255}
              editable={!criando}
            />
            <Botao titulo="CRIAR E ABRIR SALA" onPress={criar} carregando={criando} />
          </Cartao>
        </Pagina>
      </Modal>
    </Pagina>
  );
}

// StyleSheet reúne as medidas da tela: organiza o convite, o resumo e os atalhos em linhas.
const s = StyleSheet.create({
  hero: {
    marginTop: 24,
    backgroundColor: cores.azulClaro,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  // Preserva a proporção do mascote e distribui as três contagens pelo cartão.
  mascote: { width: 100, height: 130, resizeMode: 'contain' },
  contagens: { flexDirection: 'row', paddingHorizontal: 8 },
  contagem: { flex: 1, alignItems: 'center' },
  // Permite que os atalhos quebrem de linha quando a largura da tela for pequena.
  acoes: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  acao: { flexGrow: 1, flexBasis: 100, minHeight: 88 },
});
