# API do AstroChimp

A API concentra as regras das salas de aula e guarda os dados no MySQL. Ela recebe os pedidos do aplicativo, confere o acesso de cada pessoa e devolve respostas em JSON. O backend utiliza Java 17, Spring Boot 4.1.1, Spring Data JPA e Spring Security.

## Organização

| Pacote | Responsabilidade |
|---|---|
| `controller` | Definir rotas, receber parâmetros e devolver respostas HTTP |
| `service` | Conferir permissões, validar dados e executar as regras |
| `repository` | Consultar e salvar entidades com Spring Data JPA |
| `entity` | Representar tabelas e relacionamentos do banco |
| `dto` | Reunir os campos permitidos nos pedidos e nas respostas |
| `security` | Configurar autenticação e acesso às rotas |

O caminho de um pedido é `Controller → Service → Repository → MySQL`. Ao enviar uma dúvida, o controller recebe o texto e o anonimato, o service identifica o aluno pelo token e confere a sala e o repository grava o registro. A resposta é montada conforme o acesso de quem fez a consulta.

Os services usam `if`, `for` e listas com `ArrayList`. O construtor recebe os repositórios e serviços necessários. As operações com várias gravações usam `@Transactional` para confirmar ou desfazer as alterações do banco juntas.

## Dados e DTOs

As entidades representam professores, salas, alunos, dúvidas, materiais, quizzes, perguntas, alternativas e respostas. Uma sala pode ter vários alunos e materiais. Um quiz possui perguntas, e cada pergunta possui alternativas. As respostas guardam o aluno e a rodada em que a escolha foi feita.

Os 11 DTOs selecionam dados que podem ser recebidos ou devolvidos. Algumas classes atendem aos dois sentidos, e o service usa apenas os campos válidos para a operação solicitada.

| DTOs | Finalidade |
|---|---|
| `PerfilProfessorDto`, `PerfilPublicoProfessorDto` | Receber cadastro e edição; devolver o perfil sem senha e, na consulta pública, sem e-mail |
| `SalaDto`, `EntradaSalaDto` | Devolver dados da aula e o acesso do aluno que entrou, sem listar tokens de outros participantes |
| `DuvidaDto` | Apresentar texto, votos e estado, preservando o anonimato na consulta dos alunos |
| `MaterialDto` | Apresentar o conteúdo ou a rota de download, sem expor o caminho do arquivo no servidor |
| `CronometroDto` | Informar a duração da atividade, os segundos restantes e se a contagem está ativa |
| `PerguntaAlunoDto`, `AlternativaAlunoDto` | Apresentar a pergunta atual sem gabarito e receber a escolha do aluno |
| `ResultadoPerguntaDto`, `ResultadoAlternativaDto` | Apresentar as contagens por alternativa na rodada consultada pelo professor |

`@JsonProperty` define campos que só entram ou só saem no JSON. `@JsonIgnore` retira dados internos e ligações de volta entre entidades. Nas rotas do professor, o quiz usa `Quiz`, `QuizPergunta` e `QuizAlternativa`, com os campos internos protegidos e os valores permitidos copiados pelo service.

Nos campos de publicação e fixação, `Boolean` diferencia um valor ausente (`null`) de uma escolha explícita (`false`). Os ids usam `Long`, e as coleções usam `List`. O código define a ordem das perguntas e as letras das alternativas ao salvar o quiz.

## Configurar e executar

Inicie o MySQL Server na porta 3306. O Workbench é uma ferramenta de consulta e administração e depende desse serviço para acessar o banco.

Na pasta `backend`, copie o exemplo:

```powershell
Copy-Item application-local.example.properties application-local.properties
```

Preencha `application-local.properties` com o usuário e a senha do seu MySQL. O arquivo fica fora do Git e é carregado a partir desta pasta. A configuração pública usa `localhost:3306/astrochimp`; as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` também podem fornecer a conexão.

Com JDK 17 ou superior instalado, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

A API abre em `http://localhost:8080`. O usuário do banco precisa de permissão para criar bancos e tabelas. `ddl-auto=update` atualiza as tabelas da aplicação. Uploads ficam em `uploads/`, com limite de 10 MB.

