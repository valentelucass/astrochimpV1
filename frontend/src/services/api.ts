import type { DocumentPickerAsset } from 'expo-document-picker';
import type {
  Acesso,
  AcessoProfessor,
  Professor,
  CadastroProfessor,
  AtualizacaoProfessor,
  PerfilPublicoProfessor,
  Sala,
  EntradaSala,
  Cronometro,
  Duvida,
  Material,
  NovoMaterial,
  Quiz,
  NovoQuiz,
  PerguntaAluno,
  ResultadoQuiz,
} from '../types/modelos';
import { Platform } from 'react-native';
import Constants from 'expo-constants';
import { Base64 } from 'js-base64';

// No celular, o endereço do Expo aponta para o computador que também executa o Spring.
// EXPO_PUBLIC_API_URL permite informar outro endereço quando o backend estiver em outra máquina.
function enderecoDoBackend() {
  if (process.env.EXPO_PUBLIC_API_URL) return process.env.EXPO_PUBLIC_API_URL.replace(/\/$/, '');
  if (Platform.OS === 'web' && typeof window !== 'undefined')
    return 'http://' + window.location.hostname + ':8080';
  let host = Constants.expoConfig && Constants.expoConfig.hostUri;
  // hostUri vem como IP:porta do Expo. Aproveitamos o IP e usamos a porta 8080 do Spring.
  if (host) host = host.split(':')[0];
  // No emulador Android, 10.0.2.2 é o endereço que alcança o computador onde a API está aberta.
  if (Platform.OS === 'android' && (!host || host === 'localhost' || host === '127.0.0.1'))
    host = '10.0.2.2';
  return 'http://' + (host || 'localhost') + ':8080';
}

// As consultas e os downloads importam esta mesma base para acessar o mesmo backend.
export const ENDERECO_API = enderecoDoBackend();

// Monta a identificação que acompanha cada pedido; sem acesso, a rota é consultada como visitante.
export function cabecalhosDeAcesso(acesso: Acesso = null) {
  // Record<string, string> descreve um objeto com nomes e valores de texto, como Authorization e seu conteúdo.
  const cabecalhos: Record<string, string> = {};
  if (acesso && 'email' in acesso) {
    // O login Basic envia e-mail e senha no cabeçalho, no formato que o Spring já usa.
    cabecalhos.Authorization =
      'Basic ' + Base64.encode(acesso.email.trim().toLowerCase() + ':' + acesso.senha);
  }
  // O token permite ao Spring reconhecer o aluno que entrou na sala, sem pedir login de professor.
  if (acesso && 'tokenAluno' in acesso) cabecalhos['X-Aluno-Token'] = acesso.tokenAluno;
  return cabecalhos;
}

// Traduz o número da resposta HTTP quando o backend não manda uma mensagem explicando a recusa.
function mensagemDoStatus(status: number) {
  if (status === 400) return 'Confira os campos preenchidos e tente novamente.';
  if (status === 401) return 'Confira o e-mail e a senha ou entre novamente na sala.';
  if (status === 403) return 'Você não tem permissão para fazer isso.';
  if (status === 404) return 'Não encontramos estes dados. Atualize a tela e tente novamente.';
  if (status === 409) return 'Este pedido já foi feito ou não está disponível no momento.';
  if (status === 413) return 'O arquivo deve ter no máximo 10 MB.';
  return 'Não foi possível concluir o pedido. Tente novamente.';
}

// fetch envia o pedido ao Spring. T indica à chamada o tipo esperado, como Professor ou Sala[].
// Concentramos aqui corpo, identificação e erros para as telas não repetirem esse preparo em cada botão.
async function requisicao<T>(
  caminho: string,
  metodo = 'GET',
  dados: object | null = null,
  acesso: Acesso = null,
): Promise<T> {
  const cabecalhos = cabecalhosDeAcesso(acesso);
  let corpo: string | FormData | undefined;
  // Upload usa FormData para levar o arquivo; os outros pedidos enviam os campos como texto JSON.
  // No upload, deixamos o próprio formulário definir o cabeçalho com as partes do arquivo.
  if (dados instanceof FormData) corpo = dados;
  else if (dados !== null) {
    cabecalhos['Content-Type'] = 'application/json';
    corpo = JSON.stringify(dados);
  }
  let resposta;
  // Após 15 segundos sem resposta, libera o botão para a pessoa tentar de novo.
  const controle = new AbortController();
  const limite = setTimeout(() => controle.abort(), 15000);
  try {
    resposta = await fetch(ENDERECO_API + caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo,
      signal: controle.signal,
    });
  } catch {
    throw new Error('Não foi possível conectar. Verifique sua conexão e tente novamente.');
  } finally {
    // Retira o limite quando fetch termina, para ele não disparar depois de um pedido concluído.
    clearTimeout(limite);
  }
  // Primeiro lemos texto porque uma resposta sem conteúdo, como 204, não pode ser lida como JSON.
  const texto = await resposta.text();
  let resultado: unknown = null;
  if (texto) {
    try {
      resultado = JSON.parse(texto);
    } catch {
      resultado = null;
    }
  }
  // unknown exige conferir se detail ou message realmente é texto antes de mostrar o motivo à pessoa.
  if (!resposta.ok) {
    let mensagem = mensagemDoStatus(resposta.status);
    if (
      resultado &&
      typeof resultado === 'object' &&
      'detail' in resultado &&
      typeof resultado.detail === 'string'
    )
      mensagem = resultado.detail;
    else if (
      resultado &&
      typeof resultado === 'object' &&
      'message' in resultado &&
      typeof resultado.message === 'string'
    )
      mensagem = resultado.message;
    throw new ErroApi(mensagem, resposta.status);
  }
  // Rotas como responder ao quiz e excluir um quiz terminam sem um JSON de resposta.
  return resultado as T;
}

