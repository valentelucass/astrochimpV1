import { useState } from 'react';
import { Image, Text, TextInput, StyleSheet } from 'react-native';
import Pagina from '../components/Pagina';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api from '../services/api';
import cores from '../styles/cores';
import estilos from '../styles/estilos';
import type { EntradaSala } from '../types/modelos';

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
    if (!/^[A-Z0-9]{6}$/.test(codigo) || !nomeAluno.trim()) {
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
      onEntrarAluno({ ...entrada, nomeAluno: nomeAluno.trim() });
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
          onChangeText={(texto) =>
            setCodigo(
              texto
                .toUpperCase()
                .replace(/[^A-Z0-9]/g, '')
                .slice(0, 6),
            )
          }
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