## Acesso

O cadastro do professor recebe nome, e-mail, senha e endereço público. Depois do cadastro, as ações do professor utilizam HTTP Basic com e-mail e senha. O servidor armazena a senha como hash BCrypt e confere se o professor é dono da sala ou do conteúdo solicitado.

O aluno entra com código e nome em `POST /api/salas/entrar`. A resposta inclui `tokenAluno`, que deve acompanhar suas próximas requisições no cabeçalho `X-Aluno-Token`. Ele precisa pertencer à sala indicada no pedido.

O perfil público permite consultar somente os dados de apresentação e materiais publicados. Uma dúvida anônima oculta a autoria para os alunos; o professor pode consultar o autor. A pergunta do quiz enviada à turma não inclui a alternativa correta.

## Testes

```powershell
.\mvnw.cmd clean test
```

São 36 testes de integração com MySQL real. A suíte usa exclusivamente `astrochimp_test`, com as mesmas credenciais do backend, e confere o nome do banco antes de preparar os dados. As tabelas desse banco são recriadas e removidas durante os testes; não guarde dados de aula nele.

As URLs de teste ficam definidas em `@SpringBootTest`, incluindo `spring.datasource.url` e `spring.datasource.hikari.jdbc-url`. Arquivos de teste ficam em `target/test-uploads`.

| Área | Exemplos de casos verificados |
|---|---|
| Professor | Cadastro, login, duplicidade, atualização e acesso ao próprio perfil |
| Sala | Entrada, expiração, encerramento, participantes e cronômetro |
| Dúvidas | Limite de texto, anonimato, estados, fixação e voto único |
| Materiais | Conteúdo, upload, download, publicação, permissões e limpeza após rollback |
| Quiz | Validação, gabarito protegido, rodada, tempo, resposta única e contagens |
| Pedidos simultâneos | Recusa de dois votos ou duas respostas do mesmo aluno na mesma atividade |

Os testes HTTP usam MockMvc. Os testes simultâneos verificam casos de duplicidade e não representam um ensaio de carga. A cobertura das rotas não garante a verificação de todos os caminhos possíveis.

## Rotas principais

| Quem | Método e rota | Função |
|---|---|---|
| Público | `POST /api/professores` | Cadastrar professor |
| Professor | `GET /api/professores/{id}` | Consultar o próprio perfil |
| Professor | `GET /api/professores/me` | Consultar o perfil do e-mail autenticado |
| Professor | `GET /api/professores/{id}/salas` | Listar as salas do próprio professor |
| Professor | `PUT /api/professores/{id}` | Atualizar nome, descrição, foto e endereço público |
| Público | `GET /api/professores/publico/{slug}` | Ver perfil e materiais públicos |
| Professor | `POST /api/salas` | Criar sala: `{"professorId":1,"nomeAula":"Java"}` |
| Aluno | `POST /api/salas/entrar` | Entrar pelo código |
| Professor ou aluno | `GET /api/salas/{id}` | Consultar sala |
| Professor | `PATCH /api/salas/{id}/encerrar` | Encerrar sala |
| Professor | `POST /api/salas/{id}/cronometro` | Iniciar: `{"segundos":120}` |
| Professor ou aluno | `GET /api/salas/{id}/cronometro` | Consultar tempo restante |
| Professor | `DELETE /api/salas/{id}/cronometro` | Parar cronômetro |
| Aluno | `POST /api/salas/{id}/duvidas` | Criar: `{"texto":"Minha pergunta","anonima":true}` |
| Aluno | `GET /api/salas/{id}/duvidas` | Listar por votos, sem revelar autor anônimo |
| Professor | `GET /api/salas/{id}/duvidas/professor` | Listar com identidade dos autores |
| Aluno | `POST /api/duvidas/{id}/votos` | Votar uma vez por dúvida |
| Professor | `PATCH /api/duvidas/{id}/estado` | Definir `pendente`, `respondida` ou `arquivada` |
| Professor | `PATCH /api/duvidas/{id}/fixacao` | Fixar: `{"fixada":true}` |
| Professor | `POST /api/professores/{id}/materiais` | Criar link ou código no perfil |
| Professor | `POST /api/professores/{id}/materiais/upload` | Enviar arquivo (`multipart/form-data`: `titulo`, `arquivo`) |
| Professor | `GET /api/professores/{id}/materiais` | Listar materiais do perfil |
| Professor | `POST /api/salas/{salaId}/materiais/{materialId}` | Publicar material existente na sala |
| Professor | `POST /api/salas/{salaId}/materiais/upload` | Enviar arquivo novo diretamente à sala |
| Professor ou aluno | `GET /api/salas/{salaId}/materiais` | Listar materiais da sala |
| Professor | `PATCH /api/salas/{salaId}/materiais/{materialId}/fixacao` | Fixar: `{"fixado":true}` |
| Professor | `PATCH /api/professores/{id}/materiais/{materialId}/publicacao` | Tornar público: `{"publico":true}` |
| Professor ou aluno | `GET /api/materiais/{id}/arquivo?salaId=1` | Baixar arquivo; público dispensa sala |

