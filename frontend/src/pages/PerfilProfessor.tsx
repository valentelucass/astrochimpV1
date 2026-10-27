import { useEffect, useState } from 'react';
import { View, Text, Image, Switch, Modal, StyleSheet } from 'react-native';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api, { ErroApi } from '../services/api';
import { abrirMaterial } from '../services/arquivos';
import { formatarData, iniciais } from '../services/formatacao';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

import type { Professor, AcessoProfessor, Material, Quiz } from '../types/modelos';

// Recebe o perfil e o acesso do professor; o App controla as outras telas e guarda as alterações.
type Props = {
  professor: Professor;
  acesso: AcessoProfessor;
  abaInicial?: 'materiais' | 'quizzes';
  onVoltar: () => void;
  onCriarMaterial: () => void;
  onCriarQuiz: () => void;
  onPerfilPublico: () => void;
  onAtualizarProfessor: (perfil: Professor) => void;
};

// Permite administrar os materiais públicos, os quizzes salvos e os dados do próprio perfil.
export default function PerfilProfessor({
  professor,
  acesso,
  abaInicial = 'materiais',
  onVoltar,
  onCriarMaterial,
  onCriarQuiz,
  onPerfilPublico,
  onAtualizarProfessor,
}: Props) {
  // useState mantém a escolha da aba e as listas, redesenhando a tela quando elas mudam.
  const [aba, setAba] = useState<'materiais' | 'quizzes'>(abaInicial);
  const [materiais, setMateriais] = useState<Material[]>([]);
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  // Separa a espera pelas listas das ações de publicar, abrir, salvar ou excluir.
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [ocupado, setOcupado] = useState(false);
  // Um único modal alterna entre edição, leitura do quiz e confirmação de exclusão.
  const [modal, setModal] = useState<'' | 'configuracao' | 'quiz' | 'excluir'>('');
  const [erroModal, setErroModal] = useState('');
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  // Guarda um rascunho dos dados do perfil, sem alterar o que o App recebeu antes de salvar.
  const [nome, setNome] = useState('');
  const [descricao, setDescricao] = useState('');
  const [fotoUrl, setFotoUrl] = useState('');
  const [slug, setSlug] = useState('');
  // Permite mostrar as iniciais quando o endereço da foto não carregar.
  const [fotoFalhou, setFotoFalhou] = useState(false);

  async function carregar() {
    // Consulta materiais e quizzes ao mesmo tempo e mostra a espera no botão de atualizar.
    setCarregando(true);
    setErro('');
    try {
      const listas = await Promise.all([
        api.listarMateriaisProfessor(professor.id, acesso),
        api.listarQuizzes(professor.id, acesso),
      ]);
      // Substitui as listas locais pelos registros que pertencem ao professor conectado.
      setMateriais(listas[0]);
      setQuizzes(listas[1]);
    } catch (e) {
      // Mantém os dados anteriores e apresenta o motivo da consulta não ter terminado.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Encerra a indicação de carregamento, inclusive se uma das listas falhar.
      setCarregando(false);
    }
  }
  // useEffect executa a consulta ao abrir a tela e quando muda o professor identificado pelo id.
  useEffect(() => {
    carregar();
  }, [professor.id]);
  // Uma nova URL deve ter outra chance de carregar, mesmo se a foto anterior falhou.
  useEffect(() => {
    setFotoFalhou(false);
  }, [professor.fotoUrl]);

  async function publicar(material: Material, publico: boolean) {
    // Bloqueia ações simultâneas e remove mensagens da operação anterior.
    if (ocupado) return;
    setOcupado(true);
    setErro('');
    setSucesso('');
    try {
      // Confirma a visibilidade no servidor antes de mudar o interruptor local.
      await api.publicarNoPerfil(professor.id, material.id, publico, acesso);
      // map percorre a lista e devolve uma nova versão, alterando apenas o material escolhido.
      setMateriais(
        materiais.map((item) => (item.id === material.id ? { ...item, publico } : item)),
      );
      setSucesso(
        publico ? 'Material publicado no perfil público.' : 'Material agora está privado.',
      );
    } catch (e) {
      // Se a publicação falhar, o material continua com a visibilidade anterior na tela.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera os botões após a resposta da publicação.
      setOcupado(false);
    }
  }
  async function abrir(material: Material) {
    // Evita abrir ou baixar o mesmo conteúdo várias vezes enquanto a ação está pendente.
    if (ocupado) return;
    setOcupado(true);
    setErro('');
    setSucesso('');
    try {
      // O serviço usa o tipo do material para abrir link, copiar código ou baixar com autenticação.
      await abrirMaterial(material, acesso);
      setSucesso(
        material.tipo === 'codigo'
          ? 'Código copiado.'
          : 'Material aberto ou enviado para download.',
      );
    } catch (e) {
      // Mostra também falhas do aparelho ao abrir ou compartilhar o arquivo.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      setOcupado(false);
    }
  }
  function configurar() {
    // Preenche o formulário com o perfil atual toda vez que as configurações são abertas.
    setNome(professor.nome || '');
    setDescricao(professor.descricao || '');
    setFotoUrl(professor.fotoUrl || '');
    setSlug(professor.slugPerfilPublico || '');
    setErroModal('');
    setModal('configuracao');
  }
  async function salvar() {
    // Exige nome e slug válido, e confere a URL da foto somente quando foi preenchida.
    if (ocupado) return;
    if (!nome.trim()) {
      setErroModal('Informe seu nome.');
      return;
    }
    if (!/^[a-z0-9-]+$/.test(slug.trim())) {
      setErroModal('Use letras minúsculas, números e hífen no endereço público.');
      return;
    }
    if (fotoUrl.trim() && !/^https?:\/\/\S+$/i.test(fotoUrl.trim())) {
      setErroModal('Informe um endereço http ou https para a foto.');
      return;
    }
    // Mantém o formulário bloqueado até o Spring confirmar os novos dados.
    setOcupado(true);
    setErroModal('');
    try {
      const atualizado = await api.atualizarProfessor(
        professor.id,
        {
          nome: nome.trim(),
          descricao: descricao.trim(),
          fotoUrl: fotoUrl.trim(),
          slugPerfilPublico: slug.trim(),
        },
        acesso,
      );
      // Entrega o perfil confirmado ao App e fecha a edição com um aviso de sucesso.
      onAtualizarProfessor(atualizado);
      setModal('');
      setSucesso('Perfil atualizado.');
    } catch (e) {
      // O erro fica no modal para permitir corrigir os campos sem perder o rascunho.
      setErroModal(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      setOcupado(false);
    }
  }
  async function detalhar(item: Quiz) {
    // Busca as perguntas completas do quiz escolhido antes de abrir sua leitura.
    if (ocupado) return;
    setOcupado(true);
    setErro('');
    setSucesso('');
    try {
      // Só apresenta o modal de questões quando os detalhes vieram do servidor.
      setQuiz(await api.buscarQuiz(item.id, acesso));
      setErroModal('');
      setModal('quiz');
    } catch (e) {
      // A falha aparece na lista, pois o modal de leitura ainda não foi aberto.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Reabilita a escolha de outro quiz após a consulta.
      setOcupado(false);
    }
  }
  async function excluir() {
    // A confirmação só pode prosseguir com um quiz selecionado e sem outro pedido em andamento.
    if (ocupado || !quiz) return;
    setOcupado(true);
    setErroModal('');
    try {
      // Remove o cartão da lista somente depois que o servidor confirma a exclusão.
      await api.excluirQuiz(quiz.id, acesso);
      setQuizzes(quizzes.filter((item) => item.id !== quiz.id));
      setModal('');
      setQuiz(null);
      setSucesso('Quiz excluído.');
    } catch (e) {
      // O status 409 explica por que um quiz em uso ou com respostas precisa ser preservado.
      setErroModal(
        e instanceof ErroApi && e.status === 409
          ? 'Não é possível excluir um quiz em uso ou com respostas registradas.'
          : e instanceof Error
            ? e.message
            : 'Não foi possível excluir o quiz.',
      );
    } finally {
      // Libera a confirmação ou o cancelamento se a exclusão não puder terminar.
      setOcupado(false);
    }
  }
  function fechar() {
    // Evita fechar um modal enquanto sua alteração está sendo enviada ao servidor.
    if (!ocupado) setModal('');
  }

  return (
    <Pagina>
      {/* O cabeçalho abre a edição; o cartão azul apresenta a foto ou as iniciais do professor. */}
      <Cabecalho
        titulo="Perfil profissional"
        onVoltar={onVoltar}
        onAcao={configurar}
        acaoRotulo="Editar perfil"
      />
      <Cartao style={s.perfil} corDaBorda={cores.azulEscuro}>
        {professor.fotoUrl && !fotoFalhou ? (
          <Image
            source={{ uri: professor.fotoUrl }}
            onError={() => setFotoFalhou(true)}
            style={s.foto}
          />
        ) : (
          <View style={s.foto}>
            <Text style={estilos.titulo}>{iniciais(professor.nome)}</Text>
          </View>
        )}
        <View style={{ flex: 1 }}>
          <Text style={[estilos.titulo, s.branco]}>{professor.nome}</Text>
          <Text style={[estilos.texto, s.branco]}>
            {professor.descricao || 'Adicione uma descrição nas configurações do perfil.'}
          </Text>
        </View>
      </Cartao>
      {/* As abas trocam a lista visível sem sair do perfil nem fazer outro pedido. */}
      <View style={s.abas}>
        <Botao
          titulo="MEUS MATERIAIS"
          pequeno
          cor={aba === 'materiais' ? 'azul' : 'claro'}
          onPress={() => setAba('materiais')}
          style={{ flex: 1 }}
        />
        <Botao
          titulo="QUIZZES SALVOS"
          pequeno
          cor={aba === 'quizzes' ? 'azul' : 'claro'}
          onPress={() => setAba('quizzes')}
          style={{ flex: 1 }}
        />
      </View>
      {/* Os avisos pertencem às ações da página; o botão renova as duas listas. */}
      <Mensagem texto={erro} />
      <Mensagem texto={sucesso} tipo="sucesso" />
      <Botao
        titulo="ATUALIZAR LISTAS"
        pequeno
        cor="claro"
        carregando={carregando}
        desabilitado={ocupado}
        onPress={carregar}
        style={estilos.espaco}
      />
      {/* Enquanto consulta, mostra espera; depois apresenta a contagem da aba escolhida. */}
      {carregando ? (
        <Text style={estilos.vazio}>Carregando conteúdos…</Text>
      ) : (
        <>
          <Text style={[estilos.rotulo, estilos.espaco]}>
            {aba === 'materiais'
              ? materiais.length + ' MATERIAIS'
              : quizzes.length + ' QUIZZES SALVOS'}
          </Text>
          {/* A aba de materiais informa ausência de conteúdo e permite criar o primeiro. */}
          {aba === 'materiais' ? (
            <>
              {materiais.length === 0 ? (
                <Text style={estilos.vazio}>Seus materiais aparecerão aqui. Crie o primeiro!</Text>
              ) : null}
              {/* Cada cartão oferece publicação e a ação correspondente ao tipo do material. */}
              {materiais.map((material) => (
                <Cartao key={material.id}>
                  <Text style={estilos.subtitulo}>{material.titulo}</Text>
                  <Text style={[estilos.legenda, estilos.espaco]}>
                    {material.tipo} · {formatarData(material.criadoEm)}
                  </Text>
                  {/* A mudança de público ou privado é enviada ao Spring pelo interruptor. */}
                  <View style={[estilos.linha, estilos.espaco]}>
                    <Switch
                      accessibilityLabel={'Publicar ' + material.titulo}
                      value={!!material.publico}
                      disabled={ocupado}
                      onValueChange={(valor) => publicar(material, valor)}
                      trackColor={{ true: cores.verde, false: cores.cinza }}
                    />
                    <Text style={estilos.rotulo}>{material.publico ? ' PÚBLICO' : ' PRIVADO'}</Text>
                  </View>
                  <Botao
                    titulo={
                      material.tipo === 'codigo'
                        ? 'COPIAR CÓDIGO'
                        : material.tipo === 'link'
                          ? 'ABRIR LINK'
                          : 'BAIXAR ARQUIVO'
                    }
                    pequeno
                    cor="claro"
                    desabilitado={ocupado}
                    onPress={() => abrir(material)}
                  />
                </Cartao>
              ))}
              <Botao titulo="CRIAR MATERIAL" icone="add" cor="laranja" onPress={onCriarMaterial} />
            </>
          ) : (
            <>
              {quizzes.length === 0 ? (
                <Text style={estilos.vazio}>Nenhum quiz salvo ainda.</Text>
              ) : null}
              {/* Os quizzes salvos podem ser consultados ou selecionados para confirmar exclusão. */}
              {quizzes.map((item) => (
                <Cartao key={item.id}>
                  <Text style={[estilos.subtitulo, estilos.espaco]}>{item.titulo}</Text>
                  <Botao
                    titulo="VER QUESTÕES"
                    cor="claro"
                    pequeno
                    desabilitado={ocupado}
                    onPress={() => detalhar(item)}
                    style={estilos.espaco}
                  />
                  <Botao
                    titulo="EXCLUIR QUIZ"
                    cor="laranja"
                    pequeno
                    desabilitado={ocupado}
                    onPress={() => {
                      setQuiz(item);
                      setErroModal('');
                      setModal('excluir');
                    }}
                  />
                </Cartao>
              ))}
              <Botao titulo="CRIAR QUIZ" icone="add" cor="roxo" onPress={onCriarQuiz} />
            </>
          )}
        </>
      )}
      {/* Abre a visão que os visitantes recebem dos materiais publicados. */}
      <Botao
        titulo="VER PERFIL PÚBLICO"
        cor="claro"
        icone="globe-outline"
        onPress={onPerfilPublico}
        style={{ marginTop: 20 }}
      />
      <Marca legenda="HUB DO PROFESSOR" />
      {/* O valor de modal escolhe o conteúdo, e seus erros ficam dentro desta janela. */}
      <Modal visible={!!modal} animationType="slide" onRequestClose={fechar}>
        <Pagina>
          <Cabecalho
            titulo={
              modal === 'configuracao'
                ? 'Editar perfil'
                : modal === 'excluir'
                  ? 'Excluir quiz'
                  : 'Questões do quiz'
            }
            onVoltar={ocupado ? undefined : fechar}
          />
          <View style={{ marginTop: 24 }}>
            <Mensagem texto={erroModal} />
          </View>
          {/* A edição usa o rascunho local e só altera o perfil ao confirmar o salvamento. */}
          {modal === 'configuracao' ? (
            <Cartao>
              <Campo
                rotulo="Nome"
                valor={nome}
                onChangeText={setNome}
                maxLength={255}
                editable={!ocupado}
              />
              <Campo
                rotulo="Descrição"
                valor={descricao}
                onChangeText={setDescricao}
                multiline
                maxLength={255}
                editable={!ocupado}
              />
              <Campo
                rotulo="Endereço da foto (opcional)"
                valor={fotoUrl}
                onChangeText={setFotoUrl}
                autoCapitalize="none"
                keyboardType="url"
                maxLength={255}
                editable={!ocupado}
              />
              <Campo
                rotulo="Endereço público"
                valor={slug}
                onChangeText={setSlug}
                autoCapitalize="none"
                maxLength={255}
                editable={!ocupado}
              />
              <Text style={[estilos.legenda, estilos.espaco]}>
                O endereço público identifica sua página de materiais. Alterá-lo muda o endereço
                compartilhado.
              </Text>
              <Botao titulo="SALVAR PERFIL" carregando={ocupado} onPress={salvar} />
            </Cartao>
          ) : modal === 'excluir' && quiz ? (
            <Cartao>
              {/* A exclusão exige confirmar o quiz pelo título; cancelar apenas fecha a janela. */}
              <Text style={[estilos.subtitulo, estilos.espaco]}>Excluir “{quiz.titulo}”?</Text>
              <Text style={[estilos.texto, estilos.espaco]}>
                Esta ação apaga o quiz. Não é possível desfazer.
              </Text>
              <Botao
                titulo="CONFIRMAR EXCLUSÃO"
                cor="laranja"
                carregando={ocupado}
                onPress={excluir}
                style={estilos.espaco}
              />
              <Botao titulo="CANCELAR" cor="claro" desabilitado={ocupado} onPress={fechar} />
            </Cartao>
          ) : modal === 'quiz' && quiz ? (
            <>
              {/* Mostra as questões na ordem recebida, com dificuldade e limite de tempo. */}
              <Text style={[estilos.titulo, estilos.espaco]}>{quiz.titulo}</Text>
              {(quiz.perguntas || []).map((pergunta, indice) => (
                <Cartao key={pergunta.id || indice}>
                  <Text style={[estilos.subtitulo, estilos.espaco]}>
                    {indice + 1}. {pergunta.enunciado}
                  </Text>
                  <Text style={[estilos.legenda, estilos.espaco]}>
                    {pergunta.dificuldade} · {pergunta.tempoLimiteSegundos} segundos
                  </Text>
                  {/* O professor pode conferir o gabarito, com a alternativa correta em verde. */}
                  {(pergunta.alternativas || []).map((alternativa, numero) => (
                    <Text
                      key={alternativa.id || numero}
                      style={[
                        estilos.texto,
                        {
                          marginBottom: 8,
                          color: alternativa.correta ? cores.verdeEscuro : cores.texto,
                        },
                      ]}
                    >
                      {alternativa.letra}. {alternativa.texto}
                      {alternativa.correta ? ' ✓ Correta' : ''}
                    </Text>
                  ))}
                </Cartao>
              ))}
            </>
          ) : null}
        </Pagina>
      </Modal>
    </Pagina>
  );
}

// StyleSheet reúne os estilos locais do cartão de perfil, do avatar e da navegação por abas.
const s = StyleSheet.create({
  perfil: {
    marginTop: 24,
    backgroundColor: cores.azul,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  // O mesmo círculo acomoda a foto carregada ou as iniciais usadas quando ela falta.
  foto: {
    width: 75,
    height: 75,
    borderRadius: 38,
    backgroundColor: cores.azulClaro,
    alignItems: 'center',
    justifyContent: 'center',
  },
  // Contrasta os textos com o fundo azul e distribui os dois botões de abas na mesma linha.
  branco: { color: cores.branco },
  abas: { flexDirection: 'row', gap: 8, marginBottom: 24 },
});
