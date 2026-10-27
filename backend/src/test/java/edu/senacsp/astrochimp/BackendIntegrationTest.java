package edu.senacsp.astrochimp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Inicia a API com MySQL e arquivos de teste, sem abrir o banco das aulas.
@SpringBootTest(properties = {
        "debug=false",
        "spring.datasource.url=jdbc:mysql://localhost:3306/astrochimp_test?createDatabaseIfNotExist=true&connectionTimeZone=UTC",
        "spring.datasource.hikari.jdbc-url=jdbc:mysql://localhost:3306/astrochimp_test?createDatabaseIfNotExist=true&connectionTimeZone=UTC",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "astrochimp.upload-dir=target/test-uploads"
})
// Permite enviar pedidos HTTP à API durante os testes, sem iniciar um servidor externo.
@AutoConfigureMockMvc
class BackendIntegrationTest {

    // Recebe o cliente de teste que envia os pedidos às rotas da API.
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Long professorId;
    private Long salaId;
    private String autenticacao;
    private String email;
    private String slug;
    private String tokenAluno;

    // Cria professor, sala e aluno novos antes de cada teste.
    @BeforeEach
    void prepararSala() throws Exception {
        assertEquals("astrochimp_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        slug = "professor-" + UUID.randomUUID();
        email = slug + "@exemplo.com";
        autenticacao = "Basic " + Base64.getEncoder().encodeToString(
                (email + ":Senha123").getBytes(StandardCharsets.UTF_8));
        ResultActions cadastro = mvc.perform(post("/api/professores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"email\":\"" + email
                        + "\",\"senha\":\"Senha123\",\"slugPerfilPublico\":\"" + slug + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist());
        professorId = numero(cadastro, "$.id");
        salaId = criarSala();
        tokenAluno = entrar(salaId);
    }

    // Marca o método como um teste que o Maven executa.
    @Test
    void atualizarPerfilPreservaEmailEEscondeDadosPrivados() throws Exception {
        mvc.perform(put("/api/professores/{id}", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":999999,\"nome\":\"Ana Silva\",\"descricao\":\"Java\",\"email\":\"outro@exemplo.com\",\"senha\":\"SenhaModificada123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/professores/{id}", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(professorId))
                .andExpect(jsonPath("$.senha").doesNotExist());
        String senhaAlterada = "Basic " + Base64.getEncoder().encodeToString(
                (email + ":SenhaModificada123").getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/professores/{id}", professorId)
                .header(HttpHeaders.AUTHORIZATION, senhaAlterada))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/professores/publico/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.materiais").isEmpty());
    }

    @Test
    void salaNaoRevelaTokensDosParticipantesNemDadosPrivadosDoProfessor() throws Exception {
        String tokenOutroAluno = entrar(salaId);
        for (String token : new String[]{tokenAluno, tokenOutroAluno}) {
            mvc.perform(get("/api/salas/{id}", salaId).header("X-Aluno-Token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nomeProfessor").value("Ana"))
                    .andExpect(jsonPath("$..token").isEmpty())
                    .andExpect(jsonPath("$..tokenAluno").isEmpty())
                    .andExpect(jsonPath("$..senha").isEmpty())
                    .andExpect(jsonPath("$..email").isEmpty())
                    .andExpect(jsonPath("$.alunos").doesNotExist());
        }
        mvc.perform(get("/api/professores/{id}", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$..senha").isEmpty());
    }

    @Test
    void criarSalaEEntrarIgnoramIdentidadeEstadoDatasETokenEnviadosPeloCliente() throws Exception {
        ResultActions criacao = mvc.perform(post("/api/salas")
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\":" + professorId + """
                        ,"nomeAula":"Sala segura","id":999999,"codigo":"FORJADO","status":"encerrada",
                        "nomeProfessor":"Intruso","criadaEm":"2000-01-01T00:00:00","expiraEm":"2000-01-01T00:00:00"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(999999)))
                .andExpect(jsonPath("$.codigo").value(not("FORJADO")))
                .andExpect(jsonPath("$.status").value("ativa"))
                .andExpect(jsonPath("$.nomeProfessor").value("Ana"))
                .andExpect(jsonPath("$.criadaEm").value(not("2000-01-01T00:00:00")))
                .andExpect(jsonPath("$.expiraEm").value(not("2000-01-01T00:00:00")))
                .andExpect(jsonPath("$.professorId").doesNotExist());
        Long novaSalaId = numero(criacao, "$.id");
        String codigo = JsonPath.read(criacao.andReturn().getResponse().getContentAsString(), "$.codigo");
        ResultActions entrada = mvc.perform(post("/api/salas/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + """
                        ","nomeAluno":"Maria","alunoId":999999,"tokenAluno":"token-forjado",
                        "sala":{"id":999999,"status":"encerrada","nomeProfessor":"Intruso"}}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sala.id").value(novaSalaId))
                .andExpect(jsonPath("$.sala.status").value("ativa"))
                .andExpect(jsonPath("$.sala.nomeProfessor").value("Ana"))
                .andExpect(jsonPath("$.alunoId").value(not(999999)))
                .andExpect(jsonPath("$.tokenAluno").value(not("token-forjado")))
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.nomeAluno").doesNotExist());
        String tokenGerado = JsonPath.read(entrada.andReturn().getResponse().getContentAsString(), "$.tokenAluno");
        mvc.perform(get("/api/salas/{id}", novaSalaId).header("X-Aluno-Token", "token-forjado"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/salas/{id}/duvidas", novaSalaId).header("X-Aluno-Token", tokenGerado)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Minha dúvida\",\"autorId\":999999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorId").value(numero(entrada, "$.alunoId")))
                .andExpect(jsonPath("$.autorNome").value("Maria"));
    }

    @Test
    void duvidasMantemAnonimatoVotoUnicoEOrdenacao() throws Exception {
        ResultActions criacao = mvc.perform(post("/api/salas/{id}/duvidas", salaId)
                .header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Primeira\",\"anonima\":true,\"autorId\":999,\"votos\":99,\"estado\":\"respondida\",\"fixada\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorId").value(nullValue()))
                .andExpect(jsonPath("$.autorNome").value(nullValue()))
                .andExpect(jsonPath("$.votos").value(0))
                .andExpect(jsonPath("$.estado").value("pendente"))
                .andExpect(jsonPath("$.fixada").value(false));
        Long primeiraId = numero(criacao, "$.id");
        Long segundaId = numero(mvc.perform(post("/api/salas/{id}/duvidas", salaId)
                .header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Segunda\"}"))
                .andExpect(status().isCreated()), "$.id");

        mvc.perform(post("/api/duvidas/{id}/votos", segundaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.votos").value(1));
        mvc.perform(post("/api/duvidas/{id}/votos", segundaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(segundaId));
        String tokenOutroAluno = entrar(salaId);
        mvc.perform(get("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenOutroAluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(primeiraId))
                .andExpect(jsonPath("$[1].autorId").value(nullValue()))
                .andExpect(jsonPath("$[1].autorNome").value(nullValue()))
                .andExpect(jsonPath("$..token").isEmpty())
                .andExpect(jsonPath("$..tokenAluno").isEmpty());

        mvc.perform(patch("/api/duvidas/{id}/fixacao", primeiraId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fixada\":true,\"texto\":\"Ignorar\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.texto").value("Primeira"));
        mvc.perform(get("/api/salas/{id}/duvidas/professor", salaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(primeiraId))
                .andExpect(jsonPath("$[0].autorNome").value("Joao"));
        mvc.perform(patch("/api/duvidas/{id}/estado", primeiraId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"respondida\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("respondida"));
        mvc.perform(patch("/api/duvidas/{id}/estado", primeiraId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void materiaisSaoFixadosPorSalaEPublicadosExplicitamente() throws Exception {
        Long materialId = numero(mvc.perform(post("/api/professores/{id}/materiais", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Aula\",\"tipo\":\"link\",\"conteudoOuUrl\":\"https://example.com\",\"publico\":true,\"fixado\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.publico").value(false))
                .andExpect(jsonPath("$.fixado").value(false)), "$.id");
        Long outraSalaId = criarSala();
        for (Long id : new Long[]{salaId, outraSalaId}) {
            mvc.perform(post("/api/salas/{sala}/materiais/{material}", id, materialId)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        }
        mvc.perform(post("/api/salas/{sala}/materiais/{material}", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isConflict());
        mvc.perform(patch("/api/salas/{sala}/materiais/{material}/fixacao", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fixado\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fixado").value(true));
        mvc.perform(get("/api/salas/{id}/materiais", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].fixado").value(true));
        mvc.perform(get("/api/salas/{id}/materiais", outraSalaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].fixado").value(false));
        mvc.perform(patch("/api/salas/{sala}/materiais/{material}/fixacao", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fixado\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/materiais", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].fixado").value(false));
        mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", professorId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"publico\":true,\"titulo\":\"Ignorar\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publico").value(true))
                .andExpect(jsonPath("$.titulo").value("Aula"));
        mvc.perform(get("/api/professores/publico/{slug}", slug))
                .andExpect(status().isOk()).andExpect(jsonPath("$.materiais[0].id").value(materialId));
    }

    @Test
    void uploadEDownloadPreservamConteudoENome() throws Exception {
        byte[] conteudo = "Material da aula".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "aula.txt", "text/plain", conteudo);
        Long materialId = numero(mvc.perform(multipart("/api/salas/{id}/materiais/upload", salaId)
                .file(arquivo).param("titulo", "Arquivo da aula")
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("arquivo")), "$.id");
        String rotaDownload = "/api/materiais/" + materialId + "/arquivo";
        mvc.perform(get("/api/salas/{id}/materiais", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conteudoOuUrl").value(rotaDownload))
                .andExpect(jsonPath("$[0].detalhe").value("aula.txt"));
        mvc.perform(get("/api/professores/{id}/materiais", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conteudoOuUrl").value(rotaDownload));
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId)
                .param("salaId", salaId.toString()).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aula.txt\""))
                .andExpect(content().bytes(conteudo));
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId))
                .andExpect(status().isUnauthorized());
        Long outraSalaId = criarSala();
        String tokenOutraSala = entrar(outraSalaId);
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId)
                .param("salaId", salaId.toString()).header("X-Aluno-Token", tokenOutraSala))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId)
                .param("salaId", outraSalaId.toString()).header("X-Aluno-Token", tokenOutraSala))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", professorId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"publico\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/professores/publico/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..email").isEmpty())
                .andExpect(jsonPath("$..senha").isEmpty())
                .andExpect(jsonPath("$.materiais[0].conteudoOuUrl").value(rotaDownload));
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aula.txt\""))
                .andExpect(content().bytes(conteudo));
    }

    @Test
    void quizMantemOrdemEscondeGabaritoESeparaSessoes() throws Exception {
        ResultActions criacao = criarQuiz();
        Long quizId = numero(criacao, "$.id");
        Long alternativaId = numero(criacao, "$.perguntas[0].alternativas[0].id");
        Long alternativaOutraPergunta = numero(criacao, "$.perguntas[1].alternativas[0].id");
        mvc.perform(get("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perguntas[0].ordem").value(1))
                .andExpect(jsonPath("$.perguntas[0].alternativas[0].letra").value("A"))
                .andExpect(jsonPath("$.perguntas[0].alternativas[0].correta").value(true));
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        ResultActions perguntaInicial = mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroPergunta").value(1))
                .andExpect(jsonPath("$.totalPerguntas").value(2))
                .andExpect(jsonPath("$.tempoRestanteSegundos").value(greaterThan(0)))
                .andExpect(jsonPath("$.rodadaId").isNotEmpty())
                .andExpect(jsonPath("$.alternativas[*].correta").doesNotExist());
        String rodadaInicial = JsonPath.read(perguntaInicial.andReturn().getResponse().getContentAsString(), "$.rodadaId");
        responder(alternativaOutraPergunta).andExpect(status().isBadRequest());
        responder(alternativaId).andExpect(status().isOk());
        responder(alternativaId).andExpect(status().isConflict());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(1))
                .andExpect(jsonPath("$.resultados[0].totalRespostas").value(1));
        mvc.perform(delete("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/salas/{id}/quiz/proxima-pergunta", salaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroPergunta").value(2));
        mvc.perform(post("/api/salas/{id}/quiz/proxima-pergunta", salaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(0));
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rodadaId").value(not(rodadaInicial)));
        responder(alternativaId).andExpect(status().isOk());
    }

    @Test
    void quizIgnoraDadosInternosENaoDevolveDadosDoProfessor() throws Exception {
        ResultActions criacao = mvc.perform(post("/api/professores/{id}/quizzes", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":999999,"titulo":"Quiz seguro","salvarNoPerfil":true,
                         "criadoEm":"2000-01-01T00:00:00","professor":{"id":999999,"senha":"segredo"},
                         "perguntas":[{"id":999999,"enunciado":"Pergunta","tempoLimiteSegundos":300,
                           "ordem":99,"quiz":{"id":999999},"alternativas":[
                             {"id":999999,"letra":"Z","texto":"Certa","correta":true,"pergunta":{"id":999999}},
                             {"texto":"Errada","correta":false}]}]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(999999)))
                .andExpect(jsonPath("$.salvoNoPerfil").value(true))
                .andExpect(jsonPath("$.perguntas[0].id").value(not(999999)))
                .andExpect(jsonPath("$.perguntas[0].ordem").value(1))
                .andExpect(jsonPath("$.perguntas[0].alternativas[0].id").value(not(999999)))
                .andExpect(jsonPath("$.perguntas[0].alternativas[0].letra").value("A"));
        Long quizId = numero(criacao, "$.id");
        assertEquals(professorId, jdbc.queryForObject("SELECT professor_id FROM quiz WHERE id = ?", Long.class, quizId));
        for (ResultActions resposta : new ResultActions[]{criacao,
                mvc.perform(get("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                        .andExpect(status().isOk())}) {
            resposta.andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$.professor").doesNotExist())
                    .andExpect(jsonPath("$.criadoEm").doesNotExist())
                    .andExpect(jsonPath("$.perguntas[0].length()").value(6))
                    .andExpect(jsonPath("$.perguntas[0].quiz").doesNotExist())
                    .andExpect(jsonPath("$.perguntas[0].alternativas[0].length()").value(4))
                    .andExpect(jsonPath("$.perguntas[0].alternativas[0].pergunta").doesNotExist());
        }
        mvc.perform(get("/api/quizzes/{id}", quizId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outro\",\"email\":\"outro-" + email + "\",\"senha\":\"Senha123\"}"))
                .andExpect(status().isCreated());
        String autenticacaoOutro = "Basic " + Base64.getEncoder().encodeToString(
                ("outro-" + email + ":Senha123").getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacaoOutro))
                .andExpect(status().isForbidden());
    }

    @Test
    void mesmoQuizEmDuasSalasContaSomenteRespostasDaRodadaDeCadaSala() throws Exception {
        ResultActions quiz = criarQuiz();
        Long quizId = numero(quiz, "$.id");
        Long alternativaId = numero(quiz, "$.perguntas[0].alternativas[0].id");
        Long outraSalaId = criarSala();
        String tokenOutraSala = entrar(outraSalaId);
        for (Long id : new Long[]{salaId, outraSalaId}) {
            mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", id, quizId)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        }
        responder(alternativaId).andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", outraSalaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(0))
                .andExpect(jsonPath("$.resultados[0].totalRespostas").value(0));
        mvc.perform(post("/api/salas/{id}/quiz/resposta", outraSalaId)
                .header("X-Aluno-Token", tokenOutraSala).contentType(MediaType.APPLICATION_JSON)
                .content("{\"alternativaId\":" + alternativaId + "}"))
                .andExpect(status().isOk());
        for (Long id : new Long[]{salaId, outraSalaId}) {
            mvc.perform(get("/api/salas/{id}/quiz/resultado", id)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(1))
                    .andExpect(jsonPath("$.resultados[0].totalRespostas").value(1));
        }
        mvc.perform(post("/api/salas/{id}/quiz/encerrar", salaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(0));
        mvc.perform(get("/api/salas/{id}/quiz/resultado", outraSalaId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(1));
    }

    @Test
    void criarEEnviarQuizMantemPerguntaDoAlunoSemGabarito() throws Exception {
        mvc.perform(post("/api/salas/{id}/quiz/criar-e-enviar", salaId)
                .param("professorId", professorId.toString())
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"Na hora","salvarNoPerfil":false,"perguntas":[
                          {"enunciado":"Pergunta","tempoLimiteSegundos":300,"alternativas":[
                            {"texto":"Certa","correta":true},{"texto":"Errada","correta":false}]}]}
                        """))
                .andExpect(status().isOk());
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enunciado").value("Pergunta"))
                .andExpect(jsonPath("$.alternativas[*].correta").doesNotExist());
        mvc.perform(get("/api/professores/{id}/quizzes", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void quizPodeSerListadoSalvoEExcluidoSemRespostas() throws Exception {
        Long quizId = numero(criarQuiz(), "$.id");
        mvc.perform(get("/api/professores/{id}/quizzes", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(quizId))
                .andExpect(jsonPath("$[0].perguntas.length()").value(2));
        mvc.perform(patch("/api/quizzes/{id}/salvar-no-perfil", quizId).param("salvar", "false")
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.salvoNoPerfil").value(false));
        mvc.perform(get("/api/professores/{id}/quizzes", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/professores/{id}/quizzes", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Sem perguntas\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quizRecusaPerguntasInvalidasERespostaForaDoTempo() throws Exception {
        String[] perguntasInvalidas = {
                "{\"enunciado\":\"Pergunta\",\"tempoLimiteSegundos\":0,\"alternativas\":[{\"texto\":\"A\",\"correta\":true},{\"texto\":\"B\"}]}",
                "{\"enunciado\":\"Pergunta\",\"tempoLimiteSegundos\":30,\"alternativas\":[{\"texto\":\"A\",\"correta\":true}]}",
                "{\"enunciado\":\"Pergunta\",\"tempoLimiteSegundos\":30,\"alternativas\":[{\"texto\":\"A\"},{\"texto\":\"B\"}]}",
                "{\"enunciado\":\"Pergunta\",\"tempoLimiteSegundos\":30,\"alternativas\":[{\"texto\":\"A\",\"correta\":true},{\"texto\":\"B\",\"correta\":true}]}",
                "{\"enunciado\":\"Pergunta\",\"tempoLimiteSegundos\":30,\"alternativas\":[null,{\"texto\":\"B\",\"correta\":true}]}"
        };
        for (String pergunta : perguntasInvalidas) {
            mvc.perform(post("/api/professores/{id}/quizzes", professorId)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"Inválido\",\"salvarNoPerfil\":true,\"perguntas\":[" + pergunta + "]}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/professores/{id}/quizzes", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        ResultActions quiz = criarQuiz();
        Long quizId = numero(quiz, "$.id");
        Long alternativaId = numero(quiz, "$.perguntas[0].alternativas[0].id");
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        // Simula que a pergunta começou há dez minutos, sem fazer o teste esperar esse tempo.
        jdbc.update("UPDATE sala SET quiz_pergunta_iniciada_em = ? WHERE id = ?",
                java.time.LocalDateTime.now().minusMinutes(10), salaId);
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tempoRestanteSegundos").value(0))
                .andExpect(jsonPath("$..correta").isEmpty());
        responder(alternativaId).andExpect(status().isConflict());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(0));
    }

    @Test
    void cronometroESalaEncerradaPreservamRegras() throws Exception {
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"segundos\":120,\"segundosRestantes\":999999,\"ativo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosTotais").value(120))
                .andExpect(jsonPath("$.segundosRestantes").value(not(999999)))
                .andExpect(jsonPath("$.segundos").doesNotExist())
                .andExpect(jsonPath("$.ativo").value(true));
        mvc.perform(get("/api/salas/{id}/cronometro", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosRestantes").value(greaterThan(0)));
        mvc.perform(patch("/api/salas/{id}/encerrar", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("encerrada"));
        mvc.perform(get("/api/salas/{id}/cronometro", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosTotais").value(nullValue()))
                .andExpect(jsonPath("$.segundosRestantes").value(0)).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Tarde demais\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void acessosExigemProfessorDonoOuAlunoDaSala() throws Exception {
        mvc.perform(get("/api/salas/{id}/duvidas/professor", salaId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", "invalido"))
                .andExpect(status().isUnauthorized());
        Long outraSalaId = criarSala();
        mvc.perform(get("/api/salas/{id}/duvidas", outraSalaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isForbidden());
        Long outroProfessorId = numero(mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outro\",\"email\":\"outro-" + email + "\",\"senha\":\"Senha123\"}"))
                .andExpect(status().isCreated()), "$.id");
        mvc.perform(get("/api/professores/{id}", outroProfessorId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/professores/{id}/materiais", outroProfessorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Aula\",\"tipo\":\"codigo\",\"conteudoOuUrl\":\"Java\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void materialPreservaConteudoLongoComAcentosEEmojiNoMySql() throws Exception {
        String conteudo = "Explicação da aula: ação, coração e revisão 🚀\n".repeat(2000);
        Long materialId = numero(mvc.perform(post("/api/professores/{id}/materiais", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Revisão\",\"tipo\":\"codigo\",\"conteudoOuUrl\":\""
                        + conteudo.replace("\n", "\\n") + "\"}"))
                .andExpect(status().isCreated()), "$.id");
        mvc.perform(get("/api/professores/{id}/materiais", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(materialId))
                .andExpect(jsonPath("$[0].conteudoOuUrl").value(conteudo));
    }

    private Long criarSala() throws Exception {
        return numero(mvc.perform(post("/api/salas").header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\":" + professorId + ",\"nomeAula\":\"Java\"}"))
                .andExpect(status().isCreated()), "$.id");
    }

    @Test
    void cadastroNormalizaEmailESlugRecusaDuplicadosEDadosVazios() throws Exception {
        String novoEmail = "normalizado-" + email;
        String novoSlug = "novo-" + slug;
        mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"  Beatriz  \",\"email\":\"  " + novoEmail.toUpperCase()
                        + "  \",\"senha\":\"Senha123\",\"slugPerfilPublico\":\"  " + novoSlug.toUpperCase() + "  \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Beatriz"))
                .andExpect(jsonPath("$.email").value(novoEmail))
                .andExpect(jsonPath("$.slugPerfilPublico").value(novoSlug))
                .andExpect(jsonPath("$.senha").doesNotExist());
        for (String pedido : new String[]{
                "{\"nome\":\"Bia\",\"email\":\"" + novoEmail + "\",\"senha\":\"Senha123\"}",
                "{\"nome\":\"Bia\",\"email\":\"unico-" + novoEmail + "\",\"senha\":\"Senha123\",\"slugPerfilPublico\":\"" + novoSlug + "\"}"}) {
            mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON).content(pedido))
                    .andExpect(status().isConflict());
        }
        for (String pedido : new String[]{"{}", "{\"nome\":\" \",\"email\":\"a@b.com\",\"senha\":\"Senha123\"}",
                "{\"nome\":\"Bia\",\"email\":\"a@b.com\",\"senha\":\"Senha123\",\"slugPerfilPublico\":\"inválido!\"}"}) {
            mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON).content(pedido))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/professores/publico/{slug}", "ausente-" + slug))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/professores/{id}", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"slugPerfilPublico\":\"" + novoSlug + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/professores/publico/{slug}", slug)).andExpect(status().isOk());
    }

    @Test
    void salaInexistenteEntradaInvalidaEExpiracaoNaoCriamParticipacao() throws Exception {
        mvc.perform(get("/api/salas/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isNotFound());
        for (String pedido : new String[]{"{}", "{\"codigo\":\" \",\"nomeAluno\":\"Maria\"}",
                "{\"codigo\":\"ABC123\",\"nomeAluno\":\" \"}"}) {
            mvc.perform(post("/api/salas/entrar").contentType(MediaType.APPLICATION_JSON).content(pedido))
                    .andExpect(status().is4xxClientError());
        }
        String codigo = jdbc.queryForObject("SELECT codigo FROM sala WHERE id = ?", String.class, salaId);
        jdbc.update("UPDATE sala SET expira_em = ? WHERE id = ?", java.time.LocalDateTime.now().minusMinutes(1), salaId);
        Integer alunosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM aluno", Integer.class);
        mvc.perform(post("/api/salas/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + "\",\"nomeAluno\":\"Maria\"}"))
                .andExpect(status().isConflict());
        assertEquals(alunosAntes, jdbc.queryForObject("SELECT COUNT(*) FROM aluno", Integer.class));
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"segundos\":30}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Pergunta\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cronometroRecusaDuracoesInvalidasExpiraEPermiteParar() throws Exception {
        for (String pedido : new String[]{"{}", "{\"segundos\":0}", "{\"segundos\":-1}", "{\"segundos\":2147483648}"}) {
            mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                    .contentType(MediaType.APPLICATION_JSON).content(pedido)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"segundos\":30}"))
                .andExpect(status().isOk());
        jdbc.update("UPDATE sala SET cronometro_iniciado_em = ? WHERE id = ?",
                java.time.LocalDateTime.now().minusMinutes(2), salaId);
        mvc.perform(get("/api/salas/{id}/cronometro", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosRestantes").value(0))
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(delete("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosTotais").value(nullValue()));
        mvc.perform(get("/api/salas/{id}/cronometro", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosTotais").value(nullValue()));
    }

    @Test
    void duvidaAceita280CaracteresRecusa281EPermiteTodosOsEstados() throws Exception {
        String texto = "á".repeat(280);
        Long duvidaId = numero(mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + texto + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.texto").value(texto)), "$.id");
        for (String invalido : new String[]{"", " ", "x".repeat(281)}) {
            mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + invalido + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        for (String estado : new String[]{"respondida", "arquivada", "pendente"}) {
            mvc.perform(patch("/api/duvidas/{id}/estado", duvidaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(estado));
        }
        mvc.perform(post("/api/duvidas/{id}/votos", duvidaId).header("X-Aluno-Token", entrar(criarSala())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/duvidas/{id}/votos", Long.MAX_VALUE).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isNotFound());
    }

    @Test
    void outroProfessorNaoAlteraSalaDuvidaMaterialOuQuizDoDono() throws Exception {
        String outroLogin = cadastrarOutroProfessor();
        Long outroProfessorId = jdbc.queryForObject("SELECT id FROM professor WHERE email = ?", Long.class, "auditoria-" + email);
        Long quizId = numero(criarQuiz(), "$.id");
        Long duvidaId = numero(mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Dúvida\"}"))
                .andExpect(status().isCreated()), "$.id");
        Long materialId = numero(criarMaterial("Material", "codigo", "Java"), "$.id");
        mvc.perform(patch("/api/salas/{id}/encerrar", salaId).header(HttpHeaders.AUTHORIZATION, outroLogin))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/duvidas/{id}/estado", duvidaId).header(HttpHeaders.AUTHORIZATION, outroLogin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"respondida\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/duvidas/{id}/fixacao", duvidaId).header(HttpHeaders.AUTHORIZATION, outroLogin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fixada\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/salas/{sala}/materiais/{material}", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, outroLogin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/professores/{id}/materiais", professorId).header(HttpHeaders.AUTHORIZATION, outroLogin))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", professorId, materialId)
                .header(HttpHeaders.AUTHORIZATION, outroLogin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"publico\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, outroLogin))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/quizzes/{id}/salvar-no-perfil", quizId).param("salvar", "false")
                .header(HttpHeaders.AUTHORIZATION, outroLogin)).andExpect(status().isForbidden());
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, outroLogin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/salas/{id}/duvidas/professor", salaId).header(HttpHeaders.AUTHORIZATION, outroLogin))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", outroProfessorId, materialId)
                .header(HttpHeaders.AUTHORIZATION, outroLogin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"publico\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, outroLogin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"segundos\":30}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header(HttpHeaders.AUTHORIZATION, outroLogin))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/salas/{id}/quiz/encerrar", salaId).header(HttpHeaders.AUTHORIZATION, outroLogin))
                .andExpect(status().isForbidden());
    }

    @Test
    void materiaisInvalidosNaoSaoSalvosEArquivoDoPerfilPodeSerRetiradoDoPublico() throws Exception {
        for (String pedido : new String[]{"{}", "{\"titulo\":\"Aula\",\"tipo\":\"arquivo\",\"conteudoOuUrl\":\"caminho\"}",
                "{\"titulo\":\" \",\"tipo\":\"codigo\",\"conteudoOuUrl\":\"Java\"}"}) {
            mvc.perform(post("/api/professores/{id}/materiais", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                    .contentType(MediaType.APPLICATION_JSON).content(pedido)).andExpect(status().isBadRequest());
        }
        MockMultipartFile vazio = new MockMultipartFile("arquivo", "vazio.txt", "text/plain", new byte[0]);
        mvc.perform(multipart("/api/professores/{id}/materiais/upload", professorId).file(vazio)
                .param("titulo", "Vazio").header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isBadRequest());
        byte[] conteudo = "conteúdo".getBytes(StandardCharsets.UTF_8);
        Long arquivoId = numero(mvc.perform(multipart("/api/professores/{id}/materiais/upload", professorId)
                .file(new MockMultipartFile("arquivo", "../aula.txt", "text/plain", conteudo))
                .param("titulo", "Aula").header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isCreated()), "$.id");
        mvc.perform(get("/api/materiais/{id}/arquivo", arquivoId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(content().bytes(conteudo));
        for (boolean publico : new boolean[]{true, false}) {
            mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", professorId, arquivoId)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"publico\":" + publico + "}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/materiais/{id}/arquivo", arquivoId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/professores/publico/{slug}", slug))
                .andExpect(status().isOk()).andExpect(jsonPath("$.materiais").isEmpty());
        Long codigoId = numero(criarMaterial("Código", "codigo", "System.out.println(1);"), "$.id");
        mvc.perform(get("/api/materiais/{id}/arquivo", codigoId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/salas/{sala}/materiais/{material}/fixacao", salaId, codigoId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fixado\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quizSemAndamentoRecusaAcoesEDuplicacaoDeInicio() throws Exception {
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/salas/{id}/quiz/proxima-pergunta", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isNotFound());
        responder(Long.MAX_VALUE).andExpect(status().isNotFound());
        Long quizId = numero(criarQuiz(), "$.id");
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isConflict());
        mvc.perform(delete("/api/quizzes/{id}", quizId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/salas/{id}/quiz/resposta", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        responder(Long.MAX_VALUE).andExpect(status().isNotFound());
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/salas/{id}/quiz/proxima-pergunta", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void camposLongosDoProfessorSaoRecusadosAntesDeChegarAoMySql() throws Exception {
        for (String campo : new String[]{"nome", "email", "slugPerfilPublico"}) {
            String nome = campo.equals("nome") ? "x".repeat(256) : "Ana";
            String novoEmail = campo.equals("email") ? "x".repeat(256) + "@exemplo.com" : "limite-" + email;
            String novoSlug = campo.equals("slugPerfilPublico") ? "x".repeat(256) : "limite-" + slug;
            mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"" + nome + "\",\"email\":\"" + novoEmail
                            + "\",\"senha\":\"Senha123\",\"slugPerfilPublico\":\"" + novoSlug + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/professores/{id}", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"fotoUrl\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarMesmoSlugComEspacosEMaiusculasMantemOEndereco() throws Exception {
        mvc.perform(put("/api/professores/{id}", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"slugPerfilPublico\":\"  " + slug.toUpperCase() + "  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slugPerfilPublico").value(slug));
        mvc.perform(get("/api/professores/publico/{slug}", slug))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(professorId));
    }

    @Test
    void senhaAcimaDoLimiteEmBytesERecusadaSemErroInterno() throws Exception {
        // As duas senhas passam de 72 bytes UTF-8; a segunda tem apenas 37 caracteres.
        for (String senha : new String[]{"x".repeat(73), "á".repeat(37)}) {
            mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Ana\",\"email\":\"senha-longa-" + email + "\",\"senha\":\"" + senha + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void nomesLongosDeSalaEAlunoNaoEstouramColunasMySql() throws Exception {
        mvc.perform(post("/api/salas").header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\":" + professorId + ",\"nomeAula\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        String codigo = jdbc.queryForObject("SELECT codigo FROM sala WHERE id = ?", String.class, salaId);
        mvc.perform(post("/api/salas/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + "\",\"nomeAluno\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tituloEDetalheLongosDoMaterialNaoEstouramColunasMySql() throws Exception {
        for (String campo : new String[]{"titulo", "detalhe"}) {
            String titulo = campo.equals("titulo") ? "x".repeat(256) : "Material";
            String detalhe = campo.equals("detalhe") ? "x".repeat(256) : "Exemplo";
            mvc.perform(post("/api/professores/{id}/materiais", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"" + titulo + "\",\"detalhe\":\"" + detalhe
                            + "\",\"tipo\":\"codigo\",\"conteudoOuUrl\":\"Java\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void textoLongoDoQuizNaoEstouraColunasMySql() throws Exception {
        for (String campo : new String[]{"titulo", "enunciado", "dificuldade", "texto"}) {
            String longo = "x".repeat(256);
            String titulo = campo.equals("titulo") ? longo : "Quiz";
            String enunciado = campo.equals("enunciado") ? longo : "Pergunta";
            String dificuldade = campo.equals("dificuldade") ? longo : "facil";
            String alternativa = campo.equals("texto") ? longo : "A";
            mvc.perform(post("/api/professores/{id}/quizzes", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"" + titulo + "\",\"perguntas\":[{\"enunciado\":\"" + enunciado
                            + "\",\"dificuldade\":\"" + dificuldade + "\",\"tempoLimiteSegundos\":30,\"alternativas\":["
                            + "{\"texto\":\"" + alternativa + "\",\"correta\":true},{\"texto\":\"B\"}]}]}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void operacoesDeFixacaoEPublicacaoExigemCampoExplicito() throws Exception {
        Long duvidaId = numero(mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Dúvida\"}"))
                .andExpect(status().isCreated()), "$.id");
        Long materialId = numero(criarMaterial("Material", "codigo", "Java"), "$.id");
        mvc.perform(post("/api/salas/{sala}/materiais/{material}", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        int estadoDuvida = mvc.perform(patch("/api/duvidas/{id}/fixacao", duvidaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        int estadoFixacao = mvc.perform(patch("/api/salas/{sala}/materiais/{material}/fixacao", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        int estadoPublicacao = mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", professorId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertArrayEquals(new int[]{400, 400, 400}, new int[]{estadoDuvida, estadoFixacao, estadoPublicacao},
                "Dúvida, fixação de material e publicação exigem o campo booleano no pedido");
        for (String pedido : new String[]{"{\"fixada\":true}", "{\"fixada\":false}"}) {
            mvc.perform(patch("/api/duvidas/{id}/fixacao", duvidaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                    .contentType(MediaType.APPLICATION_JSON).content(pedido)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].fixada").value(false));
        for (boolean valor : new boolean[]{true, false}) {
            mvc.perform(patch("/api/salas/{sala}/materiais/{material}/fixacao", salaId, materialId)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fixado\":" + valor + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.fixado").value(valor));
            mvc.perform(patch("/api/professores/{professor}/materiais/{material}/publicacao", professorId, materialId)
                    .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"publico\":" + valor + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.publico").value(valor));
        }
    }

    @Test
    void doisVotosSimultaneosDoMesmoAlunoContamUmaVez() throws Exception {
        Long duvidaId = numero(mvc.perform(post("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Dúvida\"}"))
                .andExpect(status().isCreated()), "$.id");
        int[] estados = executarJuntos(
                post("/api/duvidas/{id}/votos", duvidaId).header("X-Aluno-Token", tokenAluno),
                post("/api/duvidas/{id}/votos", duvidaId).header("X-Aluno-Token", tokenAluno));
        Arrays.sort(estados);
        assertArrayEquals(new int[]{200, 409}, estados);
        mvc.perform(get("/api/salas/{id}/duvidas", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].votos").value(1));
    }

    @Test
    void duasAlternativasSimultaneasDoMesmoAlunoContamUmaResposta() throws Exception {
        ResultActions quiz = criarQuiz();
        Long quizId = numero(quiz, "$.id");
        Long primeiraAlternativa = numero(quiz, "$.perguntas[0].alternativas[0].id");
        Long segundaAlternativa = numero(quiz, "$.perguntas[0].alternativas[1].id");
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isOk());
        int[] estados = executarJuntos(
                post("/api/salas/{id}/quiz/resposta", salaId).header("X-Aluno-Token", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alternativaId\":" + primeiraAlternativa + "}"),
                post("/api/salas/{id}/quiz/resposta", salaId).header("X-Aluno-Token", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alternativaId\":" + segundaAlternativa + "}"));
        Arrays.sort(estados);
        assertArrayEquals(new int[]{200, 409}, estados);
        mvc.perform(get("/api/salas/{id}/quiz/resultado", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRespondentes").value(1));
    }

    private int[] executarJuntos(MockHttpServletRequestBuilder primeiro, MockHttpServletRequestBuilder segundo) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontos = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            // Os dois pedidos esperam o mesmo sinal antes de chamar a API, cada um em uma thread.
            Future<Integer> respostaPrimeiro = executor.submit(() -> {
                prontos.countDown();
                if (!iniciar.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("O primeiro pedido não recebeu o sinal de início");
                }
                return mvc.perform(primeiro).andReturn().getResponse().getStatus();
            });
            Future<Integer> respostaSegundo = executor.submit(() -> {
                prontos.countDown();
                if (!iniciar.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("O segundo pedido não recebeu o sinal de início");
                }
                return mvc.perform(segundo).andReturn().getResponse().getStatus();
            });
            assertTrue(prontos.await(5, TimeUnit.SECONDS), "Os dois pedidos precisam estar prontos");
            iniciar.countDown();
            return new int[]{respostaPrimeiro.get(15, TimeUnit.SECONDS), respostaSegundo.get(15, TimeUnit.SECONDS)};
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "As threads dos pedidos precisam terminar");
        }
    }

    @Test
    // Mantém o upload e seu registro na transação do teste, que será desfeita para conferir a remoção do arquivo.
    @Transactional
    void desfazerTransacaoDoUploadRemoveArquivoGravado() throws Exception {
        Long materialId = numero(mvc.perform(multipart("/api/professores/{id}/materiais/upload", professorId)
                .file(new MockMultipartFile("arquivo", "rollback.txt", "text/plain", new byte[]{1, 2, 3}))
                .param("titulo", "Arquivo temporário").header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isCreated()), "$.id");
        Path arquivo = Path.of(jdbc.queryForObject("SELECT conteudo_ou_url FROM material WHERE id = ?", String.class, materialId));
        assertTrue(arquivo.toAbsolutePath().normalize().startsWith(Path.of("target/test-uploads").toAbsolutePath().normalize()));
        assertTrue(Files.exists(arquivo));
        TestTransaction.flagForRollback();
        TestTransaction.end();
        assertFalse(Files.exists(arquivo));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM material WHERE id = ?", Integer.class, materialId));
    }

    @Test
    void salaEncerradaRecusaNovaEntradaMaterialEInicioDeQuiz() throws Exception {
        Long materialId = numero(criarMaterial("Material", "codigo", "Java"), "$.id");
        Long quizId = numero(criarQuiz(), "$.id");
        String codigo = jdbc.queryForObject("SELECT codigo FROM sala WHERE id = ?", String.class, salaId);
        mvc.perform(patch("/api/salas/{id}/encerrar", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk());
        mvc.perform(post("/api/salas/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + "\",\"nomeAluno\":\"Maria\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/salas/{sala}/materiais/{material}", salaId, materialId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isConflict());
        mvc.perform(post("/api/salas/{sala}/quiz/enviar-existente/{quiz}", salaId, quizId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)).andExpect(status().isConflict());
    }

    @Test
    void professorConectadoEListaDeSalasPreservamOLoginEODono() throws Exception {
        mvc.perform(get("/api/professores/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/professores/me").header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(professorId))
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/professores/{id}/salas", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(salaId))
                .andExpect(jsonPath("$..token").isEmpty()).andExpect(jsonPath("$..senha").isEmpty());
        String outroAcesso = cadastrarOutroProfessor();
        mvc.perform(get("/api/professores/{id}/salas", professorId).header(HttpHeaders.AUTHORIZATION, outroAcesso))
                .andExpect(status().isForbidden());
    }

    @Test
    void quantidadeDeParticipantesVemDaSalaENaoDoPedido() throws Exception {
        entrar(salaId);
        mvc.perform(get("/api/salas/{id}", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAlunos").value(2))
                .andExpect(jsonPath("$.alunos").doesNotExist());
        mvc.perform(post("/api/salas").header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\":" + professorId + ",\"nomeAula\":\"Outra aula\",\"totalAlunos\":999}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAlunos").value(0));
    }

    @Test
    void previaWebPodeEnviarCabecalhoDeLoginSomenteDasOrigensLocais() throws Exception {
        mvc.perform(options("/api/professores/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"));
        mvc.perform(options("/api/professores/me")
                .header(HttpHeaders.ORIGIN, "http://origem-invalida.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private String cadastrarOutroProfessor() throws Exception {
        String outroEmail = "auditoria-" + email;
        mvc.perform(post("/api/professores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outro\",\"email\":\"" + outroEmail + "\",\"senha\":\"Senha123\"}"))
                .andExpect(status().isCreated());
        return "Basic " + Base64.getEncoder().encodeToString((outroEmail + ":Senha123").getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions criarMaterial(String titulo, String tipo, String conteudo) throws Exception {
        return mvc.perform(post("/api/professores/{id}/materiais", professorId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"" + titulo + "\",\"tipo\":\"" + tipo
                        + "\",\"conteudoOuUrl\":\"" + conteudo + "\"}"))
                .andExpect(status().isCreated());
    }

    private String entrar(Long id) throws Exception {
        String sala = mvc.perform(get("/api/salas/{id}", id).header(HttpHeaders.AUTHORIZATION, autenticacao))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(sala, "$.codigo");
        String entrada = mvc.perform(post("/api/salas/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"" + codigo + "\",\"nomeAluno\":\"Joao\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(entrada, "$.tokenAluno");
    }

    private ResultActions criarQuiz() throws Exception {
        return mvc.perform(post("/api/professores/{id}/quizzes", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo":"Revisao","salvarNoPerfil":true,"perguntas":[
                          {"enunciado":"Primeira","tempoLimiteSegundos":300,"ordem":99,"alternativas":[
                            {"texto":"Certa","correta":true,"letra":"Z"},{"texto":"Errada","correta":false}]},
                          {"enunciado":"Segunda","tempoLimiteSegundos":300,"ordem":1,"alternativas":[
                            {"texto":"Certa","correta":true},{"texto":"Errada","correta":false}]}]}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.perguntas[0].ordem").value(1))
                .andExpect(jsonPath("$.perguntas[1].ordem").value(2));
    }

    private ResultActions responder(Long alternativaId) throws Exception {
        return mvc.perform(post("/api/salas/{id}/quiz/resposta", salaId).header("X-Aluno-Token", tokenAluno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"alternativaId\":" + alternativaId
                        + ",\"texto\":\"Forjado\",\"correta\":true,\"alunoId\":999999,\"sessaoId\":\"forjada\"}"));
    }

    private Long numero(ResultActions resultado, String caminho) throws Exception {
        Number valor = JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), caminho);
        return valor.longValue();
    }
}
