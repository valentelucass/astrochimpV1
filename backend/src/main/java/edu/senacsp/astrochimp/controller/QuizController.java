package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.CriarQuizDto;
import edu.senacsp.astrochimp.dto.PerguntaAlunoDto;
import edu.senacsp.astrochimp.dto.QuizProfessorDto;
import edu.senacsp.astrochimp.dto.ResponderQuizDto;
import edu.senacsp.astrochimp.dto.ResultadoPerguntaDto;
import edu.senacsp.astrochimp.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/api/professores/{professorId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizProfessorDto criar(
            @PathVariable Long professorId,
            @RequestBody CriarQuizDto pedido) {
        return quizService.criarQuiz(professorId, pedido);
    }

    @GetMapping("/api/professores/{professorId}/quizzes")
    public List<QuizProfessorDto> listarSalvos(@PathVariable Long professorId) {
        return quizService.listarQuizzesDoProfessor(professorId);
    }

    @GetMapping("/api/quizzes/{quizId}")
    public QuizProfessorDto detalhar(@PathVariable Long quizId) {
        return quizService.buscar(quizId);
    }

    @PatchMapping("/api/quizzes/{quizId}/salvar-no-perfil")
    public QuizProfessorDto alternarSalvoNoPerfil(
            @PathVariable Long quizId,
            @RequestParam boolean salvar) {
        return quizService.alternarSalvoNoPerfil(quizId, salvar);
    }

    @DeleteMapping("/api/quizzes/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long quizId) {
        quizService.excluirQuiz(quizId);
    }

    @PostMapping("/api/salas/{salaId}/quiz/enviar-existente/{quizId}")
    public void enviarQuizExistente(@PathVariable Long salaId, @PathVariable Long quizId) {
        quizService.enviarQuizExistenteParaSala(salaId, quizId);
    }

    @PostMapping("/api/salas/{salaId}/quiz/criar-e-enviar")
    public void criarEEnviar(
            @PathVariable Long salaId,
            @RequestParam Long professorId,
            @RequestBody CriarQuizDto pedido) {
        quizService.criarEEnviarQuizParaSala(salaId, professorId, pedido);
    }

    @GetMapping("/api/salas/{salaId}/quiz/pergunta-atual")
    public PerguntaAlunoDto perguntaAtual(
            @PathVariable Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return quizService.obterPerguntaAtual(salaId, tokenAluno);
    }

    @PostMapping("/api/salas/{salaId}/quiz/proxima-pergunta")
    public void proximaPergunta(@PathVariable Long salaId) {
        quizService.avancarProximaPergunta(salaId);
    }

    @PostMapping("/api/salas/{salaId}/quiz/resposta")
    public void registrarResposta(
            @PathVariable Long salaId,
            @RequestHeader("X-Aluno-Token") String tokenAluno,
            @RequestBody ResponderQuizDto pedido) {
        quizService.registrarResposta(salaId, tokenAluno, pedido);
    }

    @GetMapping("/api/salas/{salaId}/quiz/resultado")
    public ResultadoPerguntaDto resultado(@PathVariable Long salaId) {
        return quizService.obterResultadoPerguntaAtual(salaId);
    }

    @PostMapping("/api/salas/{salaId}/quiz/encerrar")
    public void encerrar(@PathVariable Long salaId) {
        quizService.encerrarQuiz(salaId);
    }

}
