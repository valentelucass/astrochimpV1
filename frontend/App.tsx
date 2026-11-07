import { useEffect, useState } from 'react'; // Importa os hooks do React para guardar a tela e a sessão e executar ações quando esses dados mudam.
import { ActivityIndicator, BackHandler, Platform, Text, View } from 'react-native'; // Importa os elementos do React Native usados na espera inicial, no botão voltar e na estrutura visual do App.
import { StatusBar } from 'expo-status-bar'; // Importa a barra de status do Expo para ajustar sua aparência ao visual do aplicativo.
import { SafeAreaProvider, initialWindowMetrics } from 'react-native-safe-area-context'; // Importa o contexto e as medidas iniciais que informam às páginas o espaço ocupado pelas barras do dispositivo.
import { useFonts } from 'expo-font'; // Importa o hook de fontes do Expo para carregar a família Nunito antes de mostrar as telas.
import { Nunito_400Regular } from '@expo-google-fonts/nunito/400Regular'; // Importa o peso regular da Nunito para os textos comuns do aplicativo.
import { Nunito_700Bold } from '@expo-google-fonts/nunito/700Bold'; // Importa o peso negrito da Nunito para rótulos e textos de destaque.
import { Nunito_800ExtraBold } from '@expo-google-fonts/nunito/800ExtraBold'; // Importa o peso extra negrito da Nunito para títulos e elementos de maior destaque.
import Inicio from './src/pages/Inicio'; // Importa a tela inicial para permitir que o App a mostre como primeiro caminho.
import LoginProfessor from './src/pages/LoginProfessor'; // Importa a tela de login para permitir a entrada do professor.
import CadastroProfessor from './src/pages/CadastroProfessor'; // Importa a tela de cadastro para criar uma nova conta de professor.
import PainelProfessor from './src/pages/PainelProfessor'; // Importa o painel para mostrar ao professor suas salas e ações principais.
import PerfilProfessor from './src/pages/PerfilProfessor'; // Importa a tela de perfil para o professor consultar e alterar seus dados.
import PerfilPublico from './src/pages/PerfilPublico'; // Importa o perfil público para mostrar os materiais visíveis de um professor.
import SalaProfessor from './src/pages/SalaProfessor'; // Importa a sala do professor para acompanhar e controlar uma aula.
import SalaAluno from './src/pages/SalaAluno'; // Importa a sala do aluno para participar da aula e usar seus recursos.
import QuizAluno from './src/pages/QuizAluno'; // Importa a tela do quiz para o aluno responder às perguntas liberadas.
import MateriaisSala from './src/pages/MateriaisSala'; // Importa a tela de materiais para consultar os conteúdos publicados na sala.
import CriarMaterial from './src/pages/CriarMaterial'; // Importa a tela de criação de material para o professor publicar um conteúdo.
import CriarQuiz from './src/pages/CriarQuiz'; // Importa a tela de criação de quiz para o professor cadastrar perguntas e respostas.
import EscolherQuiz from './src/pages/EscolherQuiz'; // Importa a tela de escolha de quiz para o professor selecionar uma atividade da sala.
import cores from './src/styles/cores'; // Importa as cores do projeto para estilizar a espera e as mensagens do componente principal.
import type { AcessoProfessor, Aluno, EntradaSala, Professor, Sala } from './src/types/modelos'; // Importa somente os tipos dos dados que o App guarda e entrega às diferentes telas.

// O TypeScript só aceita estes nomes ao trocar a página; isso evita escrever um nome de tela errado.
type Tela =
  | 'Inicio'
  | 'Login'
  | 'Cadastro'
  | 'Painel'
  | 'Perfil'
  | 'PerfilPublico'
  | 'SalaProfessor'
  | 'SalaAluno'
  | 'QuizAluno'
  | 'MateriaisSala'
  | 'CriarMaterial'
  | 'CriarQuiz'
  | 'EscolherQuiz';

