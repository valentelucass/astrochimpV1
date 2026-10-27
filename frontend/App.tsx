import { useEffect, useState } from 'react';
import { ActivityIndicator, BackHandler, Platform, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { Nunito_400Regular } from '@expo-google-fonts/nunito/400Regular';
import { Nunito_700Bold } from '@expo-google-fonts/nunito/700Bold';
import { Nunito_800ExtraBold } from '@expo-google-fonts/nunito/800ExtraBold';
import Inicio from './src/pages/Inicio';
import LoginProfessor from './src/pages/LoginProfessor';
import CadastroProfessor from './src/pages/CadastroProfessor';
import PainelProfessor from './src/pages/PainelProfessor';
import PerfilProfessor from './src/pages/PerfilProfessor';
import PerfilPublico from './src/pages/PerfilPublico';
import SalaProfessor from './src/pages/SalaProfessor';
import SalaAluno from './src/pages/SalaAluno';
import QuizAluno from './src/pages/QuizAluno';
import MateriaisSala from './src/pages/MateriaisSala';
import CriarMaterial from './src/pages/CriarMaterial';
import CriarQuiz from './src/pages/CriarQuiz';
import EscolherQuiz from './src/pages/EscolherQuiz';
import cores from './src/styles/cores';
import type { AcessoProfessor, Aluno, EntradaSala, Professor, Sala } from './src/types/modelos';

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
    <>
      <StatusBar style="dark" />
      {pagina}
    </>
  );
}