// Junta título e arquivo nos nomes de campos que as rotas de upload do Spring recebem.
function formularioDeArquivo(titulo: string, arquivo: DocumentPickerAsset) {
  const formulario = new FormData();
  formulario.append('titulo', titulo);
  // O navegador fornece File; o celular fornece o endereço local uri, o nome e o tipo do arquivo.
  if (Platform.OS === 'web') {
    if (!arquivo.file) throw new Error('Selecione o arquivo novamente.');
    formulario.append('arquivo', arquivo.file);
  } else {
    formulario.append('arquivo', {
      uri: arquivo.uri,
      name: arquivo.name,
      type: arquivo.mimeType || 'application/octet-stream',
    });
  }
  return formulario;
}

// O status permite diferenciar um voto repetido (409) de uma falha de conexão.
export class ErroApi extends Error {
  status: number;

  constructor(mensagem: string, status: number) {
    // super guarda a mensagem de Error; acrescentamos status para a tela tratar uma recusa específica.
    super(mensagem);
    this.name = 'ErroApi';
    this.status = status;
  }
}

// Cada função abaixo escolhe uma rota e os dados do pedido. requisicao cuida do envio e da resposta.
const api = {
  // O cadastro recebe a senha; o perfil devolvido pelo Spring já vem sem ela.
  cadastrarProfessor: (dados: CadastroProfessor) =>
    requisicao<Professor>('/api/professores', 'POST', dados),
  // O e-mail autenticado identifica o perfil, então a tela de login não precisa adivinhar seu id.
  buscarProfessorLogado: (acesso: AcessoProfessor) =>
    requisicao<Professor>('/api/professores/me', 'GET', null, acesso),
  // A edição envia nome, descrição, foto e endereço público permitidos pelo formulário do perfil.
  atualizarProfessor: (id: number, dados: AtualizacaoProfessor, acesso: AcessoProfessor) =>
    requisicao<Professor>('/api/professores/' + id, 'PUT', dados, acesso),
  // Consulta pelo nome do endereço público; encodeURIComponent prepara esse nome para entrar na URL.
  buscarPerfilPublico: (slug: string) =>
    requisicao<PerfilPublicoProfessor>('/api/professores/publico/' + encodeURIComponent(slug)),
  // O painel recebe as salas do professor conectado, incluindo o histórico das encerradas.
  listarSalas: (professorId: number, acesso: AcessoProfessor) =>
    requisicao<Sala[]>('/api/professores/' + professorId + '/salas', 'GET', null, acesso),
  // O Spring gera o código e a data de expiração da aula a partir do professor e do nome informado.
  criarSala: (professorId: number, nomeAula: string, acesso: AcessoProfessor) =>
    requisicao<Sala>('/api/salas', 'POST', { professorId, nomeAula }, acesso),
  // A entrada devolve o id e o token da nova participação do aluno nesta sala.
  entrarSala: (codigo: string, nomeAluno: string) =>
    requisicao<EntradaSala>('/api/salas/entrar', 'POST', { codigo, nomeAluno }),
  // Atualiza dados como estado da aula e quantidade de alunos que entraram nela.
  buscarSala: (salaId: number, acesso: Acesso) =>
    requisicao<Sala>('/api/salas/' + salaId, 'GET', null, acesso),
  // Pede ao Spring para finalizar a aula do professor conectado, incluindo quiz e cronômetro em andamento.
  encerrarSala: (salaId: number, acesso: AcessoProfessor) =>
    requisicao<Sala>('/api/salas/' + salaId + '/encerrar', 'PATCH', null, acesso),
  // segundos é o tempo da atividade que todos os participantes acompanharão na sala.
  iniciarCronometro: (salaId: number, segundos: number, acesso: AcessoProfessor) =>
    requisicao<Cronometro>('/api/salas/' + salaId + '/cronometro', 'POST', { segundos }, acesso),
  // O servidor calcula quanto falta desde o início; a tela não escolhe o tempo restante.
  buscarCronometro: (salaId: number, acesso: Acesso) =>
    requisicao<Cronometro>('/api/salas/' + salaId + '/cronometro', 'GET', null, acesso),
  // Remove a contagem da sala e devolve o cronômetro sem atividade.
  pararCronometro: (salaId: number, acesso: AcessoProfessor) =>
    requisicao<Cronometro>('/api/salas/' + salaId + '/cronometro', 'DELETE', null, acesso),
  // A rota do professor mostra autores; a rota do aluno esconde a identidade nas dúvidas anônimas.
  listarDuvidas: (salaId: number, acesso: Acesso, professor = false) =>
    requisicao<Duvida[]>(
      '/api/salas/' + salaId + '/duvidas' + (professor ? '/professor' : ''),
      'GET',
      null,
      acesso,
    ),
  // Envia só o texto e a opção de anonimato; o token identifica quem escreveu.
  criarDuvida: (salaId: number, texto: string, anonima: boolean, acesso: Acesso) =>
    requisicao<Duvida>('/api/salas/' + salaId + '/duvidas', 'POST', { texto, anonima }, acesso),
  // Acrescenta um voto do aluno identificado; o backend recusa outro voto dele na mesma dúvida.
  votarDuvida: (duvidaId: number, acesso: Acesso) =>
    requisicao<Duvida>('/api/duvidas/' + duvidaId + '/votos', 'POST', null, acesso),
  // O professor pode deixar a dúvida pendente, respondida ou arquivada.
  alterarEstadoDuvida: (duvidaId: number, estado: string, acesso: AcessoProfessor) =>
    requisicao<Duvida>('/api/duvidas/' + duvidaId + '/estado', 'PATCH', { estado }, acesso),
  // true destaca a dúvida no topo; false retira esse destaque sem apagar a pergunta.
  fixarDuvida: (duvidaId: number, fixada: boolean, acesso: AcessoProfessor) =>
    requisicao<Duvida>('/api/duvidas/' + duvidaId + '/fixacao', 'PATCH', { fixada }, acesso),
  // A biblioteca privada do professor inclui os conteúdos publicados e os que ainda estão privados.
  listarMateriaisProfessor: (professorId: number, acesso: AcessoProfessor) =>
    requisicao<Material[]>('/api/professores/' + professorId + '/materiais', 'GET', null, acesso),
  // A turma recebe apenas os materiais que foram compartilhados nesta aula.
  listarMateriaisSala: (salaId: number, acesso: Acesso) =>
    requisicao<Material[]>('/api/salas/' + salaId + '/materiais', 'GET', null, acesso),
  // Salva link ou código no perfil antes de ele ser enviado a uma sala.
  criarMaterial: (professorId: number, dados: NovoMaterial, acesso: AcessoProfessor) =>
    requisicao<Material>('/api/professores/' + professorId + '/materiais', 'POST', dados, acesso),
  // Este upload guarda o arquivo na biblioteca do professor, sem escolher uma sala.
  enviarArquivoProfessor: (
    professorId: number,
    titulo: string,
    arquivo: DocumentPickerAsset,
    acesso: AcessoProfessor,
  ) =>
    requisicao<Material>(
      '/api/professores/' + professorId + '/materiais/upload',
      'POST',
      formularioDeArquivo(titulo, arquivo),
      acesso,
    ),
  // Este upload já compartilha o arquivo na aula indicada por salaId.
  enviarArquivoSala: (
    salaId: number,
    titulo: string,
    arquivo: DocumentPickerAsset,
    acesso: AcessoProfessor,
  ) =>
    requisicao<Material>(
      '/api/salas/' + salaId + '/materiais/upload',
      'POST',
      formularioDeArquivo(titulo, arquivo),
      acesso,
    ),
  // Associa à sala um material que já existe no perfil, sem criar outra cópia dele.
  publicarMaterial: (salaId: number, materialId: number, acesso: AcessoProfessor) =>
    requisicao<Material>('/api/salas/' + salaId + '/materiais/' + materialId, 'POST', null, acesso),
  // O destaque vale para esta sala; fixar aqui não muda o destaque em outra aula.
  fixarMaterial: (salaId: number, materialId: number, fixado: boolean, acesso: AcessoProfessor) =>
    requisicao<Material>(
      '/api/salas/' + salaId + '/materiais/' + materialId + '/fixacao',
      'PATCH',
      { fixado },
      acesso,
    ),
  // Escolhe se visitantes sem login podem acessar este conteúdo no perfil público do professor.
  publicarNoPerfil: (
    professorId: number,
    materialId: number,
    publico: boolean,
    acesso: AcessoProfessor,
  ) =>
    requisicao<Material>(
      '/api/professores/' + professorId + '/materiais/' + materialId + '/publicacao',
      'PATCH',
      { publico },
      acesso,
    ),
  // Retorna os quizzes guardados para o professor escolher os desafios das próximas aulas.
  listarQuizzes: (professorId: number, acesso: AcessoProfessor) =>
    requisicao<Quiz[]>('/api/professores/' + professorId + '/quizzes', 'GET', null, acesso),
  // O detalhe do professor inclui perguntas e alternativas corretas para ele revisar seu quiz.
  buscarQuiz: (quizId: number, acesso: AcessoProfessor) =>
    requisicao<Quiz>('/api/quizzes/' + quizId, 'GET', null, acesso),
  // Guarda o conjunto de perguntas e alternativas sem iniciar uma rodada na sala.
  criarQuiz: (professorId: number, dados: NovoQuiz, acesso: AcessoProfessor) =>
    requisicao<Quiz>('/api/professores/' + professorId + '/quizzes', 'POST', dados, acesso),
  // Cria o desafio e já coloca sua primeira pergunta na sala escolhida pelo professor.
  criarEEnviarQuiz: (
    salaId: number,
    professorId: number,
    dados: NovoQuiz,
    acesso: AcessoProfessor,
  ) =>
    requisicao<Quiz>(
      '/api/salas/' + salaId + '/quiz/criar-e-enviar?professorId=' + professorId,
      'POST',
      dados,
      acesso,
    ),
  // Reaplica um quiz existente; cada aplicação recebe uma rodada nova no backend.
  enviarQuiz: (salaId: number, quizId: number, acesso: AcessoProfessor) =>
    requisicao<void>(
      '/api/salas/' + salaId + '/quiz/enviar-existente/' + quizId,
      'POST',
      null,
      acesso,
    ),
  // Muda a opção de manter o quiz na biblioteca que o professor pode reutilizar.
  salvarQuizNoPerfil: (quizId: number, salvar: boolean, acesso: AcessoProfessor) =>
    requisicao<Quiz>(
      '/api/quizzes/' + quizId + '/salvar-no-perfil?salvar=' + salvar,
      'PATCH',
      null,
      acesso,
    ),
  // A exclusão é recusada pelo Spring se o quiz estiver em uso ou tiver respostas registradas.
  excluirQuiz: (quizId: number, acesso: AcessoProfessor) =>
    requisicao<void>('/api/quizzes/' + quizId, 'DELETE', null, acesso),
  // 404 nesta rota significa que não há quiz acontecendo; null permite mostrar a tela de espera.
  buscarPerguntaAtual: async (salaId: number, acesso: Acesso): Promise<PerguntaAluno | null> => {
    try {
      return await requisicao<PerguntaAluno>(
        '/api/salas/' + salaId + '/quiz/pergunta-atual',
        'GET',
        null,
        acesso,
      );
    } catch (erro) {
      if (erro instanceof ErroApi && erro.status === 404) return null;
      throw erro;
    }
  },
  // Envia o id escolhido; o token e a sala permitem ao servidor conferir aluno, pergunta e rodada.
  responderQuiz: (salaId: number, alternativaId: number, acesso: Acesso) =>
    requisicao<void>('/api/salas/' + salaId + '/quiz/resposta', 'POST', { alternativaId }, acesso),
  // Só o professor consulta as quantidades de respostas por alternativa da pergunta atual.
  buscarResultadoQuiz: async (
    salaId: number,
    acesso: AcessoProfessor,
  ): Promise<ResultadoQuiz | null> => {
    try {
      return await requisicao<ResultadoQuiz>(
        '/api/salas/' + salaId + '/quiz/resultado',
        'GET',
        null,
        acesso,
      );
    } catch (erro) {
      if (erro instanceof ErroApi && erro.status === 404) return null;
      throw erro;
    }
  },
  // Avança a rodada; ao passar da última pergunta, o backend conclui o quiz da sala.
  proximaPergunta: (salaId: number, acesso: AcessoProfessor) =>
    requisicao<void>('/api/salas/' + salaId + '/quiz/proxima-pergunta', 'POST', null, acesso),
  // Permite interromper o desafio antes da última pergunta, mantendo a aula aberta.
  encerrarQuiz: (salaId: number, acesso: AcessoProfessor) =>
    requisicao<void>('/api/salas/' + salaId + '/quiz/encerrar', 'POST', null, acesso),
};

export default api;
