package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.PerguntaAlunoDto;
import edu.senacsp.astrochimp.dto.AlternativaAlunoDto;
import edu.senacsp.astrochimp.dto.ResultadoPerguntaDto;
import edu.senacsp.astrochimp.entity.Quiz;
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

// O Spring registra este controller para atender as rotas de quizzes e devolver os objetos de resposta em JSON.
@RestController
public class QuizController {

    private final QuizService quizService;

    // O Spring passa o QuizService criado por ele para este controller usar nas rotas.
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // @PostMapping liga o POST /api/professores/{professorId}/quizzes à criação do quiz.
    @PostMapping("/api/professores/{professorId}/quizzes")
    // Devolve 201 para avisar que o quiz foi criado.
    @ResponseStatus(HttpStatus.CREATED)
    public Quiz criar(
            // @PathVariable lê professorId no endereço; o service confere se é o professor que está logado.
            @PathVariable Long professorId,
            // Monta um Quiz com o título, as perguntas e as alternativas recebidos no JSON.
            @RequestBody Quiz pedido) {
        return quizService.criarQuiz(professorId, pedido);
    }

    // @GetMapping liga esta rota GET à consulta dos quizzes salvos no perfil do professor.
    @GetMapping("/api/professores/{professorId}/quizzes")
    public List<Quiz> listarSalvos(@PathVariable Long professorId) {
        return quizService.listarQuizzesDoProfessor(professorId);
    }

    @GetMapping("/api/quizzes/{quizId}")
    public Quiz detalhar(@PathVariable Long quizId) {
        return quizService.buscar(quizId);
    }

    // @PatchMapping liga esta rota PATCH à alteração de salvoNoPerfil do quiz.
    @PatchMapping("/api/quizzes/{quizId}/salvar-no-perfil")
    public Quiz alternarSalvoNoPerfil(
            @PathVariable Long quizId,
            // Lê salvar no endereço, como ?salvar=true, para decidir se o quiz fica no perfil.
            @RequestParam boolean salvar) {
        return quizService.alternarSalvoNoPerfil(quizId, salvar);
    }

    // @DeleteMapping liga o DELETE /api/quizzes/{quizId} ao pedido de exclusão do quiz.
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
            @RequestBody Quiz pedido) {
        quizService.criarEEnviarQuizParaSala(salaId, professorId, pedido);
    }

    @GetMapping("/api/salas/{salaId}/quiz/pergunta-atual")
    public PerguntaAlunoDto perguntaAtual(
            @PathVariable Long salaId,
            // @RequestHeader lê X-Aluno-Token; sem esse cabeçalho, o service exige o login do professor dono da sala.
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
            // alternativaId no JSON preenche id da alternativa que o aluno escolheu.
            @RequestBody AlternativaAlunoDto pedido) {
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
