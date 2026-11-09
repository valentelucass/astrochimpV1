import { useState } from 'react'; // Importa useState do React para guardar o quiz, suas perguntas e o resultado do envio.
import { View, Text, Switch, StyleSheet } from 'react-native'; // Importa os elementos do React Native usados para montar campos, opções, textos e estilos do quiz.
import Pagina from '../components/Pagina'; // Importa Pagina para aplicar a estrutura e a rolagem padrão da tela.
import Cabecalho from '../components/Cabecalho'; // Importa Cabecalho para mostrar o título e a ação de voltar.
import Cartao from '../components/Cartao'; // Importa Cartao para separar os dados gerais e cada pergunta do quiz.
import Campo from '../components/Campo'; // Importa Campo para receber título, enunciados e alternativas.
import Botao from '../components/Botao'; // Importa Botao para adicionar perguntas e salvar o quiz.
import Mensagem from '../components/Mensagem'; // Importa Mensagem para informar erros ou confirmar a criação.
import api from '../services/api'; // Importa o serviço da API para enviar o quiz completo ao backend.
import type { Professor, Sala, AcessoProfessor } from '../types/modelos'; // Importa somente os tipos que descrevem o professor, a sala e seu acesso.
import cores from '../styles/cores'; // Importa as cores do projeto para destacar alternativas e opções do quiz.
import estilos from '../styles/estilos'; // Importa os estilos compartilhados para manter textos e espaços no padrão visual.

// O tempo permanece texto durante a digitação; a conversão para número acontece ao preparar o pedido.
type AlternativaFormulario = { texto: string; correta: boolean };
type PerguntaFormulario = {
  enunciado: string;
  dificuldade: string;
  tempoLimiteSegundos: string;
  alternativas: AlternativaFormulario[];
};

// Cria um objeto novo para cada pergunta, com duas alternativas ainda sem gabarito.
function novaPergunta(): PerguntaFormulario {
  return {
    enunciado: '',
    dificuldade: 'Fácil',
    tempoLimiteSegundos: '30',
    alternativas: [
      { texto: '', correta: false },
      { texto: '', correta: false },
    ],
  };
}

// Recebe o dono do quiz, seu acesso e a sala opcional; o App cuida de voltar e do destino após salvar.
type Props = {
  professor: Professor;
  acesso: AcessoProfessor;
  sala?: Sala | null;
  onVoltar: () => void;
  onSalvo: () => void;
};

