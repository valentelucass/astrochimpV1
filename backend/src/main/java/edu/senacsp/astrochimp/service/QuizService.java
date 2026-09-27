package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.AlternativaAlunoDto;
import edu.senacsp.astrochimp.dto.CriarAlternativaDto;
import edu.senacsp.astrochimp.dto.CriarPerguntaDto;
import edu.senacsp.astrochimp.dto.CriarQuizDto;
import edu.senacsp.astrochimp.dto.PerguntaAlunoDto;
import edu.senacsp.astrochimp.dto.ResponderQuizDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class QuizService {

    private static final String[] LETRAS = {"A", "B", "C", "D", "E", "F"};

    private final QuizRepository quizRepository;
    private final QuizAlternativaRepository quizAlternativaRepository;
    private final QuizRespostaRepository quizRespostaRepository;
    private final SalaRepository salaRepository;
    private final AcessoService acesso;

    public QuizService(
            QuizRepository quizRepository,
            QuizAlternativaRepository quizAlternativaRepository,
            QuizRespostaRepository quizRespostaRepository,
            SalaRepository salaRepository,
            AcessoService acesso) {
        this.quizRepository = quizRepository;
        this.quizAlternativaRepository = quizAlternativaRepository;
        this.quizRespostaRepository = quizRespostaRepository;
        this.salaRepository = salaRepository;
        this.acesso = acesso;
    }

    @Transactional
    public Quiz criarQuiz(Long professorId, CriarQuizDto pedido) {
        Professor professor = acesso.professor(professorId);
        Quiz quiz = montarQuiz(professor, pedido);
        return quizRepository.save(quiz);
    }

    public List<Quiz> listarQuizzesDoProfessor(Long professorId) {
        acesso.professor(professorId);
        return quizRepository.findByProfessorIdAndSalvoNoPerfilTrue(professorId);
    }

    public Quiz buscarPorId(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não encontrado");
        }
        acesso.professor(quiz.getProfessor().getId());
        return quiz;
    }

    @Transactional
    public Quiz alternarSalvoNoPerfil(Long quizId, boolean salvar) {
        Quiz quiz = buscarPorId(quizId);
        quiz.setSalvoNoPerfil(salvar);
        return quizRepository.save(quiz);
    }

    @Transactional
    public void excluirQuiz(Long quizId) {
        Quiz quiz = buscarPorId(quizId);
        if (salaRepository.existsByQuizEmAndamentoId(quizId) ||
            quizRespostaRepository.existsByAlternativa_Pergunta_Quiz_Id(quizId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quiz com respostas ou em uso não pode ser excluído");
        }
        quizRepository.delete(quiz);
    }

    @Transactional
    public Sala enviarQuizExistenteParaSala(Long salaId, Long quizId) {
        Sala sala = acesso.salaDoProfessor(salaId);
        Quiz quiz = buscarPorId(quizId);
        if (!sala.getProfessor().getId().equals(quiz.getProfessor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Quiz pertence a outro professor");
        }
        return iniciarSessaoDeQuiz(sala, quiz);
    }

    @Transactional
    public Sala criarEEnviarQuizParaSala(Long salaId, Long professorId, CriarQuizDto pedido) {
        Sala sala = acesso.salaDoProfessor(salaId);
        Professor professor = acesso.professor(professorId);
        if (!sala.getProfessor().getId().equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor não é dono da sala");
        }
        Quiz quizMontado = montarQuiz(professor, pedido);
        Quiz quiz = quizRepository.save(quizMontado);
        return iniciarSessaoDeQuiz(sala, quiz);
    }

    private Sala iniciarSessaoDeQuiz(Sala sala, Quiz quiz) {
        acesso.exigirSalaAtiva(sala);
        if (sala.getQuizEmAndamento() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe quiz em andamento");
        }
        if (quiz.getPerguntas() == null || quiz.getPerguntas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz sem perguntas");
        }

        QuizPergunta primeiraPergunta = quiz.getPerguntas().get(0);
        for (QuizPergunta pergunta : quiz.getPerguntas()) {
            if (pergunta.getOrdem() < primeiraPergunta.getOrdem()) {
                primeiraPergunta = pergunta;
            }
        }

        sala.setQuizEmAndamento(quiz);
        sala.setQuizPerguntaAtual(primeiraPergunta);
        sala.setQuizPerguntaIniciadaEm(LocalDateTime.now());
        sala.setQuizSessaoId(UUID.randomUUID().toString());
        return salaRepository.save(sala);
    }

    @Transactional(readOnly = true)
    public PerguntaAlunoDto obterPerguntaAtual(Long salaId, String tokenAluno) {
        Sala sala = acesso.sala(salaId);
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            acesso.alunoDaSala(salaId, tokenAluno);
        } else {
            acesso.salaDoProfessor(salaId);
        }
        if (sala.getQuizEmAndamento() == null || sala.getQuizPerguntaAtual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }

        QuizPergunta pergunta = sala.getQuizPerguntaAtual();
        int totalPerguntas = sala.getQuizEmAndamento().getPerguntas().size();
        int tempoRestante = calcularTempoRestante(pergunta, sala.getQuizPerguntaIniciadaEm());

        List<AlternativaAlunoDto> alternativas = new ArrayList<>();
        for (QuizAlternativa alternativa : pergunta.getAlternativas()) {
            alternativas.add(new AlternativaAlunoDto(
                    alternativa.getId(), alternativa.getLetra(), alternativa.getTexto()));
        }

        return new PerguntaAlunoDto(
                sala.getQuizEmAndamento().getId(),
                pergunta.getId(),
                pergunta.getEnunciado(),
                pergunta.getDificuldade(),
                pergunta.getOrdem(),
                totalPerguntas,
                tempoRestante,
                alternativas);
    }

    @Transactional
    public Sala avancarProximaPergunta(Long salaId) {
        Sala sala = acesso.salaDoProfessor(salaId);
        if (sala.getQuizEmAndamento() == null || sala.getQuizPerguntaAtual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }

        List<QuizPergunta> perguntas = new ArrayList<>(sala.getQuizEmAndamento().getPerguntas());
        perguntas.sort(new Comparator<QuizPergunta>() {
            @Override
            public int compare(QuizPergunta primeira, QuizPergunta segunda) {
                return Integer.compare(primeira.getOrdem(), segunda.getOrdem());
            }
        });

        int indiceAtual = -1;
        for (int i = 0; i < perguntas.size(); i++) {
            if (perguntas.get(i).getId().equals(sala.getQuizPerguntaAtual().getId())) {
                indiceAtual = i;
                break;
            }
        }

        boolean eraUltimaPergunta = indiceAtual < 0 || indiceAtual == perguntas.size() - 1;
        if (eraUltimaPergunta) {
            encerrarQuiz(salaId);
            return sala;
        }

        QuizPergunta proxima = perguntas.get(indiceAtual + 1);
        sala.setQuizPerguntaAtual(proxima);
        sala.setQuizPerguntaIniciadaEm(LocalDateTime.now());
        return salaRepository.save(sala);
    }

    @Transactional
    public void registrarResposta(Long salaId, String tokenAluno, ResponderQuizDto pedido) {
        Sala sala = acesso.sala(salaId);
        acesso.exigirSalaAtiva(sala);
        Aluno aluno = acesso.alunoDaSala(salaId, tokenAluno);
        if (sala.getQuizPerguntaAtual() == null || sala.getQuizSessaoId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }
        if (pedido == null || pedido.getAlternativaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa obrigatória");
        }
        QuizAlternativa alternativa = quizAlternativaRepository.findById(pedido.getAlternativaId()).orElse(null);
        if (alternativa == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alternativa não encontrada");
        }

        if (!alternativa.getPergunta().getId().equals(sala.getQuizPerguntaAtual().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa não é da pergunta atual");
        }
        if (calcularTempoRestante(sala.getQuizPerguntaAtual(), sala.getQuizPerguntaIniciadaEm()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tempo de resposta encerrado");
        }
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
        Sala sala = acesso.salaDoProfessor(salaId);
        if (sala.getQuizPerguntaAtual() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum quiz em andamento");
        }

        QuizPergunta pergunta = sala.getQuizPerguntaAtual();
        List<ResultadoAlternativaDto> resultados = new ArrayList<>();
        long totalRespondentes = 0;
        for (QuizAlternativa alternativa : pergunta.getAlternativas()) {
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
    public void encerrarQuiz(Long salaId) {
        Sala sala = acesso.salaDoProfessor(salaId);
        sala.setQuizEmAndamento(null);
        sala.setQuizPerguntaAtual(null);
        sala.setQuizPerguntaIniciadaEm(null);
        sala.setQuizSessaoId(null);
        salaRepository.save(sala);
    }

    private Quiz montarQuiz(Professor professor, CriarQuizDto pedido) {
        if (pedido == null || pedido.getTitulo() == null || pedido.getTitulo().isBlank() ||
            pedido.getPerguntas() == null || pedido.getPerguntas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título e perguntas obrigatórios");
        }

        Quiz quiz = new Quiz();
        quiz.setTitulo(pedido.getTitulo());
        quiz.setSalvoNoPerfil(pedido.isSalvarNoPerfil());
        quiz.setProfessor(professor);
        quiz.setCriadoEm(LocalDateTime.now());

        List<QuizPergunta> perguntas = new ArrayList<>();
        int ordem = 1;
        for (CriarPerguntaDto dadosPergunta : pedido.getPerguntas()) {
            QuizPergunta pergunta = montarPergunta(quiz, dadosPergunta, ordem);
            perguntas.add(pergunta);
            ordem++;
        }

        quiz.setPerguntas(perguntas);

        return quiz;
    }

    private QuizPergunta montarPergunta(Quiz quiz, CriarPerguntaDto pedido, int ordem) {
        if (pedido == null || pedido.getEnunciado() == null || pedido.getEnunciado().isBlank() ||
            pedido.getTempoLimiteSegundos() <= 0 || pedido.getAlternativas() == null ||
            pedido.getAlternativas().size() < 2 || pedido.getAlternativas().size() > LETRAS.length) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pergunta precisa de tempo e de 2 a 6 alternativas");
        }
        int totalCorretas = 0;
        for (CriarAlternativaDto alternativa : pedido.getAlternativas()) {
            if (alternativa == null || alternativa.getTexto() == null || alternativa.getTexto().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto da alternativa obrigatório");
            }
            if (alternativa.isCorreta()) {
                totalCorretas++;
            }
        }
        if (totalCorretas != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pergunta precisa de uma resposta correta");
        }

        QuizPergunta pergunta = new QuizPergunta();
        pergunta.setQuiz(quiz);
        pergunta.setEnunciado(pedido.getEnunciado());
        pergunta.setDificuldade(pedido.getDificuldade());
        pergunta.setTempoLimiteSegundos(pedido.getTempoLimiteSegundos());
        pergunta.setOrdem(ordem);

        List<QuizAlternativa> alternativas = new ArrayList<>();
        int indice = 0;
        for (CriarAlternativaDto dadosAlternativa : pedido.getAlternativas()) {
            QuizAlternativa alternativa = new QuizAlternativa();
            alternativa.setPergunta(pergunta);
            alternativa.setLetra(LETRAS[indice]);
            alternativa.setTexto(dadosAlternativa.getTexto());
            alternativa.setCorreta(dadosAlternativa.isCorreta());
            alternativa.setTotalRespostas(0);
            alternativas.add(alternativa);
            indice++;
        }
        pergunta.setAlternativas(alternativas);

        return pergunta;
    }

    private int calcularTempoRestante(QuizPergunta pergunta, LocalDateTime iniciadaEm) {
        if (iniciadaEm == null) {
            return 0;
        }
        long segundosDecorridos = Duration.between(iniciadaEm, LocalDateTime.now()).getSeconds();
        long segundosRestantes = pergunta.getTempoLimiteSegundos() - segundosDecorridos;
        if (segundosRestantes < 0) {
            return 0;
        }
        return (int) segundosRestantes;
    }

}
