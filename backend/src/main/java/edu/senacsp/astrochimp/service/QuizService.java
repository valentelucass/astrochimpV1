package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.AlternativaAlunoDto;
import edu.senacsp.astrochimp.dto.PerguntaAlunoDto;
import edu.senacsp.astrochimp.dto.ResultadoAlternativaDto;
import edu.senacsp.astrochimp.dto.ResultadoPerguntaDto;
import edu.senacsp.astrochimp.entity.Aluno;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.entity.Quiz;
import edu.senacsp.astrochimp.entity.QuizAlternativa;
import edu.senacsp.astrochimp.entity.QuizPergunta;
import edu.senacsp.astrochimp.entity.QuizResposta;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.QuizAlternativaRepository;
import edu.senacsp.astrochimp.repository.QuizRepository;
import edu.senacsp.astrochimp.repository.QuizRespostaRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// O Spring cria este service para montar o quiz, receber respostas e controlar as rodadas.
@Service
public class QuizService {

    private static final String[] LETRAS = {"A", "B", "C", "D", "E", "F"};

    private final QuizRepository quizRepository;
    private final QuizAlternativaRepository quizAlternativaRepository;
    private final QuizRespostaRepository quizRespostaRepository;
    private final SalaRepository salaRepository;
    private final AcessoService acessoService;

    // Recebe as consultas de quizzes, alternativas, respostas e salas, além das regras de permissão.
    public QuizService(
            QuizRepository quizRepository,
            QuizAlternativaRepository quizAlternativaRepository,
            QuizRespostaRepository quizRespostaRepository,
            SalaRepository salaRepository,
            AcessoService acessoService) {
        this.quizRepository = quizRepository;
        this.quizAlternativaRepository = quizAlternativaRepository;
        this.quizRespostaRepository = quizRespostaRepository;
        this.salaRepository = salaRepository;
        this.acessoService = acessoService;
    }

    // @Transactional salva o quiz, suas perguntas e alternativas na mesma transação.
    // Se a operação lançar um erro como ResponseStatusException, o Spring desfaz suas mudanças no banco.
    @Transactional
    public Quiz criarQuiz(Long professorId, Quiz pedido) {
        Professor professor = acessoService.professor(professorId);
        Quiz quiz = montarQuiz(professor, pedido);
        return carregarPerguntas(quizRepository.save(quiz));
    }

    // readOnly indica que a consulta dos quizzes, perguntas e alternativas apenas lê os dados.
    @Transactional(readOnly = true)
    public List<Quiz> listarQuizzesDoProfessor(Long professorId) {
        acessoService.professor(professorId);
        // A lista começa vazia e recebe apenas quizzes marcados para continuar no perfil do professor.
        List<Quiz> quizzes = new ArrayList<>();
        for (Quiz quiz : quizRepository.findByProfessorIdAndSalvoNoPerfilTrue(professorId)) {
            quizzes.add(carregarPerguntas(quiz));
        }
        return quizzes;
    }

    @Transactional(readOnly = true)
    // Prepara o detalhe completo do desafio para o professor revisar suas perguntas e o gabarito.
    public Quiz buscar(Long quizId) {
        return carregarPerguntas(buscarPorId(quizId));
    }

