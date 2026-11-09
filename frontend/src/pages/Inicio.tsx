import { useState } from 'react'; // Importa useState do React para guardar o código da sala e as mensagens de entrada.
import { Image, Text, TextInput, StyleSheet } from 'react-native'; // Importa os elementos do React Native usados para mostrar a imagem, os textos, a entrada e os estilos iniciais.
import Pagina from '../components/Pagina'; // Importa Pagina para aplicar a estrutura e a rolagem padrão da tela.
import Cartao from '../components/Cartao'; // Importa Cartao para agrupar as opções de aluno e professor.
import Campo from '../components/Campo'; // Importa Campo para receber o nome do aluno.
import Botao from '../components/Botao'; // Importa Botao para entrar na sala ou abrir o acesso do professor.
import Mensagem from '../components/Mensagem'; // Importa Mensagem para informar falhas ao procurar a sala.
import Marca from '../components/Marca'; // Importa Marca para mostrar a identidade do AstroChimp no fim da tela.
import api from '../services/api'; // Importa o serviço da API para localizar a sala e registrar a entrada do aluno.
import { codigoSalaValido, limparCodigoSala } from '../services/textos'; // Confere o código da sala e mantém somente as letras e os números permitidos.
import cores from '../styles/cores'; // Importa as cores do projeto para estilizar a entrada do código e os destaques da tela.
import estilos from '../styles/estilos'; // Importa os estilos compartilhados para manter textos e espaços no padrão visual.
import type { EntradaSala } from '../types/modelos'; // Importa somente o tipo que descreve os dados devolvidos quando o aluno entra na sala.

// Recebe do App os caminhos de entrada do aluno e de abertura do acesso do professor.
type Props = {
  onEntrarAluno: (entrada: EntradaSala & { nomeAluno: string }) => void;
  onEntrarProfessor: () => void;
};

// Apresenta a missão e identifica o aluno na sala informada pelo professor.
export default function Inicio({ onEntrarAluno, onEntrarProfessor }: Props) {
  // useState guarda o que muda na tela; aqui mantém o código e o nome digitados.
  const [codigo, setCodigo] = useState('');
  const [nomeAluno, setNomeAluno] = useState('');
  // Controla a espera da entrada e a mensagem de falha sem apagar o formulário.
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState('');

  // Envia a identificação ao Spring e entrega a sessão recebida ao App.
  async function entrar() {
    // Evita repetir o pedido e exige seis letras ou números, além de um nome preenchido.
    if (carregando) return;
    if (!codigoSalaValido(codigo) || !nomeAluno.trim()) {
      setErro('Informe seu nome e o código da sala com 6 letras ou números.');
      return;
    }
    // Retira o erro anterior e bloqueia os campos enquanto aguarda a resposta.
    setErro('');
    setCarregando(true);
    try {
      // O servidor confere a sala e devolve o token usado nas próximas ações do aluno.
      const entrada = await api.entrarSala(codigo, nomeAluno.trim());
      // App guarda a sessão somente em memória e escolhe a próxima tela.
      onEntrarAluno({
        sala: entrada.sala,
        alunoId: entrada.alunoId,
        tokenAluno: entrada.tokenAluno,
        nomeAluno: nomeAluno.trim(),
      });
    } catch (falha) {
      // Aproveita a mensagem do servidor quando a falha é um Error conhecido.
      setErro(falha instanceof Error ? falha.message : 'Não foi possível entrar na sala.');
    } finally {
      // Libera uma nova tentativa mesmo quando a conexão falha.
      setCarregando(false);
    }
  }

  return (
    <Pagina>
      {/* O mascote e o convite apresentam a entrada antes dos campos da missão. */}
      <Image source={require('../../assets/mascote.png')} style={styles.mascote} />
      <Text style={[estilos.titulo, styles.centro]}>Pronto para a decolagem?</Text>
      <Text style={[estilos.texto, styles.descricao]}>
        Insira seu código de missão de 6 caracteres para se juntar aos seus colegas!
      </Text>
      <Cartao style={styles.cartao}>
        {/* A digitação transforma o código em maiúsculas e aceita só letras e números. */}
        <Text style={[estilos.rotulo, styles.centro]}>✨ CÓDIGO DA SALA ✨</Text>
        <TextInput
          accessibilityLabel="Código da sala"
          value={codigo}
          onChangeText={(texto) => setCodigo(limparCodigoSala(texto))}
          maxLength={6}
          autoCapitalize="characters"
          autoCorrect={false}
          placeholder="••••••"
          placeholderTextColor={cores.sombra}
          style={styles.codigo}
          editable={!carregando}
        />
        <Text style={[estilos.legenda, styles.ajuda]}>
          Peça para o seu professor o código da sala!
        </Text>
        {/* O nome acompanha o código porque o backend precisa identificar quem entra. */}
        <Campo
          rotulo="SEU NOME"
          valor={nomeAluno}
          onChangeText={setNomeAluno}
          placeholder="Como você se chama?"
          editable={!carregando}
          maxLength={100}
        />
        {/* O erro fica junto do botão para o aluno corrigir e tentar novamente. */}
        <Mensagem texto={erro} />
        <Botao
          titulo="ENTRAR NA SALA"
          icone="rocket-outline"
          onPress={entrar}
          carregando={carregando}
        />
      </Cartao>
      {/* O acesso do professor pede ao App para trocar a tela, sem entrar como aluno. */}
      <Botao
        titulo="ENTRAR COMO PROFESSOR"
        cor="claro"
        icone="school-outline"
        onPress={onEntrarProfessor}
        desabilitado={carregando}
      />
      <Marca />
    </Pagina>
  );
}

// StyleSheet reúne a aparência da tela: mascote centralizado e código grande dentro do cartão.
const styles = StyleSheet.create({
  mascote: {
    width: 190,
    height: 195,
    resizeMode: 'contain',
    alignSelf: 'center',
    marginTop: 20,
    marginBottom: 24,
  },
  centro: { textAlign: 'center' },
  descricao: { textAlign: 'center', color: cores.textoSuave, marginTop: 12, marginBottom: 32 },
  cartao: { paddingVertical: 28, borderRadius: 34 },
  codigo: {
    marginTop: 22,
    borderWidth: 3,
    borderColor: cores.borda,
    borderBottomWidth: 6,
    borderRadius: 20,
    backgroundColor: cores.azulClaro,
    padding: 16,
    textAlign: 'center',
    fontFamily: 'Nunito_800ExtraBold',
    fontSize: 32,
    letterSpacing: 4,
    color: cores.azulEscuro,
  },
  ajuda: { textAlign: 'center', marginTop: 12, marginBottom: 24 },
});