// Reúne perguntas e alternativas em um quiz, com envio imediato quando há uma sala aberta.
export default function CriarQuiz({ professor, acesso, sala, onVoltar, onSalvo }: Props) {
  // useState conserva as escolhas do formulário e seus setters fazem a tela mostrar cada alteração.
  const [titulo, setTitulo] = useState('');
  const [salvarNoPerfil, setSalvarNoPerfil] = useState(true);
  // O formulário guarda perguntas e alternativas em arrays; apenas a API define seus ids.
  const [perguntas, setPerguntas] = useState<PerguntaFormulario[]>([novaPergunta()]);
  // A mensagem orienta a correção dos campos e salvando impede mudanças durante o pedido.
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);

  // Copia os dados de cada pergunta para não alterar diretamente o estado que o React está exibindo.
  function copiarPerguntas() {
    const copia: PerguntaFormulario[] = [];
    for (const pergunta of perguntas) {
      copia.push({
        enunciado: pergunta.enunciado,
        dificuldade: pergunta.dificuldade,
        tempoLimiteSegundos: pergunta.tempoLimiteSegundos,
        alternativas: pergunta.alternativas,
      });
    }
    return copia;
  }

  // Altera somente um dos três campos de texto da pergunta escolhida.
  function alterarPergunta(
    indice: number,
    campo: 'enunciado' | 'dificuldade' | 'tempoLimiteSegundos',
    valor: string,
  ) {
    const copia = copiarPerguntas();
    if (campo === 'enunciado') copia[indice].enunciado = valor;
    else if (campo === 'dificuldade') copia[indice].dificuldade = valor;
    else copia[indice].tempoLimiteSegundos = valor;
    setPerguntas(copia);
  }

  // Substitui as alternativas de uma pergunta usando outra lista, sem modificar a lista anterior.
  function alterarAlternativas(indice: number, alternativas: AlternativaFormulario[]) {
    const copia = copiarPerguntas();
    copia[indice].alternativas = alternativas;
    setPerguntas(copia);
  }
  // Acrescenta uma pergunta independente ao final do formulário.
  function adicionarPergunta() {
    const copia = copiarPerguntas();
    copia.push(novaPergunta());
    setPerguntas(copia);
  }
  // Retira a pergunta escolhida; o botão impede excluir a única que restou.
  function removerPergunta(indice: number) {
    setPerguntas(perguntas.filter((pergunta, i) => i !== indice));
  }
  // Acrescenta uma opção vazia à pergunta; o botão limita a seis alternativas.
  function adicionarAlternativa(indice: number) {
    const alternativas = perguntas[indice].alternativas.slice();
    alternativas.push({ texto: '', correta: false });
    alterarAlternativas(indice, alternativas);
  }
  // Retira a opção indicada; a tela mantém pelo menos duas em cada pergunta.
  function removerAlternativa(indice: number, alternativaIndice: number) {
    alterarAlternativas(
      indice,
      perguntas[indice].alternativas.filter((alternativa, i) => i !== alternativaIndice),
    );
  }
  // Atualiza o texto de uma alternativa sem mudar a marcação da resposta correta.
  function alterarAlternativa(indice: number, alternativaIndice: number, texto: string) {
    // map cria outra lista: troca o texto da opção escolhida e mantém as demais.
    alterarAlternativas(
      indice,
      perguntas[indice].alternativas.map((alternativa, i) =>
        i === alternativaIndice ? { texto, correta: alternativa.correta } : alternativa,
      ),
    );
  }
  // Marca a opção escolhida e desmarca as outras alternativas da mesma pergunta.
  function marcarCorreta(indice: number, alternativaIndice: number) {
    alterarAlternativas(
      indice,
      perguntas[indice].alternativas.map((alternativa, i) => ({
        texto: alternativa.texto,
        correta: i === alternativaIndice,
      })),
    );
  }

  // Confere o formulário inteiro antes de criar o quiz no perfil ou iniciar seu envio para a sala.
  async function salvar() {
    if (salvando) return;
    setErro('');
    if (!titulo.trim()) return setErro('Informe o título do quiz.');
    if (!perguntas.length) return setErro('Adicione pelo menos uma pergunta.');
    // Confere cada enunciado, o tempo aceito pelo Spring e a quantidade de opções preenchidas.
    for (let i = 0; i < perguntas.length; i++) {
      const pergunta = perguntas[i];
      const tempo = Number(pergunta.tempoLimiteSegundos);
      if (!pergunta.enunciado.trim())
        return setErro('Preencha o enunciado da pergunta ' + (i + 1) + '.');
      if (!Number.isInteger(tempo) || tempo <= 0 || tempo > 2147483647)
        return setErro('Informe um tempo inteiro maior que zero na pergunta ' + (i + 1) + '.');
      if (pergunta.alternativas.length < 2 || pergunta.alternativas.length > 6)
        return setErro('Cada pergunta precisa de 2 a 6 alternativas.');
      // Conta o gabarito para recusar perguntas sem resposta correta ou com mais de uma.
      let corretas = 0;
      for (const alternativa of pergunta.alternativas) {
        if (!alternativa.texto.trim())
          return setErro('Preencha todas as alternativas da pergunta ' + (i + 1) + '.');
        if (alternativa.correta) corretas++;
      }
      if (corretas !== 1)
        return setErro('Marque uma resposta correta na pergunta ' + (i + 1) + '.');
    }
    // Remove espaços extras, converte o tempo e garante que um quiz fora da sala fique salvo no perfil.
    const dados = {
      titulo: titulo.trim(),
      salvoNoPerfil: !sala || salvarNoPerfil,
      perguntas: perguntas.map((pergunta) => ({
        dificuldade: pergunta.dificuldade,
        enunciado: pergunta.enunciado.trim(),
        tempoLimiteSegundos: Number(pergunta.tempoLimiteSegundos),
        alternativas: pergunta.alternativas.map((alternativa) => ({
          correta: alternativa.correta,
          texto: alternativa.texto.trim(),
        })),
      })),
    };
    setSalvando(true);
    try {
      // A sala define a rota; o callback só é chamado depois da resposta de sucesso.
      if (sala) await api.criarEEnviarQuiz(sala.id, professor.id, dados, acesso);
      else await api.criarQuiz(professor.id, dados, acesso);
      onSalvo();
    } catch (e: unknown) {
      // Mantém as perguntas preenchidas e informa por que o cadastro não terminou.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera a edição depois da resposta ou de uma falha.
      setSalvando(false);
    }
  }

  return (
    <Pagina>
      {/* Mostra o destino do quiz e permite voltar pelo App. */}
      <Cabecalho
        titulo="Criar quiz"
        subtitulo={sala ? sala.nomeAula : 'Sua biblioteca de quizzes'}
        onVoltar={onVoltar}
      />
      <View style={styles.intro}>
        <Text style={estilos.titulo}>Hora do desafio!</Text>
        <Text style={estilos.texto}>
          Crie perguntas e escolha uma resposta correta para cada uma.
        </Text>
      </View>
      {/* Define o título e, dentro da sala, permite escolher se o quiz também ficará no perfil. */}
      <Cartao>
        <Campo
          rotulo="TÍTULO DO QUIZ"
          valor={titulo}
          onChangeText={setTitulo}
          maxLength={255}
          editable={!salvando}
          placeholder="O que vamos aprender?"
        />
        {sala ? (
          <View style={styles.linha}>
            <Text style={[estilos.texto, styles.flex]}>Salvar no meu perfil</Text>
            <Switch
              accessibilityLabel="Salvar quiz no meu perfil"
              value={salvarNoPerfil}
              onValueChange={setSalvarNoPerfil}
              disabled={salvando}
              trackColor={{ true: cores.verde, false: cores.cinza }}
            />
          </View>
        ) : null}
        <Text style={estilos.legenda}>
          {sala
            ? 'Guarde este quiz para usar novamente em outras aulas.'
            : 'Este quiz será salvo no seu perfil para usar nas próximas aulas.'}
        </Text>
      </Cartao>
      {/* Cada pergunta ganha seu cartão com enunciado, dificuldade, tempo e alternativas. */}
      {perguntas.map((pergunta, indice) => (
        <Cartao key={indice} corDaBorda={cores.borda}>
          <View style={[styles.linha, estilos.espaco]}>
            <Text style={[estilos.subtitulo, styles.flex]}>Pergunta {indice + 1}</Text>
            <Botao
              titulo="Remover"
              cor="claro"
              pequeno
              onPress={() => removerPergunta(indice)}
              desabilitado={salvando || perguntas.length === 1}
            />
          </View>
          <Campo
            rotulo="ENUNCIADO"
            valor={pergunta.enunciado}
            onChangeText={(valor) => alterarPergunta(indice, 'enunciado', valor)}
            multiline
            maxLength={255}
            editable={!salvando}
            placeholder="Escreva sua pergunta"
          />
          <Text style={estilos.rotulo}>DIFICULDADE</Text>
          {/* As opções de dificuldade mudam apenas a pergunta deste cartão. */}
          <View style={styles.opcoes}>
            {['Fácil', 'Média', 'Difícil'].map((dificuldade) => (
              <Botao
                key={dificuldade}
                titulo={dificuldade}
                pequeno
                cor={pergunta.dificuldade === dificuldade ? 'roxo' : 'claro'}
                onPress={() => alterarPergunta(indice, 'dificuldade', dificuldade)}
                desabilitado={salvando}
              />
            ))}
          </View>
          <Campo
            rotulo="TEMPO PARA RESPONDER (SEGUNDOS)"
            valor={pergunta.tempoLimiteSegundos}
            onChangeText={(valor) => alterarPergunta(indice, 'tempoLimiteSegundos', valor)}
            keyboardType="number-pad"
            maxLength={10}
            editable={!salvando}
          />
          <Text style={[estilos.rotulo, estilos.espaco]}>ALTERNATIVAS • MARQUE A CORRETA</Text>
          {/* Exibe as opções da pergunta e os controles para marcar o gabarito ou remover uma delas. */}
          {pergunta.alternativas.map((alternativa, i) => (
            <View key={i} style={[styles.alternativa, alternativa.correta && styles.correta]}>
              <Campo
                rotulo={'ALTERNATIVA ' + 'ABCDEF'[i]}
                valor={alternativa.texto}
                onChangeText={(valor) => alterarAlternativa(indice, i, valor)}
                maxLength={255}
                editable={!salvando}
              />
              <View style={styles.opcoes}>
                <Botao
                  titulo={alternativa.correta ? 'Correta' : 'Marcar correta'}
                  icone={alternativa.correta ? 'checkmark-circle' : 'ellipse-outline'}
                  pequeno
                  cor={alternativa.correta ? 'verde' : 'claro'}
                  desabilitado={salvando}
                  onPress={() => marcarCorreta(indice, i)}
                />
                <Botao
                  titulo="Remover"
                  pequeno
                  cor="claro"
                  desabilitado={salvando || pergunta.alternativas.length <= 2}
                  onPress={() => removerAlternativa(indice, i)}
                />
              </View>
            </View>
          ))}
          <Botao
            titulo="Adicionar alternativa"
            pequeno
            cor="claro"
            icone="add"
            desabilitado={salvando || pergunta.alternativas.length >= 6}
            onPress={() => adicionarAlternativa(indice)}
          />
        </Cartao>
      ))}
      <Botao
        titulo="Adicionar pergunta"
        cor="claro"
        icone="add-circle-outline"
        onPress={adicionarPergunta}
        desabilitado={salvando}
        style={estilos.espaco}
      />
      {/* O aviso indica o campo pendente antes da tentativa de salvar o desafio. */}
      <Mensagem texto={erro} />
      <Botao
        titulo={sala ? 'Criar e enviar quiz' : 'Salvar quiz'}
        cor="laranja"
        icone="rocket-outline"
        onPress={salvar}
        carregando={salvando}
      />
    </Pagina>
  );
}

// StyleSheet.create reúne a aparência desta tela: separa perguntas e destaca em verde a resposta correta.
const styles = StyleSheet.create({
  intro: { paddingVertical: 24, gap: 8 },
  linha: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  flex: { flex: 1 },
  opcoes: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginVertical: 12 },
  alternativa: {
    backgroundColor: cores.azulClaro,
    borderRadius: 18,
    padding: 12,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: cores.borda,
  },
  correta: { backgroundColor: cores.verdeClaro, borderColor: cores.verde },
});
