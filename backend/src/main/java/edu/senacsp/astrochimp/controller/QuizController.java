package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.AlternativaProfessorDto;
import edu.senacsp.astrochimp.dto.CriarQuizDto;
import edu.senacsp.astrochimp.dto.PerguntaAlunoDto;
import edu.senacsp.astrochimp.dto.PerguntaProfessorDto;
import edu.senacsp.astrochimp.dto.QuizProfessorDto;
import edu.senacsp.astrochimp.dto.ResponderQuizDto;
import edu.senacsp.astrochimp.dto.ResultadoPerguntaDto;
import edu.senacsp.astrochimp.entity.Quiz;
import edu.senacsp.astrochimp.entity.QuizAlternativa;
import edu.senacsp.astrochimp.entity.QuizPergunta;
import edu.senacsp.astrochimp.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;


// Essa classe tivemos bastante dificuldade para construir ela, confessamos que tivemos que
// buscar ajuda, não atoa mostra ser de Controllers a mais complexa e com estruturas maiores

@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }



    // Aqui mantivemos o mesmo que as outras, mas com algumas diferenças, 
    // como o ResponseEntity, que é usado para montar a resposta HTTP completa, 
    // com status, cabeçalhos e corpo da resposta
    @PostMapping("/api/professores/{professorId}/quizzes")
    public ResponseEntity<QuizProfessorDto> criar(
            @PathVariable Long professorId,
            @RequestBody CriarQuizDto pedido) {
        Quiz quiz = quizService.criarQuiz(professorId, pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(converterQuizParaDto(quiz));
    }


    // Nessa precisamos ter uma lista de quizzes, então 
    // criamos uma lista de QuizProfessorDto, e para cada quiz que o serviço retorna,
    // chamamos o método converterQuizParaDto para converter o quiz em um QuizProfessorDto
    @GetMapping("/api/professores/{professorId}/quizzes")
    public List<QuizProfessorDto> listarSalvos(@PathVariable Long professorId) {
        List<QuizProfessorDto> quizzes = new ArrayList<>();
        for (Quiz quiz : quizService.listarQuizzesDoProfessor(professorId)) { // aqui temos um for para percorrer a lista de quizzes que o serviço retorna
            quizzes.add(converterQuizParaDto(quiz));
        }
        return quizzes;
    }

    @GetMapping("/api/quizzes/{quizId}")
    public QuizProfessorDto detalhar(@PathVariable Long quizId) {
        return converterQuizParaDto(quizService.buscarPorId(quizId));
    }

    @PatchMapping("/api/quizzes/{quizId}/salvar-no-perfil")
    public QuizProfessorDto alternarSalvoNoPerfil(
            @PathVariable Long quizId,
            @RequestParam boolean salvar) {
        return converterQuizParaDto(quizService.alternarSalvoNoPerfil(quizId, salvar));
    }

    @DeleteMapping("/api/quizzes/{quizId}")
    public ResponseEntity<Void> excluir(@PathVariable Long quizId) {
        quizService.excluirQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/salas/{salaId}/quiz/enviar-existente/{quizId}")
    public ResponseEntity<Void> enviarQuizExistente(@PathVariable Long salaId, @PathVariable Long quizId) {
        quizService.enviarQuizExistenteParaSala(salaId, quizId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/salas/{salaId}/quiz/criar-e-enviar")
    public ResponseEntity<Void> criarEEnviar(
            @PathVariable Long salaId,
            @RequestParam Long professorId,
            @RequestBody CriarQuizDto pedido) {
        quizService.criarEEnviarQuizParaSala(salaId, professorId, pedido);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/salas/{salaId}/quiz/pergunta-atual")
    public PerguntaAlunoDto perguntaAtual(
            @PathVariable Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return quizService.obterPerguntaAtual(salaId, tokenAluno);
    }

    @PostMapping("/api/salas/{salaId}/quiz/proxima-pergunta")
    public ResponseEntity<Void> proximaPergunta(@PathVariable Long salaId) {
        quizService.avancarProximaPergunta(salaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/salas/{salaId}/quiz/resposta")
    public ResponseEntity<Void> registrarResposta(
            @PathVariable Long salaId,
            @RequestHeader("X-Aluno-Token") String tokenAluno,
            @RequestBody ResponderQuizDto pedido) {
        quizService.registrarResposta(salaId, tokenAluno, pedido);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/salas/{salaId}/quiz/resultado")
    public ResultadoPerguntaDto resultado(@PathVariable Long salaId) {
        return quizService.obterResultadoPerguntaAtual(salaId);
    }

    @PostMapping("/api/salas/{salaId}/quiz/encerrar")
    public ResponseEntity<Void> encerrar(@PathVariable Long salaId) {
        quizService.encerrarQuiz(salaId);
        return ResponseEntity.ok().build();
    }

    private QuizProfessorDto converterQuizParaDto(Quiz quiz) {
        List<QuizPergunta> perguntasOrdenadas = new ArrayList<>(quiz.getPerguntas());
        perguntasOrdenadas.sort(new Comparator<QuizPergunta>() {
            @Override
            public int compare(QuizPergunta primeira, QuizPergunta segunda) {
                return Integer.compare(primeira.getOrdem(), segunda.getOrdem());
            }
        });
        List<PerguntaProfessorDto> perguntas = new ArrayList<>();
        for (QuizPergunta pergunta : perguntasOrdenadas) {
            perguntas.add(converterPerguntaParaDto(pergunta));
        }

        return new QuizProfessorDto(
                quiz.getId(), quiz.getTitulo(), quiz.isSalvoNoPerfil(), perguntas);
    }

    private PerguntaProfessorDto converterPerguntaParaDto(QuizPergunta pergunta) {
        List<AlternativaProfessorDto> alternativas = new ArrayList<>();
        for (QuizAlternativa alternativa : pergunta.getAlternativas()) {
            AlternativaProfessorDto dadosAlternativa = new AlternativaProfessorDto(
                    alternativa.getId(), alternativa.getLetra(), alternativa.getTexto(), alternativa.isCorreta());
            alternativas.add(dadosAlternativa);
        }

        return new PerguntaProfessorDto(
                pergunta.getId(), pergunta.getEnunciado(), pergunta.getDificuldade(),
                pergunta.getTempoLimiteSegundos(), pergunta.getOrdem(), alternativas);
    }
}
