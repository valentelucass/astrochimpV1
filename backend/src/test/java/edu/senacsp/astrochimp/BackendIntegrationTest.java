package edu.senacsp.astrochimp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "debug=false",
        "spring.datasource.url=jdbc:h2:mem:limpeza;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "astrochimp.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class BackendIntegrationTest {

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

    @BeforeEach
    void prepararSala() throws Exception {
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

    @Test
    void atualizarPerfilPreservaEmailEEscondeDadosPrivados() throws Exception {
        mvc.perform(put("/api/professores/{id}", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana Silva\",\"descricao\":\"Java\",\"email\":\"outro@exemplo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/professores/publico/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Silva"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.materiais").isEmpty());
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
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId)
                .param("salaId", salaId.toString()).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aula.txt\""))
                .andExpect(content().bytes(conteudo));
        mvc.perform(get("/api/materiais/{id}/arquivo", materialId))
                .andExpect(status().isUnauthorized());
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
        mvc.perform(get("/api/salas/{id}/quiz/pergunta-atual", salaId).header("X-Aluno-Token", tokenAluno))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroPergunta").value(1))
                .andExpect(jsonPath("$.totalPerguntas").value(2))
                .andExpect(jsonPath("$.tempoRestanteSegundos").value(greaterThan(0)))
                .andExpect(jsonPath("$.alternativas[*].correta").doesNotExist());
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
        responder(alternativaId).andExpect(status().isOk());
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
    void cronometroESalaEncerradaPreservamRegras() throws Exception {
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/salas/{id}/cronometro", salaId).header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"segundos\":120}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosTotais").value(120))
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
    void bancoAntigoAceitaCadastrosAposAplicarCompatibilidade() throws Exception {
        // Simula as colunas obrigatórias que existiam antes da limpeza.
        jdbc.execute("ALTER TABLE material ADD COLUMN IF NOT EXISTS fixado BOOLEAN DEFAULT FALSE NOT NULL");
        jdbc.execute("ALTER TABLE material ALTER COLUMN fixado DROP DEFAULT");
        jdbc.execute("ALTER TABLE quiz_alternativa ADD COLUMN IF NOT EXISTS total_respostas INTEGER DEFAULT 0 NOT NULL");
        jdbc.execute("ALTER TABLE quiz_alternativa ALTER COLUMN total_respostas DROP DEFAULT");
        ResourceDatabasePopulator script = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        script.execute(jdbc.getDataSource());
        script.execute(jdbc.getDataSource());

        mvc.perform(post("/api/professores/{id}/materiais", professorId)
                .header(HttpHeaders.AUTHORIZATION, autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Aula\",\"tipo\":\"codigo\",\"conteudoOuUrl\":\"Java\"}"))
                .andExpect(status().isCreated());
        criarQuiz().andExpect(status().isCreated());
    }

    private Long criarSala() throws Exception {
        return numero(mvc.perform(post("/api/salas").header(HttpHeaders.AUTHORIZATION, autenticacao)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\":" + professorId + ",\"nomeAula\":\"Java\"}"))
                .andExpect(status().isCreated()), "$.id");
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
                .contentType(MediaType.APPLICATION_JSON).content("{\"alternativaId\":" + alternativaId + "}"));
    }

    private Long numero(ResultActions resultado, String caminho) throws Exception {
        Number valor = JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), caminho);
        return valor.longValue();
    }
}
