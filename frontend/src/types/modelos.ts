// type descreve os campos de um objeto para o TypeScript conferir seu uso no código.
// Isso não valida uma resposta em tempo de execução nem substitui a proteção dos dados feita no Spring.
// Este acesso mantém o e-mail e a senha do professor apenas na sessão aberta para autenticar os pedidos.
export type AcessoProfessor = { email: string; senha: string };
// Depois de entrar na sala, o aluno usa o token recebido para ser reconhecido nos próximos pedidos.
export type AcessoAluno = { tokenAluno: string };
// Uma consulta pode usar professor, aluno ou null quando a rota é pública.
export type Acesso = AcessoProfessor | AcessoAluno | null;

// São os dados do próprio perfil devolvidos pelo servidor. descricao e fotoUrl podem vir sem valor (null).
// slugPerfilPublico é o nome que identifica o endereço público do professor, como ana-souza.
export type Professor = {
  id: number;
  nome: string;
  email: string;
  descricao: string | null;
  fotoUrl: string | null;
  slugPerfilPublico: string;
};

// O formulário de criação da conta envia a senha; o tipo Professor acima não inclui senha na resposta.
export type CadastroProfessor = {
  nome: string;
  email: string;
  senha: string;
  slugPerfilPublico: string;
};

// A edição do perfil envia só os campos que a tela permite alterar; e-mail e senha ficam fora desse pedido.
export type AtualizacaoProfessor = {
  nome: string;
  descricao: string;
  fotoUrl: string;
  slugPerfilPublico: string;
};

// O código serve para o aluno entrar; totalAlunos conta participações, mesmo depois de alguém sair da tela.
// expiraEm é o limite de duração da aula, diferente do cronômetro de uma atividade.
export type Sala = {
  id: number;
  codigo: string;
  nomeAula: string;
  status: string;
  nomeProfessor: string;
  criadaEm: string;
  expiraEm: string;
  totalAlunos: number;
};

// Além do acesso, a sessão do aluno guarda seu nome e id para a tela reconhecer a participação atual.
export type Aluno = { id: number; nomeAluno: string; tokenAluno: string };
// A API devolve sala, alunoId e tokenAluno; a tela de entrada acrescenta o nome que a pessoa digitou.
export type EntradaSala = { sala: Sala; alunoId: number; tokenAluno: string; nomeAluno?: string };

// Representa uma pergunta do feed, com quantidade de votos, estado e destaque de fixação.
// Para os alunos, autorId e autorNome vêm null quando o autor pediu anonimato.
export type Duvida = {
  id: number;
  texto: string;
  anonima: boolean;
  autorId: number | null;
  autorNome: string | null;
  estado: string;
  fixada: boolean;
  votos: number;
  criadaEm: string;
};

// Cronômetro da atividade da sala: 120 em segundosTotais significa dois minutos definidos pelo professor.
// segundosRestantes informa quanto ainda falta; ativo fica false quando a contagem para ou termina.
export type Cronometro = {
  segundosTotais: number | null;
  segundosRestantes: number;
  ativo: boolean;
};

// Conteúdo já salvo: conteudoOuUrl traz o código, o endereço do link ou a rota para baixar um arquivo.
// publico libera o perfil para visitantes; fixado indica destaque na sala consultada.
export type Material = {
  id: number;
  titulo: string;
  tipo: string;
  conteudoOuUrl: string;
  detalhe: string | null;
  publico: boolean;
  fixado: boolean;
  criadoEm: string;
};

// Campos preenchidos ao criar link ou código; id e data só aparecem depois que o backend salva o material.
export type NovoMaterial = {
  titulo: string;
  tipo: string;
  conteudoOuUrl: string;
  detalhe: string;
};

// A consulta pública recebe apenas dados de apresentação e materiais publicados, sem e-mail ou senha.
export type PerfilPublicoProfessor = {
  id: number;
  nome: string;
  descricao: string | null;
  fotoUrl: string | null;
  slugPerfilPublico: string;
  materiais: Material[];
};

// Na área do professor, correta identifica o gabarito de cada alternativa cadastrada.
export type AlternativaQuiz = { id: number; letra: string; texto: string; correta: boolean };
// A pergunta completa reúne o enunciado, seu tempo de resposta e as alternativas já salvas.
export type PerguntaQuiz = {
  id: number;
  enunciado: string;
  dificuldade: string;
  ordem: number;
  tempoLimiteSegundos: number;
  alternativas: AlternativaQuiz[];
};
// O quiz guardado pode ser aplicado de novo; salvoNoPerfil indica se ele pertence à biblioteca reutilizável.
export type Quiz = {
  id: number;
  titulo: string;
  salvoNoPerfil: boolean;
  perguntas: PerguntaQuiz[];
};

// Na criação, ainda não existem ids nem letras A/B: o servidor define esses dados ao salvar.
export type NovaAlternativa = { texto: string; correta: boolean };
// O formulário converte o tempo digitado para número antes de montar esta pergunta do pedido.
export type NovaPergunta = {
  enunciado: string;
  dificuldade: string;
  tempoLimiteSegundos: number;
  alternativas: NovaAlternativa[];
};
// Agrupa título, opção de salvar e todas as perguntas enviadas de uma vez pelo formulário.
export type NovoQuiz = { titulo: string; salvoNoPerfil: boolean; perguntas: NovaPergunta[] };

// A pergunta para o aluno não tem o campo correta, para não mostrar o gabarito durante o quiz.
// rodadaId muda quando o mesmo quiz é reaplicado, permitindo limpar a seleção da rodada anterior.
export type PerguntaAluno = {
  quizId: number;
  perguntaId: number;
  rodadaId: string;
  enunciado: string;
  dificuldade: string;
  numeroPergunta: number;
  totalPerguntas: number;
  tempoRestanteSegundos: number;
  alternativas: { id: number; letra: string; texto: string }[];
};

// O painel do professor recebe a contagem de respostas de cada alternativa da pergunta atual.
// totalRespondentes conta quem respondeu a ela nesta rodada, não todos os participantes da sala.
export type ResultadoQuiz = {
  perguntaId: number;
  enunciado: string;
  totalRespondentes: number;
  resultados: { letra: string; texto: string; totalRespostas: number }[];
};