Para criar link ou trecho de código, envie `titulo`, `tipo` (`link` ou `codigo`), `conteudoOuUrl` e `detalhe` no corpo JSON. O pin de material pertence à sala: fixar em uma sala não fixa nas outras.

## Quiz

O professor cria com `POST /api/professores/{professorId}/quizzes`:

```json
{
  "titulo": "Revisão",
  "salvarNoPerfil": true,
  "perguntas": [{
    "enunciado": "Quanto é 2 + 2?",
    "dificuldade": "facil",
    "tempoLimiteSegundos": 30,
    "alternativas": [
      {"texto": "4", "correta": true},
      {"texto": "5", "correta": false}
    ]
  }]
}
```

Cada pergunta aceita de 2 a 6 alternativas e exatamente uma correta.

| Quem | Método e rota | Função |
|---|---|---|
| Professor | `GET /api/professores/{id}/quizzes` | Listar quizzes salvos |
| Professor | `GET /api/quizzes/{id}` | Ver quiz com gabarito |
| Professor | `PATCH /api/quizzes/{id}/salvar-no-perfil?salvar=true` | Salvar ou remover do perfil |
| Professor | `DELETE /api/quizzes/{id}` | Excluir se não tiver respostas nem estiver em uso |
| Professor | `POST /api/salas/{salaId}/quiz/enviar-existente/{quizId}` | Iniciar quiz salvo |
| Professor | `POST /api/salas/{salaId}/quiz/criar-e-enviar?professorId=1` | Criar e iniciar na hora |
| Professor ou aluno | `GET /api/salas/{salaId}/quiz/pergunta-atual` | Ver pergunta e tempo restante; aluno não vê gabarito |
| Aluno | `POST /api/salas/{salaId}/quiz/resposta` | Responder: `{"alternativaId":1}` |
| Professor | `GET /api/salas/{salaId}/quiz/resultado` | Ver quantidade por alternativa |
| Professor | `POST /api/salas/{salaId}/quiz/proxima-pergunta` | Avançar pergunta |
| Professor | `POST /api/salas/{salaId}/quiz/encerrar` | Encerrar quiz |

Uma nova execução do mesmo quiz começa com zero respostas. Respostas após o tempo ou repetidas pelo mesmo aluno são recusadas.


## Atualização do aplicativo

As telas da sala consultam dúvidas, materiais, cronômetro e pergunta atual a cada três segundos. O tempo de uma pergunta é calculado pelo servidor a partir do instante em que ela foi aberta na sala. Uma nova aplicação do mesmo quiz recebe outra identificação de rodada.

A conferência de tipos e a execução do aplicativo estão descritas no [README do frontend](../frontend/README.md). O [README principal](../README.md) apresenta o projeto e a configuração completa do ambiente.
