import { useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api from '../services/api';
import type { Professor, Sala, AcessoProfessor, Material } from '../types/modelos';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

// Recebe o professor e seu acesso; a sala é opcional, e os callbacks devolvem a navegação ao App.
type Props = {
  professor: Professor;
  acesso: AcessoProfessor;
  sala?: Sala | null;
  onVoltar: () => void;
  onSalvo: () => void;
};

// Monta o formulário que salva um material no perfil ou compartilha diretamente com a sala.
export default function CriarMaterial({ professor, acesso, sala, onVoltar, onSalvo }: Props) {
  // useState mantém o que foi digitado entre renderizações; seus setters atualizam os campos na tela.
  const [titulo, setTitulo] = useState('');
  const [tipo, setTipo] = useState<'link' | 'codigo' | 'arquivo'>('link');
  const [conteudoOuUrl, setConteudoOuUrl] = useState('');
  const [detalhe, setDetalhe] = useState('');
  const [arquivo, setArquivo] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  // Guarda a mensagem de falha e acompanha separadamente a seleção e o envio do arquivo.
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [selecionando, setSelecionando] = useState(false);
  // Se publicar falhar, guardamos o material criado para tentar de novo sem duplicá-lo no perfil.
  const [materialCriado, setMaterialCriado] = useState<Material | null>(null);

  // Abre o seletor do aparelho ou navegador e guarda o arquivo completo exigido pelo serviço de envio.
  async function escolherArquivo() {
    setErro('');
    setSelecionando(true);
    try {
      const resultado = await DocumentPicker.getDocumentAsync({
        copyToCacheDirectory: true,
        multiple: false,
      });
      // Cancelar mantém a escolha anterior; arquivos acima do limite são recusados antes do envio.
      if (resultado.canceled) return;
      const asset = resultado.assets[0];
      if ((asset.size || (asset.file && asset.file.size) || 0) > 10 * 1024 * 1024) {
        setErro('Escolha um arquivo de até 10 MB.');
        return;
      }
      // Preserva também os dados do arquivo web, como lastModified, sem reconstruir o objeto.
      setArquivo(asset);
    } catch (e: unknown) {
      // Informa falha na seleção sem descartar os campos preenchidos.
      setErro(e instanceof Error ? e.message : 'Não foi possível selecionar o arquivo.');
    } finally {
      // Libera o seletor mesmo após cancelar ou recusar um arquivo.
      setSelecionando(false);
    }
  }

  // Salva conforme o tipo escolhido e só avisa o App depois de concluir todas as etapas.
  async function salvar() {
    if (salvando) return;
    setErro('');
    // Valida título, conteúdo e endereço antes da API; a repetição de publicação usa os dados já salvos.
    if (!materialCriado && !titulo.trim()) return setErro('Informe o título do material.');
    if (!materialCriado && tipo === 'arquivo' && !arquivo) return setErro('Selecione um arquivo.');
    if (!materialCriado && tipo !== 'arquivo' && !conteudoOuUrl.trim())
      return setErro('Preencha o conteúdo do material.');
    if (!materialCriado && tipo === 'link' && !/^https?:\/\/[^\s]+$/i.test(conteudoOuUrl.trim()))
      return setErro('Informe um link completo, começando com http:// ou https://.');
    setSalvando(true);
    try {
      // Arquivos seguem a rota de upload da sala ou do perfil; link e código são cadastrados como texto.
      if (tipo === 'arquivo') {
        if (!arquivo) return;
        if (sala) await api.enviarArquivoSala(sala.id, titulo.trim(), arquivo, acesso);
        else await api.enviarArquivoProfessor(professor.id, titulo.trim(), arquivo, acesso);
      } else {
        // Reaproveita o cadastro anterior se só faltou publicar na sala.
        let material = materialCriado;
        if (!material) {
          material = await api.criarMaterial(
            professor.id,
            {
              titulo: titulo.trim(),
              tipo,
              conteudoOuUrl: tipo === 'codigo' ? conteudoOuUrl : conteudoOuUrl.trim(),
              detalhe: detalhe.trim(),
            },
            acesso,
          );
          if (sala) setMaterialCriado(material);
        }
        if (sala) await api.publicarMaterial(sala.id, material.id, acesso);
      }
      // O App pode sair do formulário somente depois do cadastro e da publicação necessários.
      onSalvo();
    } catch (e: unknown) {
      // Mantém o formulário e mostra a falha para permitir uma nova tentativa.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera o botão após concluir ou falhar o envio.
      setSalvando(false);
    }
  }

  // Impede editar um conteúdo que está sendo enviado ou que já foi salvo e aguarda publicação.
  const bloqueado = salvando || !!materialCriado;
  return (
    <Pagina>
      {/* Identifica onde o material será salvo e oferece o retorno à tela anterior. */}
      <Cabecalho
        titulo="Criar material"
        subtitulo={sala ? sala.nomeAula : 'Seus materiais'}
        onVoltar={onVoltar}
      />
      <View style={styles.intro}>
        <Text style={estilos.titulo}>Compartilhe conhecimento</Text>
        <Text style={estilos.texto}>
          Adicione um link, um trecho de código ou um arquivo para estudar.
        </Text>
      </View>
      {/* Agrupa os dados do material; o tipo escolhido decide quais campos ficam visíveis. */}
      <Cartao>
        <Campo
          rotulo="TÍTULO"
          valor={titulo}
          onChangeText={setTitulo}
          maxLength={255}
          editable={!bloqueado}
          placeholder="Como se chama este material?"
        />
        <Text style={estilos.rotulo}>TIPO DE MATERIAL</Text>
        {/* map transforma cada opção do array em um botão para escolher o tipo. */}
        <View style={styles.opcoes}>
          {(
            [
              ['link', 'Link'],
              ['codigo', 'Código'],
              ['arquivo', 'Arquivo'],
            ] as const
          ).map(([valor, nome]) => (
            <Botao
              key={valor}
              titulo={nome}
              pequeno
              cor={tipo === valor ? 'azul' : 'claro'}
              desabilitado={bloqueado}
              onPress={() => setTipo(valor)}
            />
          ))}
        </View>
        {/* Arquivos mostram nome e tamanho; os outros tipos recebem conteúdo e detalhe opcional. */}
        {tipo === 'arquivo' ? (
          <View style={styles.arquivo}>
            <Text style={estilos.texto}>
              {arquivo ? arquivo.name : 'Nenhum arquivo selecionado'}
            </Text>
            <Text style={estilos.legenda}>
              Tamanho máximo: 10 MB
              {arquivo && arquivo.size
                ? ' • ' + (arquivo.size / 1024 / 1024).toFixed(2) + ' MB'
                : ''}
            </Text>
            <Botao
              titulo="Escolher arquivo"
              icone="attach-outline"
              cor="claro"
              onPress={escolherArquivo}
              carregando={selecionando}
              desabilitado={bloqueado}
            />
          </View>
        ) : (
          <>
            <Campo
              rotulo={tipo === 'link' ? 'ENDEREÇO DO LINK' : 'CÓDIGO'}
              valor={conteudoOuUrl}
              onChangeText={setConteudoOuUrl}
              multiline={tipo === 'codigo'}
              autoCapitalize="none"
              autoCorrect={false}
              editable={!bloqueado}
              placeholder={tipo === 'link' ? 'https://...' : 'Cole seu exemplo aqui'}
            />
            <Campo
              rotulo="DETALHE (OPCIONAL)"
              valor={detalhe}
              onChangeText={setDetalhe}
              maxLength={255}
              editable={!bloqueado}
              placeholder="Uma breve descrição"
            />
          </>
        )}
      </Cartao>
      {/* Informa quando o cadastro já existe e resta apenas concluir a publicação na sala. */}
      {materialCriado ? (
        <Text style={[estilos.legenda, estilos.espaco]}>
          Material salvo no perfil. Tente novamente para concluir o envio à sala.
        </Text>
      ) : null}
      {/* Mantém o erro perto do botão que salva ou repete a publicação. */}
      <Mensagem texto={erro} />
      <Botao
        titulo={
          materialCriado
            ? 'Tentar enviar para sala'
            : sala
              ? 'Salvar e enviar para sala'
              : 'Salvar material'
        }
        cor="laranja"
        icone="add-outline"
        onPress={salvar}
        carregando={salvando}
        desabilitado={selecionando}
      />
      <Marca />
    </Pagina>
  );
}

// StyleSheet.create reúne a aparência desta tela: espaça o formulário e destaca a área do arquivo.
const styles = StyleSheet.create({
  intro: { gap: 8, paddingVertical: 24 },
  opcoes: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginVertical: 14 },
  arquivo: { padding: 16, borderRadius: 18, backgroundColor: cores.azulClaro, gap: 12 },
});
