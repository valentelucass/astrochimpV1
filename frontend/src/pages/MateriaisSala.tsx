import { useEffect, useState } from 'react';
import { View, Text, FlatList, Modal, ActivityIndicator, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import api from '../services/api';
import type { Professor, Sala, AcessoProfessor, Material, Aluno } from '../types/modelos';
import { abrirMaterial } from '../services/arquivos';
import { formatarData } from '../services/formatacao';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

// Relaciona os tipos enviados pelo Spring aos ícones e nomes usados nos cartões.
const icones: Record<string, 'link-outline' | 'code-slash-outline' | 'document-text-outline'> = {
  link: 'link-outline',
  codigo: 'code-slash-outline',
  arquivo: 'document-text-outline',
};
const tipos: Record<string, string> = { link: 'LINK', codigo: 'CÓDIGO', arquivo: 'ARQUIVO' };

// Recebe a sala e a sessão do professor ou aluno; criar material só aparece quando o App fornece essa ação.
type Props = {
  sala: Sala;
  professor?: Professor | null;
  acesso?: AcessoProfessor | null;
  aluno?: Aluno | null;
  onVoltar: () => void;
  onCriarMaterial?: () => void;
};

// Mostra os conteúdos da aula e libera as ações de publicação e fixação apenas para o professor.
export default function MateriaisSala({
  sala,
  professor,
  acesso,
  aluno,
  onVoltar,
  onCriarMaterial,
}: Props) {
  // Alunos usam o token da entrada na sala; professores usam e-mail e senha.
  const acessoAtual = aluno ? { tokenAluno: aluno.tokenAluno } : (acesso ?? null);
  // useState guarda a lista recebida e os avisos; os setters atualizam o que aparece sem trocar de página.
  const [materiais, setMateriais] = useState<Material[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  // O id ocupado bloqueia ações simultâneas; tentativa pede uma nova consulta à sala.
  const [ocupado, setOcupado] = useState<Material['id'] | null>(null);
  const [tentativa, setTentativa] = useState(0);
  // A janela do perfil tem lista, carregamento e erro próprios para não misturar os avisos da sala.
  const [mostrarPerfil, setMostrarPerfil] = useState(false);
  const [materiaisPerfil, setMateriaisPerfil] = useState<Material[]>([]);
  const [carregandoPerfil, setCarregandoPerfil] = useState(false);
  const [erroPerfil, setErroPerfil] = useState('');

  // useEffect consulta os materiais ao abrir a tela ou mudar sala, acesso ou tentativa de atualização.
  useEffect(() => {
    let ativa = true;
    setCarregando(true);
    setErro('');
    // Busca os materiais permitidos para esta sessão e só aplica a resposta enquanto a tela está ativa.
    api
      .listarMateriaisSala(sala.id, acessoAtual)
      .then((lista) => {
        if (ativa) setMateriais(lista || []);
      })
      .catch((e: unknown) => {
        // Mostra a falha da consulta somente se esta ainda for a tela ativa.
        if (ativa) setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
      })
      .finally(() => {
        // Encerra o carregamento desta consulta sem interferir na próxima tela.
        if (ativa) setCarregando(false);
      });
    // Ao sair ou trocar a consulta, ignora respostas antigas que ainda estejam chegando.
    return () => {
      ativa = false;
    };
  }, [
    sala.id,
    aluno && aluno.tokenAluno,
    acesso && acesso.email,
    acesso && acesso.senha,
    tentativa,
  ]);

  // Encaminha o material ao serviço que abre links, copia código ou baixa arquivos autenticados.
  async function abrir(material: Material) {
    if (ocupado !== null) return;
    setOcupado(material.id);
    setErro('');
    setSucesso('');
    try {
      await abrirMaterial(material, acessoAtual, sala.id);
      if (material.tipo === 'codigo') setSucesso('Código copiado!');
    } catch (e: unknown) {
      // Mostra a falha de abertura ou cópia na própria página da sala.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera as ações dos cartões quando a abertura termina.
      setOcupado(null);
    }
  }

  // Envia a nova fixação apenas com acesso de professor e atualiza a lista após a confirmação.
  async function fixar(material: Material) {
    if (!acesso || aluno) return;
    if (ocupado !== null) return;
    setOcupado(material.id);
    setErro('');
    setSucesso('');
    try {
      await api.fixarMaterial(sala.id, material.id, !material.fixado, acesso);
      // map monta outro array, trocando a fixação só do material que o servidor acabou de confirmar.
      setMateriais(
        materiais.map((item) =>
          item.id === material.id ? { ...item, fixado: !item.fixado } : item,
        ),
      );
    } catch (e: unknown) {
      // Informa a falha sem alterar a fixação que estava na lista.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      setOcupado(null);
    }
  }

  // Abre a janela e consulta os materiais que pertencem ao professor autenticado.
  async function abrirPerfil() {
    if (!professor || !acesso) return;
    setMostrarPerfil(true);
    setCarregandoPerfil(true);
    setErroPerfil('');
    try {
      setMateriaisPerfil((await api.listarMateriaisProfessor(professor.id, acesso)) || []);
    } catch (e: unknown) {
      // Mostra a falha dentro da janela do perfil.
      setErroPerfil(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Encerra a indicação de carregamento dos materiais do professor.
      setCarregandoPerfil(false);
    }
  }

  // Publica um material existente na sala sem criar outra cópia no perfil.
  async function publicar(material: Material) {
    if (!acesso || aluno) return;
    if (ocupado !== null) return;
    setOcupado(material.id);
    setErroPerfil('');
    try {
      await api.publicarMaterial(sala.id, material.id, acesso);
      setMostrarPerfil(false);
      setSucesso('Material enviado para a sala.');
      // Dispara uma nova consulta para trazer a lista oficial da sala após a publicação.
      setTentativa(tentativa + 1);
    } catch (e: unknown) {
      // Mantém a janela aberta com a falha para tentar publicar de novo.
      setErroPerfil(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera os botões após a resposta da publicação.
      setOcupado(null);
    }
  }

  // Ordena uma cópia para mostrar fixados primeiro sem reorganizar o array guardado no estado.
  const ordenados = [...materiais].sort((a, b) => Number(!!b.fixado) - Number(!!a.fixado));
  return (
    <Pagina rolar={false}>
      <Cabecalho titulo="Materiais da sala" subtitulo={sala.nomeAula} onVoltar={onVoltar} />
      {/* A lista cuida da rolagem; o cabeçalho reúne ações do professor, avisos e atualização. */}
      <FlatList
        data={ordenados}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.conteudo}
        ListHeaderComponent={
          <>
            <Text style={estilos.titulo}>Explore e aprenda</Text>
            <Text style={[estilos.texto, estilos.espaco]}>
              Os conteúdos desta aula ficam reunidos aqui.
            </Text>
            {/* Apenas a sessão do professor recebe os caminhos para criar ou publicar conteúdo. */}
            {!aluno ? (
              <View style={estilos.espaco}>
                {onCriarMaterial ? (
                  <Botao
                    titulo="Criar material"
                    icone="add"
                    cor="laranja"
                    onPress={onCriarMaterial}
                    desabilitado={ocupado !== null}
                    style={styles.espaco}
                  />
                ) : null}
                <Botao
                  titulo="Adicionar do meu perfil"
                  icone="folder-open-outline"
                  cor="claro"
                  onPress={abrirPerfil}
                  desabilitado={ocupado !== null}
                />
              </View>
            ) : null}
            {/* Avisa falhas, confirma a cópia de código ou o envio e permite consultar a lista novamente. */}
            <Mensagem texto={erro} />
            <Mensagem texto={sucesso} tipo="sucesso" />
            <Botao
              titulo="Atualizar materiais"
              cor="claro"
              pequeno
              icone="refresh-outline"
              desabilitado={carregando || ocupado !== null}
              onPress={() => setTentativa(tentativa + 1)}
              style={estilos.espaco}
            />
            {carregando ? (
              <ActivityIndicator color={cores.azul} accessibilityLabel="Carregando materiais" />
            ) : null}
          </>
        }
        ListEmptyComponent={
          !carregando && !erro ? (
            <Cartao>
              {/* Explica como a sala receberá seus primeiros materiais, conforme a sessão aberta. */}
              <Ionicons name="book-outline" size={44} color={cores.azul} />
              <Text style={estilos.subtitulo}>A biblioteca está começando</Text>
              <Text style={estilos.texto}>
                {aluno
                  ? 'Quando o professor compartilhar materiais, eles aparecerão aqui.'
                  : 'Crie um material ou envie um conteúdo do seu perfil.'}
              </Text>
            </Cartao>
          ) : null
        }
        renderItem={({ item }) => (
          <Cartao corDaBorda={item.fixado ? cores.roxo : cores.borda}>
            {/* O cartão informa tipo, data e detalhe, com destaque para a fixação na sala. */}
            {item.fixado ? (
              <Text style={[estilos.rotulo, styles.fixado]}>FIXADO NA SALA</Text>
            ) : null}
            <View style={styles.linha}>
              <View style={styles.icone}>
                <Ionicons
                  name={icones[item.tipo] || 'document-outline'}
                  size={28}
                  color={cores.azulEscuro}
                />
              </View>
              <View style={styles.flex}>
                <Text style={estilos.subtitulo}>{item.titulo}</Text>
                <Text style={estilos.legenda}>
                  {tipos[item.tipo] || item.tipo} • {formatarData(item.criadoEm)}
                </Text>
              </View>
            </View>
            {item.detalhe ? (
              <Text style={[estilos.legenda, estilos.espaco]}>{item.detalhe}</Text>
            ) : null}
            <Botao
              titulo={
                item.tipo === 'codigo'
                  ? 'Copiar código'
                  : item.tipo === 'link'
                    ? 'Abrir link'
                    : 'Baixar arquivo'
              }
              cor="azul"
              onPress={() => abrir(item)}
              carregando={ocupado === item.id}
              desabilitado={ocupado !== null}
              style={styles.espaco}
            />
            {/* A fixação só fica disponível para o professor. */}
            {!aluno ? (
              <Botao
                titulo={item.fixado ? 'Desfixar material' : 'Fixar material'}
                icone="pin-outline"
                pequeno
                cor="claro"
                onPress={() => fixar(item)}
                desabilitado={ocupado !== null}
              />
            ) : null}
          </Cartao>
        )}
      />
      {/* A janela separa os materiais do perfil e evita fechar durante uma publicação. */}
      <Modal
        visible={mostrarPerfil}
        animationType="slide"
        onRequestClose={() => {
          if (ocupado === null) setMostrarPerfil(false);
        }}
      >
        <Pagina rolar={false}>
          {/* Permite voltar à sala e escolher um material existente sem sair desta janela. */}
          <Cabecalho
            titulo="Materiais do meu perfil"
            onVoltar={ocupado === null ? () => setMostrarPerfil(false) : undefined}
          />
          <FlatList
            data={materiaisPerfil}
            keyExtractor={(item) => String(item.id)}
            contentContainerStyle={styles.conteudo}
            ListHeaderComponent={
              <>
                <Text style={[estilos.texto, estilos.espaco]}>
                  Escolha um conteúdo para compartilhar nesta sala.
                </Text>
                <Mensagem texto={erroPerfil} />
                {erroPerfil ? (
                  <Botao
                    titulo="Tentar novamente"
                    cor="claro"
                    onPress={abrirPerfil}
                    style={estilos.espaco}
                  />
                ) : null}
                {carregandoPerfil ? <ActivityIndicator color={cores.azul} /> : null}
              </>
            }
            ListEmptyComponent={
              !carregandoPerfil && !erroPerfil ? (
                <Text style={estilos.vazio}>Você ainda não tem materiais no perfil.</Text>
              ) : null
            }
            renderItem={({ item }) => {
              // Confere se o material já está na lista da sala para bloquear um envio repetido.
              const publicado = materiais.some((material) => material.id === item.id);
              return (
                <Cartao>
                  <Text style={estilos.subtitulo}>{item.titulo}</Text>
                  <Text style={[estilos.legenda, estilos.espaco]}>
                    {tipos[item.tipo]} • {formatarData(item.criadoEm)}
                  </Text>
                  <Botao
                    titulo={publicado ? 'Já está na sala' : 'Enviar para sala'}
                    pequeno
                    cor="verde"
                    desabilitado={publicado || ocupado !== null}
                    carregando={ocupado === item.id}
                    onPress={() => publicar(item)}
                  />
                </Cartao>
              );
            }}
          />
        </Pagina>
      </Modal>
    </Pagina>
  );
}

// StyleSheet.create reúne a aparência desta tela: organiza os cartões e usa roxo para os conteúdos fixados.
const styles = StyleSheet.create({
  conteudo: { padding: 24, paddingBottom: 36, flexGrow: 1 },
  linha: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 18 },
  flex: { flex: 1 },
  icone: { backgroundColor: cores.azulClaro, padding: 13, borderRadius: 19 },
  espaco: { marginBottom: 12 },
  fixado: { color: cores.roxoEscuro, marginBottom: 14 },
});
