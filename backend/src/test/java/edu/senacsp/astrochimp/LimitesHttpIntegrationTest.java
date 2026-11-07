package edu.senacsp.astrochimp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Verifica os limites pendentes pela rede, com Tomcat e MySQL reais exclusivos desta classe.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:mysql://localhost:3306/astrochimp_casos_20261102?createDatabaseIfNotExist=true&connectionTimeZone=UTC",
        "spring.datasource.hikari.jdbc-url=jdbc:mysql://localhost:3306/astrochimp_casos_20261102?createDatabaseIfNotExist=true&connectionTimeZone=UTC",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false"
})
@ContextConfiguration(initializers = LimitesHttpIntegrationTest.ConferirBancoVazio.class)
// Fecha o servidor e remove as tabelas de teste ao terminar esta classe.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LimitesHttpIntegrationTest {
    private static final String BANCO = "astrochimp_casos_20261102";
    private static final int DEZ_MIB = 10 * 1024 * 1024;
    private static final String BORDA = "AstroChimpFronteira";
    private static Path pastaArquivos;

    // A porta é escolhida pelo sistema; o cliente só envia dados ao servidor local deste teste.
    @LocalServerPort
    private int porta;
    @Autowired
    private JdbcTemplate jdbc;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private String login;
    private long professorId;
    private long salaId;
    private String token;

    // Interrompe a inicialização antes do Hibernate se o banco exclusivo já contiver alguma tabela.
    public static class ConferirBancoVazio implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext contexto) {
            var ambiente = contexto.getEnvironment();
            String url = ambiente.getRequiredProperty("spring.datasource.url");
            if (!url.startsWith("jdbc:mysql://localhost:3306/" + BANCO + "?")) {
                throw new IllegalStateException("URL não aponta para o banco exclusivo dos casos");
            }
            try (var conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/?connectionTimeZone=UTC",
                    ambiente.getRequiredProperty("spring.datasource.username"),
                    ambiente.getProperty("spring.datasource.password", ""));
                 var consulta = conexao.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?")) {
                consulta.setString(1, BANCO);
                try (var resultado = consulta.executeQuery()) {
                    resultado.next();
                    if (resultado.getInt(1) != 0) {
                        throw new IllegalStateException("Banco de casos contém tabelas; execução cancelada para preservar dados");
                    }
                }
                System.out.println("PREFLIGHT: " + BANCO + " sem tabelas; MySQL " + conexao.getMetaData().getDatabaseProductVersion());
            } catch (java.sql.SQLException erro) {
                throw new IllegalStateException("Não foi possível conferir o banco exclusivo", erro);
            }
        }
    }

    // Cada execução usa uma pasta nova; não apaga uploads de execuções anteriores.
    @DynamicPropertySource
    static void configurarArquivos(DynamicPropertyRegistry propriedades) throws Exception {
        pastaArquivos = Files.createDirectories(Path.of("target", "casos-test-uploads", UUID.randomUUID().toString()));
        propriedades.add("astrochimp.upload-dir", () -> pastaArquivos.toAbsolutePath().toString());
    }

    // Cria dados fictícios por HTTP para cada teste, sem compartilhar login ou sala com outros casos.
    @BeforeEach
    void prepararSala() throws Exception {
        assertEquals(BANCO, jdbc.queryForObject("SELECT DATABASE()", String.class));
        String email = "casos-" + UUID.randomUUID() + "@exemplo.com";
        login = "Basic " + Base64.getEncoder().encodeToString((email + ":Senha123").getBytes(StandardCharsets.UTF_8));
        String professor = json("POST", "/api/professores", "{\"nome\":\"Teste de limites\",\"email\":\"" + email
                + "\",\"senha\":\"Senha123\"}", null, 201);
        professorId = numero(professor, "$.id");
        String sala = json("POST", "/api/salas", "{\"professorId\":" + professorId + ",\"nomeAula\":\"Limites\"}", login, 201);
        salaId = numero(sala, "$.id");
        String codigo = JsonPath.read(sala, "$.codigo");
        String entrada = json("POST", "/api/salas/entrar", "{\"codigo\":\"" + codigo + "\",\"nomeAluno\":\"Aluno teste\"}", null, 200);
        token = JsonPath.read(entrada, "$.tokenAluno");
    }

    // @Test identifica cada cenário executado; várias verificações no método continuam sendo um único teste.
    @Test
    void duvidaComUmCaractereEPersistidaSemAlterarTexto() throws Exception {
        conferirDuvida("á");
    }

    @Test
    void duvidaCom279CaracteresEPersistidaSemAlterarTexto() throws Exception {
        conferirDuvida("á".repeat(279));
    }

    // O caso null complementa os casos já existentes de texto vazio e somente espaços.
    @Test
    void duvidaNulaRetorna400SemGravarRegistro() throws Exception {
        long antes = total("duvida");
        jsonAluno("POST", "/api/salas/" + salaId + "/duvidas", "{\"texto\":null}", 400);
        assertEquals(antes, total("duvida"));
    }

    // A sexta opção deve receber F, continuar sem gabarito para o aluno e aceitar a resposta correta.
    @Test
    void quizComSeisAlternativasPermiteResponderALetraF() throws Exception {
        String quiz = json("POST", rotaQuizzes(), pedidoQuiz(pergunta(6)), login, 201);
        List<String> letras = JsonPath.read(quiz, "$.perguntas[0].alternativas[*].letra");
        assertEquals(List.of("A", "B", "C", "D", "E", "F"), letras);
        assertEquals(Boolean.TRUE, JsonPath.read(quiz, "$.perguntas[0].alternativas[5].correta"));
        long quizId = numero(quiz, "$.id");
        json("POST", "/api/salas/" + salaId + "/quiz/enviar-existente/" + quizId, "", login, 200);
        String atual = jsonAluno("GET", "/api/salas/" + salaId + "/quiz/pergunta-atual", null, 200);
        assertEquals(6, ((List<?>) JsonPath.read(atual, "$.alternativas")).size());
        assertTrue(((List<?>) JsonPath.read(atual, "$..correta")).isEmpty());
        long alternativaF = numero(atual, "$.alternativas[5].id");
        jsonAluno("POST", "/api/salas/" + salaId + "/quiz/resposta", "{\"alternativaId\":" + alternativaF + "}", 200);
        String resultado = json("GET", "/api/salas/" + salaId + "/quiz/resultado", null, login, 200);
        assertEquals(1, numero(resultado, "$.totalRespondentes"));
        assertEquals(1, numero(resultado, "$.resultados[5].totalRespostas"));
        assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM quiz_alternativa WHERE pergunta_id = ?", Integer.class,
                numero(atual, "$.perguntaId")));
    }

    // Verifica a recusa de sete opções, inclusive após uma pergunta válida, sem cadastro parcial.
    @Test
    void quizComSeteAlternativasRetorna400SemSalvarPerguntasOuOpcoes() throws Exception {
        long quizzes = total("quiz");
        long perguntas = total("quiz_pergunta");
        long alternativas = total("quiz_alternativa");
        for (String perguntasDoPedido : new String[]{pergunta(7), pergunta(6) + "," + pergunta(7)}) {
            json("POST", rotaQuizzes(), pedidoQuiz(perguntasDoPedido), login, 400);
            assertEquals(quizzes, total("quiz"));
            assertEquals(perguntas, total("quiz_pergunta"));
            assertEquals(alternativas, total("quiz_alternativa"));
        }
    }

    // A rota que cria e envia também deve manter a sala sem quiz se houver sete opções.
    @Test
    void criarEEnviarQuizComSeteAlternativasNaoIniciaRodada() throws Exception {
        long antes = total("quiz");
        json("POST", "/api/salas/" + salaId + "/quiz/criar-e-enviar?professorId=" + professorId,
                pedidoQuiz(pergunta(7)), login, 400);
        assertEquals(antes, total("quiz"));
        assertNull(jdbc.queryForObject("SELECT quiz_em_andamento_id FROM sala WHERE id = ?", Long.class, salaId));
        jsonAluno("GET", "/api/salas/" + salaId + "/quiz/pergunta-atual", null, 404);
    }

    // Um byte é o menor arquivo não vazio; o download precisa preservar o mesmo conteúdo.
    @Test
    void uploadDeUmBytePreservaConteudoNoDownload() throws Exception {
        conferirUploadAceito(1);
    }

    // O limite real inclui os cabeçalhos das partes e o campo titulo, além dos bytes do arquivo.
    @Test
    void uploadComRequisicaoExatamente10MiBEAceito() throws Exception {
        int tamanhoArquivo = DEZ_MIB - multipart(0).length;
        assertEquals(DEZ_MIB, multipart(tamanhoArquivo).length);
        conferirUploadAceito(tamanhoArquivo);
    }

    @Test
    void uploadComRequisicaoUmByteAcimaDe10MiBAceitaEspacoDoFormulario() throws Exception {
        int tamanhoArquivo = DEZ_MIB - multipart(0).length + 1;
        assertTrue(tamanhoArquivo < DEZ_MIB);
        assertEquals(DEZ_MIB + 1, multipart(tamanhoArquivo).length);
        conferirUploadAceito(tamanhoArquivo);
    }

    // O limite total de 11 MiB deixa espaço para o arquivo de 10 MiB e os campos do formulário.
    @Test
    void uploadDeArquivoExatamente10MiBPreservaConteudoNoDownload() throws Exception {
        conferirUploadAceito(DEZ_MIB);
    }

    @Test
    void uploadDeArquivoUmByteAcimaDe10MiBRetorna413() throws Exception {
        conferirUploadRecusado(DEZ_MIB + 1);
    }

    // O arquivo respeita 10 MiB; um campo extra aumenta somente o tamanho total da requisição.
    @Test
    void uploadComRequisicaoUmByteAcimaDe11MiBRetorna413() throws Exception {
        int limiteRequisicao = 11 * 1024 * 1024;
        int tamanhoExtra = limiteRequisicao + 1 - multipartComCampoExtra(DEZ_MIB, 0).length;
        byte[] corpo = multipartComCampoExtra(DEZ_MIB, tamanhoExtra);
        assertEquals(limiteRequisicao + 1, corpo.length);
        conferirUploadRecusado(corpo);
    }

    private void conferirDuvida(String texto) throws Exception {
        String resposta = jsonAluno("POST", "/api/salas/" + salaId + "/duvidas", "{\"texto\":\"" + texto + "\"}", 201);
        assertEquals(texto, JsonPath.read(resposta, "$.texto"));
        assertEquals(texto, jdbc.queryForObject("SELECT texto FROM duvida WHERE id = ?", String.class, numero(resposta, "$.id")));
    }

    private String rotaQuizzes() {
        return "/api/professores/" + professorId + "/quizzes";
    }

    private String pedidoQuiz(String perguntas) {
        return "{\"titulo\":\"Quiz de limites\",\"perguntas\":[" + perguntas + "]}";
    }

    // Monta opções distintas e marca a última como correta para exercitar também a letra F.
    private String pergunta(int quantidade) {
        StringBuilder alternativas = new StringBuilder();
        for (int indice = 0; indice < quantidade; indice++) {
            if (indice > 0) alternativas.append(',');
            alternativas.append("{\"texto\":\"Opcao ").append(indice + 1)
                    .append("\",\"correta\":").append(indice == quantidade - 1).append('}');
        }
        return "{\"enunciado\":\"Escolha a ultima\",\"tempoLimiteSegundos\":300,\"alternativas\":[" + alternativas + "]}";
    }

    private void conferirUploadAceito(int tamanhoArquivo) throws Exception {
        long antes = total("material");
        HttpResponse<byte[]> resposta = enviarUpload(tamanhoArquivo);
        assertEquals(201, resposta.statusCode(), () -> new String(resposta.body(), StandardCharsets.UTF_8));
        assertEquals(antes + 1, total("material"));
        String dados = new String(resposta.body(), StandardCharsets.UTF_8);
        long id = numero(dados, "$.id");
        Path arquivo = Path.of(jdbc.queryForObject("SELECT conteudo_ou_url FROM material WHERE id = ?", String.class, id));
        assertTrue(arquivo.toAbsolutePath().normalize().startsWith(pastaArquivos.toAbsolutePath().normalize()));
        try {
            assertEquals(tamanhoArquivo, Files.size(arquivo));
            HttpResponse<byte[]> download = enviar("GET", "/api/materiais/" + id + "/arquivo", new byte[0], null, login, null);
            assertEquals(200, download.statusCode());
            byte[] esperado = new byte[tamanhoArquivo];
            Arrays.fill(esperado, (byte) 'x');
            assertArrayEquals(esperado, download.body());
        } finally {
            // Remove somente o arquivo criado neste caso, depois de conferir a pasta exclusiva.
            Files.deleteIfExists(arquivo);
        }
    }

    private void conferirUploadRecusado(int tamanhoArquivo) throws Exception {
        conferirUploadRecusado(multipart(tamanhoArquivo));
    }

    private void conferirUploadRecusado(byte[] corpo) throws Exception {
        long registros = total("material");
        long arquivos = totalArquivos();
        assertEquals(413, enviarUpload(corpo).statusCode());
        assertEquals(registros, total("material"));
        assertEquals(arquivos, totalArquivos());
    }

    private HttpResponse<byte[]> enviarUpload(int tamanhoArquivo) throws Exception {
        return enviarUpload(multipart(tamanhoArquivo));
    }

    private HttpResponse<byte[]> enviarUpload(byte[] corpo) throws Exception {
        return enviar("POST", "/api/professores/" + professorId + "/materiais/upload", corpo,
                "multipart/form-data; boundary=" + BORDA, login, null);
    }

    // Monta o corpo em bytes para medir exatamente a fronteira da requisição, sem cliente simulado.
    private byte[] multipart(int tamanhoArquivo) {
        byte[] inicio = ("--" + BORDA + "\r\nContent-Disposition: form-data; name=\"titulo\"\r\n\r\nLimite\r\n"
                + "--" + BORDA + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"limite.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] fim = ("\r\n--" + BORDA + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] corpo = new byte[inicio.length + tamanhoArquivo + fim.length];
        System.arraycopy(inicio, 0, corpo, 0, inicio.length);
        Arrays.fill(corpo, inicio.length, inicio.length + tamanhoArquivo, (byte) 'x');
        System.arraycopy(fim, 0, corpo, inicio.length + tamanhoArquivo, fim.length);
        return corpo;
    }

    private byte[] multipartComCampoExtra(int tamanhoArquivo, int tamanhoExtra) {
        byte[] original = multipart(tamanhoArquivo);
        byte[] fim = ("\r\n--" + BORDA + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] campo = ("\r\n--" + BORDA + "\r\nContent-Disposition: form-data; name=\"apoio\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        int inicioCampo = original.length - fim.length;
        byte[] corpo = Arrays.copyOf(original, original.length + campo.length + tamanhoExtra);
        System.arraycopy(campo, 0, corpo, inicioCampo, campo.length);
        Arrays.fill(corpo, inicioCampo + campo.length, inicioCampo + campo.length + tamanhoExtra, (byte) 'x');
        System.arraycopy(fim, 0, corpo, corpo.length - fim.length, fim.length);
        return corpo;
    }

    private long totalArquivos() throws Exception {
        try (var arquivos = Files.list(pastaArquivos)) {
            return arquivos.filter(Files::isRegularFile).count();
        }
    }

    // As tabelas vêm somente dos nomes fixos dos testes, nunca de dados recebidos pela API.
    private long total(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
    }

    private long numero(String dados, String caminho) {
        return ((Number) JsonPath.read(dados, caminho)).longValue();
    }

    private String json(String metodo, String rota, String corpo, String autenticacao, int esperado) throws Exception {
        HttpResponse<byte[]> resposta = enviar(metodo, rota, corpo == null ? new byte[0] : corpo.getBytes(StandardCharsets.UTF_8),
                "application/json", autenticacao, null);
        String dados = new String(resposta.body(), StandardCharsets.UTF_8);
        assertEquals(esperado, resposta.statusCode(), () -> metodo + " " + rota + ": " + dados);
        return dados;
    }

    private String jsonAluno(String metodo, String rota, String corpo, int esperado) throws Exception {
        HttpResponse<byte[]> resposta = enviar(metodo, rota, corpo == null ? new byte[0] : corpo.getBytes(StandardCharsets.UTF_8),
                "application/json", null, token);
        String dados = new String(resposta.body(), StandardCharsets.UTF_8);
        assertEquals(esperado, resposta.statusCode(), () -> metodo + " " + rota + ": " + dados);
        return dados;
    }

    private HttpResponse<byte[]> enviar(String metodo, String rota, byte[] corpo, String tipo, String autenticacao, String tokenAluno) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + rota))
                .timeout(Duration.ofSeconds(30)).method(metodo, HttpRequest.BodyPublishers.ofByteArray(corpo));
        if (tipo != null) pedido.header("Content-Type", tipo);
        if (autenticacao != null) pedido.header("Authorization", autenticacao);
        if (tokenAluno != null) pedido.header("X-Aluno-Token", tokenAluno);
        return cliente.send(pedido.build(), HttpResponse.BodyHandlers.ofByteArray());
    }
}