// Este componente começa o aplicativo e passa os dados da sessão para a página escolhida.
export default function App() {
  // useState guarda um valor entre as renderizações. tela começa em Inicio e setTela escolhe outra página.
  const [tela, setTela] = useState<Tela>('Inicio');
  // null significa que ainda não há pessoa conectada nem sala escolhida.
  // O perfil identifica o professor; acesso guarda o e-mail e a senha usados nos pedidos ao Spring.
  const [professor, setProfessor] = useState<Professor | null>(null);
  const [acesso, setAcesso] = useState<AcessoProfessor | null>(null);
  const [aluno, setAluno] = useState<Aluno | null>(null);
  const [sala, setSala] = useState<Sala | null>(null);
  // Esta opção decide se o conteúdo novo será enviado à sala atual ou guardado só no perfil.
  const [criacaoNaSala, setCriacaoNaSala] = useState(false);
  const [abaPerfil, setAbaPerfil] = useState<'materiais' | 'quizzes'>('materiais');
  // Carrega as três espessuras da fonte Nunito antes de desenhar os textos das telas.
  const [fontesCarregadas, erroFontes] = useFonts({
    Nunito_400Regular,
    Nunito_700Bold,
    Nunito_800ExtraBold,
  });

  function entrarProfessor(perfil: Professor, login: AcessoProfessor) {
    // A senha só fica na memória desta sessão para autenticar os pedidos ao Spring.
    setProfessor(perfil);
    setAcesso(login);
    // Encerra uma possível sessão de aluno antes de abrir o painel do professor autenticado.
    setAluno(null);
    setSala(null);
    setTela('Painel');
  }

  // A entrada da API traz a sala, o id do aluno e o token usado para enviar dúvidas e respostas.
  function entrarAluno(entrada: EntradaSala & { nomeAluno: string }) {
    setSala(entrada.sala);
    setAluno({ id: entrada.alunoId, tokenAluno: entrada.tokenAluno, nomeAluno: entrada.nomeAluno });
    setProfessor(null);
    setAcesso(null);
    setTela('SalaAluno');
  }

  // Apaga os dados da sessão da memória e exige uma nova entrada para continuar usando a sala.
  function sair() {
    setProfessor(null);
    setAcesso(null);
    setAluno(null);
    setSala(null);
    setTela('Inicio');
  }

  // Usa os dados da sala criada ou escolhida no painel para abrir seus controles.
  function abrirSala(dados: Sala) {
    setSala(dados);
    setTela('SalaProfessor');
  }

  // O mesmo formulário de quiz atende a criação pelo perfil e a criação durante uma aula.
  function criarQuiz(naSala: boolean) {
    setCriacaoNaSala(naSala);
    setTela('CriarQuiz');
  }

  // Guarda de onde veio o pedido de material para devolver a pessoa à biblioteca certa ao salvar.
  function criarMaterial(naSala: boolean) {
    setCriacaoNaSala(naSala);
    setTela('CriarMaterial');
  }

  // Cada página tem um destino de volta. true avisa ao Android que já tratamos o botão voltar.
  // Na entrada devolvemos false, deixando o próprio aparelho cuidar desse botão.
  function voltar() {
    if (tela === 'Inicio') return false;
    if (tela === 'Login') setTela('Inicio');
    else if (tela === 'Cadastro') setTela('Login');
    else if (tela === 'Painel' || tela === 'SalaAluno') sair();
    else if (tela === 'SalaProfessor' || tela === 'Perfil') setTela('Painel');
    else if (tela === 'PerfilPublico') setTela('Perfil');
    else if (tela === 'QuizAluno') setTela('SalaAluno');
    else if (tela === 'MateriaisSala') setTela(aluno ? 'SalaAluno' : 'SalaProfessor');
    else if (tela === 'EscolherQuiz') setTela('SalaProfessor');
    else if (tela === 'CriarQuiz') setTela(criacaoNaSala ? 'EscolherQuiz' : 'Perfil');
    else if (tela === 'CriarMaterial') setTela(criacaoNaSala ? 'MateriaisSala' : 'Perfil');
    return true;
  }

  // useEffect registra o botão físico de voltar e refaz o registro quando seu destino muda.
  useEffect(() => {
    if (Platform.OS === 'web') return;
    const evento = BackHandler.addEventListener('hardwareBackPress', voltar);
    // Remove o registro anterior para um único toque não executar dois caminhos de volta.
    return () => evento.remove();
  }, [tela, aluno, criacaoNaSala]);

  // Durante a leitura das fontes mostramos uma espera; se ela falhar, orientamos reabrir o app.
  if (!fontesCarregadas)
    return (
      <View
        style={{
          flex: 1,
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: cores.fundo,
        }}
      >
        {erroFontes ? (
          <Text>Não foi possível carregar as fontes. Reabra o aplicativo.</Text>
        ) : (
          <ActivityIndicator color={cores.azul} />
        )}
      </View>
    );

  // As funções passadas nas props permitem que os botões das páginas peçam a troca de tela ao App.
  let pagina;
  // Login e cadastro podem ser abertos antes de existir uma sessão de professor.
  if (tela === 'Login')
    pagina = (
      <LoginProfessor
        onEntrar={entrarProfessor}
        onCadastrar={() => setTela('Cadastro')}
        onVoltar={voltar}
      />
    );
  else if (tela === 'Cadastro')
    pagina = <CadastroProfessor onEntrar={entrarProfessor} onVoltar={voltar} />;
  // O painel só recebe um perfil e um login já confirmados pela API.
  else if (tela === 'Painel' && professor && acesso)
    pagina = (
      <PainelProfessor
        professor={professor}
        acesso={acesso}
        onCriarSala={abrirSala}
        onAbrirSala={abrirSala}
        onPerfil={() => {
          setAbaPerfil('materiais');
          setTela('Perfil');
        }}
        onCriarQuiz={() => criarQuiz(false)}
        onPerfilPublico={() => setTela('PerfilPublico')}
        onSair={sair}
      />
    );
  // O perfil altera os próprios conteúdos; uma edição devolve os dados novos a setProfessor.
  else if (tela === 'Perfil' && professor && acesso)
    pagina = (
      <PerfilProfessor
        professor={professor}
        acesso={acesso}
        abaInicial={abaPerfil}
        onVoltar={voltar}
        onCriarMaterial={() => criarMaterial(false)}
        onCriarQuiz={() => criarQuiz(false)}
        onPerfilPublico={() => setTela('PerfilPublico')}
        onAtualizarProfessor={setProfessor}
      />
    );
  // slugPerfilPublico é o nome do endereço público, como professora-ana.
  else if (tela === 'PerfilPublico' && professor)
    pagina = <PerfilPublico slug={professor.slugPerfilPublico} onVoltar={voltar} />;
  // Os controles recebem a sala escolhida. Ao encerrar a aula, o painel volta a ser mostrado.
  else if (tela === 'SalaProfessor' && sala && professor && acesso)
    pagina = (
      <SalaProfessor
        sala={sala}
        professor={professor}
        acesso={acesso}
        onMateriais={() => setTela('MateriaisSala')}
        onQuizzes={() => setTela('EscolherQuiz')}
        onVoltar={voltar}
        onEncerrar={() => {
          setSala(null);
          setTela('Painel');
        }}
      />
    );
  // O aluno navega entre o feed, o quiz e os materiais usando a mesma participação na sala.
  else if (tela === 'SalaAluno' && sala && aluno)
    pagina = (
      <SalaAluno
        sala={sala}
        aluno={aluno}
        onQuiz={() => setTela('QuizAluno')}
        onMateriais={() => setTela('MateriaisSala')}
        onSair={sair}
      />
    );
  else if (tela === 'QuizAluno' && sala && aluno)
    pagina = <QuizAluno sala={sala} aluno={aluno} onVoltar={voltar} />;
  // A biblioteca atende os dois papéis; só o professor recebe a ação de criar material.
  else if (tela === 'MateriaisSala' && sala)
    pagina = (
      <MateriaisSala
        sala={sala}
        professor={professor}
        acesso={acesso}
        aluno={aluno}
        onVoltar={voltar}
        onCriarMaterial={professor ? () => criarMaterial(true) : undefined}
      />
    );
  // sala=null faz o formulário salvar só no perfil. Depois de salvar, abrimos a lista de materiais.
  else if (tela === 'CriarMaterial' && professor && acesso)
    pagina = (
      <CriarMaterial
        professor={professor}
        acesso={acesso}
        sala={criacaoNaSala ? sala : null}
        onVoltar={voltar}
        onSalvo={() => {
          setAbaPerfil('materiais');
          setTela(criacaoNaSala ? 'MateriaisSala' : 'Perfil');
        }}
      />
    );
  // Um quiz enviado retorna aos controles da aula; um quiz guardado retorna à aba de quizzes.
  else if (tela === 'CriarQuiz' && professor && acesso)
    pagina = (
      <CriarQuiz
        professor={professor}
        acesso={acesso}
        sala={criacaoNaSala ? sala : null}
        onVoltar={voltar}
        onSalvo={() => {
          setAbaPerfil('quizzes');
          setTela(criacaoNaSala ? 'SalaProfessor' : 'Perfil');
        }}
      />
    );
  // Aqui o professor escolhe um quiz pronto ou abre o formulário de um novo para esta sala.
  else if (tela === 'EscolherQuiz' && sala && professor && acesso)
    pagina = (
      <EscolherQuiz
        professor={professor}
        acesso={acesso}
        sala={sala}
        onVoltar={voltar}
        onCriarQuiz={() => criarQuiz(true)}
        onEnviado={() => setTela('SalaProfessor')}
      />
    );
  // Sem os dados exigidos pela página, mostramos a entrada para iniciar uma sessão válida.
  else pagina = <Inicio onEntrarAluno={entrarAluno} onEntrarProfessor={() => setTela('Login')} />;

  // StatusBar deixa os ícones do sistema escuros sobre o fundo claro; pagina contém a tela escolhida acima.
  return (
    <SafeAreaProvider initialMetrics={initialWindowMetrics}>
      <StatusBar style="dark" />
      {pagina}
    </SafeAreaProvider>
  );
}