    // Uma busca válida precisa do id existente e do login do professor que é dono desse quiz.
    private Quiz buscarPorId(Long quizId) {
        if (quizId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz obrigatório");
        }
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não encontrado");
        }
        acessoService.professor(quiz.getProfessor().getId());
        return quiz;
    }

    @Transactional
    // Muda a presença do desafio na biblioteca reutilizável, sem iniciar uma rodada.
    public Quiz alternarSalvoNoPerfil(Long quizId, boolean salvar) {
        Quiz quiz = buscarPorId(quizId);
        quiz.setSalvoNoPerfil(salvar);
        return carregarPerguntas(quizRepository.save(quiz));
    }

    @Transactional
    // Preserva o histórico: não permite apagar um desafio em andamento nem um que já recebeu respostas.
    public void excluirQuiz(Long quizId) {
        Quiz quiz = buscarPorId(quizId);
        if (salaRepository.existsByQuizEmAndamentoId(quizId) ||
            quizRespostaRepository.existsByAlternativa_Pergunta_Quiz_Id(quizId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quiz com respostas ou em uso não pode ser excluído");
        }
        quizRepository.delete(quiz);
    }

    @Transactional
    // Reutiliza um desafio pronto depois de conferir que sala e quiz pertencem ao mesmo professor.
    public void enviarQuizExistenteParaSala(Long salaId, Long quizId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        Quiz quiz = buscarPorId(quizId);
        if (!sala.getProfessor().getId().equals(quiz.getProfessor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quiz pertence a outro professor");
        }
        iniciarSessaoDeQuiz(sala, quiz);
    }

    @Transactional
    // O cadastro e a abertura do desafio usam a mesma transação, evitando salvar um quiz cujo envio falhou.
    public void criarEEnviarQuizParaSala(Long salaId, Long professorId, Quiz pedido) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        Professor professor = acessoService.professor(professorId);
        if (!sala.getProfessor().getId().equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor não é dono da sala");
        }
        Quiz quizMontado = montarQuiz(professor, pedido);
        Quiz quiz = quizRepository.save(quizMontado);
        iniciarSessaoDeQuiz(sala, quiz);
    }

    private void iniciarSessaoDeQuiz(Sala sala, Quiz quiz) {
        acessoService.exigirSalaAtiva(sala);
        if (sala.getQuizEmAndamento() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe quiz em andamento");
        }
        if (quiz.getPerguntas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz sem perguntas");
        }

        // A primeira pergunta e o instante de abertura passam a definir a contagem desta aplicação do quiz.
        sala.setQuizEmAndamento(quiz);
        sala.setQuizPerguntaAtual(quiz.getPerguntas().get(0));
        sala.setQuizPerguntaIniciadaEm(LocalDateTime.now());
        // Cada envio do quiz à sala cria uma nova rodada, com um código próprio.
        // Assim, respostas de outra sala ou de uma aplicação anterior do mesmo quiz não entram nesta rodada.
        sala.setQuizSessaoId(UUID.randomUUID().toString());
        salaRepository.save(sala);
    }

    @Transactional(readOnly = true)
    public PerguntaAlunoDto obterPerguntaAtual(Long salaId, String tokenAluno) {
        Sala sala = acessoService.salaComAcesso(salaId, tokenAluno);
        acessoService.exigirSalaAtiva(sala);
        if (sala.getQuizEmAndamento() == null || sala.getQuizPerguntaAtual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }

        QuizPergunta pergunta = sala.getQuizPerguntaAtual();
        int totalPerguntas = sala.getQuizEmAndamento().getPerguntas().size();
        int tempoRestanteSegundos = calcularTempoRestante(pergunta, sala.getQuizPerguntaIniciadaEm());

        // Enviamos só id, letra e texto de cada alternativa para o aluno poder escolher.
        // O campo correta fica fora da resposta para não revelar o gabarito durante o quiz.
        List<AlternativaAlunoDto> alternativas = new ArrayList<>();
        for (QuizAlternativa alternativa : pergunta.getAlternativas()) {
            alternativas.add(new AlternativaAlunoDto(
                    alternativa.getId(), alternativa.getLetra(), alternativa.getTexto()));
        }

        PerguntaAlunoDto dados = new PerguntaAlunoDto(
                sala.getQuizEmAndamento().getId(),
                pergunta.getId(),
                pergunta.getEnunciado(),
                pergunta.getDificuldade(),
                pergunta.getOrdem(),
                totalPerguntas,
                tempoRestanteSegundos,
                alternativas);
        dados.setRodadaId(sala.getQuizSessaoId());
        return dados;
    }

    @Transactional
    public void avancarProximaPergunta(Long salaId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        if (sala.getQuizEmAndamento() == null || sala.getQuizPerguntaAtual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }

        List<QuizPergunta> perguntas = sala.getQuizEmAndamento().getPerguntas();

        // Procura a posição da pergunta atual; -1 indica que nenhum item da lista foi encontrado ainda.
        int indiceAtual = -1;
        for (int i = 0; i < perguntas.size(); i++) {
            if (perguntas.get(i).getId().equals(sala.getQuizPerguntaAtual().getId())) {
                indiceAtual = i;
                break;
            }
        }

        boolean eraUltimaPergunta = indiceAtual < 0 || indiceAtual == perguntas.size() - 1;
        if (eraUltimaPergunta) {
            limparQuizDaSala(sala);
            salaRepository.save(sala);
            return;
        }

        QuizPergunta proxima = perguntas.get(indiceAtual + 1);
        sala.setQuizPerguntaAtual(proxima);
        sala.setQuizPerguntaIniciadaEm(LocalDateTime.now());
        salaRepository.save(sala);
    }

    @Transactional
    public void registrarResposta(Long salaId, String tokenAluno, AlternativaAlunoDto pedido) {
        if (salaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sala obrigatória");
        }
        // Duas requisições esperam sua vez para conferir e salvar respostas nesta sala.
        Sala sala = salaRepository.buscarParaAtualizar(salaId).orElse(null);
        if (sala == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala não encontrada");
        }
        acessoService.exigirSalaAtiva(sala);
        Aluno aluno = acessoService.alunoDaSala(salaId, tokenAluno);
        if (sala.getQuizEmAndamento() == null || sala.getQuizPerguntaAtual() == null || sala.getQuizSessaoId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }
        if (pedido == null || pedido.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa obrigatória");
        }
        // Do pedido usamos só o id; o texto e a pergunta da alternativa vêm do banco.
        QuizAlternativa alternativa = quizAlternativaRepository.findById(pedido.getId()).orElse(null);
        if (alternativa == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alternativa não encontrada");
        }

        if (!alternativa.getPergunta().getId().equals(sala.getQuizPerguntaAtual().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa não é da pergunta atual");
        }
        if (calcularTempoRestante(sala.getQuizPerguntaAtual(), sala.getQuizPerguntaIniciadaEm()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tempo de resposta encerrado");
        }
        // A mesma pergunta pode ser usada de novo em outra rodada.
        // O código da rodada permite responder novamente, mas impede duas respostas nesta rodada.
        boolean jaRespondeu = quizRespostaRepository.existsBySessaoIdAndAlunoIdAndAlternativa_Pergunta_Id(
                sala.getQuizSessaoId(), aluno.getId(), sala.getQuizPerguntaAtual().getId());
        if (jaRespondeu) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aluno já respondeu esta pergunta");
        }

        QuizResposta resposta = new QuizResposta();
        resposta.setAluno(aluno);
        resposta.setAlternativa(alternativa);
        resposta.setRespondidoEm(LocalDateTime.now());
        resposta.setSessaoId(sala.getQuizSessaoId());
        quizRespostaRepository.save(resposta);
    }

    @Transactional(readOnly = true)
    public ResultadoPerguntaDto obterResultadoPerguntaAtual(Long salaId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        if (sala.getQuizPerguntaAtual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }

        QuizPergunta pergunta = sala.getQuizPerguntaAtual();
        List<ResultadoAlternativaDto> resultados = new ArrayList<>();
        long totalRespondentes = 0;
        for (QuizAlternativa alternativa : pergunta.getAlternativas()) {
            // Contamos só as respostas desta rodada, mesmo se outra sala estiver usando o mesmo quiz.
            long totalRespostas = quizRespostaRepository.countBySessaoIdAndAlternativaId(
                    sala.getQuizSessaoId(), alternativa.getId());
            resultados.add(new ResultadoAlternativaDto(
                    alternativa.getLetra(), alternativa.getTexto(), totalRespostas));
            totalRespondentes += totalRespostas;
        }

        return new ResultadoPerguntaDto(
                pergunta.getId(), pergunta.getEnunciado(), totalRespondentes, resultados);
    }

    @Transactional
    // Remove os controles do desafio da sala; o professor pode continuar a aula e aplicar outro quiz.
    public void encerrarQuiz(Long salaId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        limparQuizDaSala(sala);
        salaRepository.save(sala);
    }

    private void limparQuizDaSala(Sala sala) {
        // A sala deixa de ter uma rodada em andamento; as respostas antigas continuam no banco.
        sala.setQuizEmAndamento(null);
        sala.setQuizPerguntaAtual(null);
        sala.setQuizPerguntaIniciadaEm(null);
        sala.setQuizSessaoId(null);
    }

    private Quiz montarQuiz(Professor professor, Quiz pedido) {
        if (pedido == null || pedido.getTitulo() == null || pedido.getTitulo().isBlank() ||
            pedido.getPerguntas() == null || pedido.getPerguntas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título e perguntas obrigatórios");
        }
        validarTamanho(pedido.getTitulo().trim(), "Título do quiz");

        // Copiamos só o que o professor pode enviar. Dono, datas, ordem e letras são definidos aqui.
        Quiz quiz = new Quiz();
        quiz.setTitulo(pedido.getTitulo().trim());
        quiz.setSalvoNoPerfil(pedido.isSalvoNoPerfil());
        quiz.setProfessor(professor);
        quiz.setCriadoEm(LocalDateTime.now());

        List<QuizPergunta> perguntas = new ArrayList<>();
        // A numeração exibida começa em 1 e segue a ordem em que o formulário enviou as perguntas.
        int ordem = 1;
        for (QuizPergunta dadosPergunta : pedido.getPerguntas()) {
            QuizPergunta pergunta = montarPergunta(quiz, dadosPergunta, ordem);
            perguntas.add(pergunta);
            ordem++;
        }

        quiz.setPerguntas(perguntas);

        return quiz;
    }

    private QuizPergunta montarPergunta(Quiz quiz, QuizPergunta pedido, int ordem) {
        if (pedido == null || pedido.getEnunciado() == null || pedido.getEnunciado().isBlank() ||
            pedido.getTempoLimiteSegundos() <= 0 || pedido.getAlternativas() == null ||
            pedido.getAlternativas().size() < 2 || pedido.getAlternativas().size() > LETRAS.length) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pergunta precisa de tempo e de 2 a 6 alternativas");
        }
        validarTamanho(pedido.getEnunciado().trim(), "Enunciado");
        validarTamanho(pedido.getDificuldade(), "Dificuldade");
        validarAlternativas(pedido.getAlternativas());

        QuizPergunta pergunta = new QuizPergunta();
        pergunta.setQuiz(quiz);
        pergunta.setEnunciado(pedido.getEnunciado().trim());
        pergunta.setDificuldade(pedido.getDificuldade());
        pergunta.setTempoLimiteSegundos(pedido.getTempoLimiteSegundos());
        pergunta.setOrdem(ordem);
        pergunta.setAlternativas(montarAlternativas(pergunta, pedido.getAlternativas()));
        return pergunta;
    }

    // Além de exigir texto em todas as opções, conta as marcadas como corretas e aceita exatamente uma.
    private void validarAlternativas(List<QuizAlternativa> alternativas) {
        int totalCorretas = 0;
        for (QuizAlternativa alternativa : alternativas) {
            if (alternativa == null || alternativa.getTexto() == null || alternativa.getTexto().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto da alternativa obrigatório");
            }
            validarTamanho(alternativa.getTexto().trim(), "Texto da alternativa");
            if (alternativa.isCorreta()) {
                totalCorretas++;
            }
        }
        if (totalCorretas != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pergunta precisa de uma resposta correta");
        }
    }

    // Cada opção fica ligada à pergunta que estamos montando e recebe sua letra pela posição no array LETRAS.
    private List<QuizAlternativa> montarAlternativas(QuizPergunta pergunta, List<QuizAlternativa> alternativasDoPedido) {
        List<QuizAlternativa> alternativas = new ArrayList<>();
        int indice = 0;
        for (QuizAlternativa dadosAlternativa : alternativasDoPedido) {
            QuizAlternativa alternativa = new QuizAlternativa();
            alternativa.setPergunta(pergunta);
            alternativa.setLetra(LETRAS[indice]);
            alternativa.setTexto(dadosAlternativa.getTexto().trim());
            alternativa.setCorreta(dadosAlternativa.isCorreta());
            alternativas.add(alternativa);
            indice++;
        }
        return alternativas;
    }

    private Quiz carregarPerguntas(Quiz quiz) {
        // As listas são carregadas pelo JPA quando usadas. size() consulta as alternativas
        // ainda dentro da transação, para que o controller consiga enviá-las depois.
        for (QuizPergunta pergunta : quiz.getPerguntas()) {
            pergunta.getAlternativas().size();
        }
        return quiz;
    }

    private int calcularTempoRestante(QuizPergunta pergunta, LocalDateTime iniciadaEm) {
        if (iniciadaEm == null) {
            return 0;
        }
        // O professor escolheu o limite desta pergunta ao criar o quiz.
        // iniciadaEm é o instante em que ela foi aberta nesta sala, não a data de criação do quiz.
        int segundosTotais = pergunta.getTempoLimiteSegundos();
        long segundosDecorridos = Duration.between(iniciadaEm, LocalDateTime.now()).getSeconds();
        if (segundosDecorridos < 0) {
            segundosDecorridos = 0;
        }
        long segundosRestantes = segundosTotais - segundosDecorridos;
        if (segundosRestantes < 0) {
            return 0;
        }
        return (int) segundosRestantes;
    }

    private void validarTamanho(String texto, String campo) {
        // Título, enunciado, dificuldade e texto das alternativas usam colunas de 255 caracteres.
        if (texto != null && texto.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ter no máximo 255 caracteres");
        }
    }

}
