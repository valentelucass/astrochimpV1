# AstroChimp — backend

API em Java 17, Spring Boot 4.1.1, Maven, Spring Data JPA e H2. Os controllers recebem as requisições; os services fazem as regras; os repositories acessam o banco.

## Como o código está organizado

O caminho de uma requisição é `Controller → Service → Repository → banco`.

- `controller`: define a rota, recebe os parâmetros e chama o service. `@ResponseStatus` define o status de criação (`201`) ou exclusão (`204`). O download usa `ResponseEntity` porque também precisa enviar cabeçalhos.
- `service`: verifica permissões, valida os dados, executa as regras e monta o DTO de resposta. `@Transactional` mantém as operações de banco do método na mesma transação.
- `repository`: usa `JpaRepository` para consultar e salvar entidades. A consulta de dúvidas ordena por fixação, quantidade de votos e data.
- `entity`: representa as tabelas e os relacionamentos do banco. As coleções começam vazias. `@OrderBy` carrega as perguntas do quiz na ordem e as alternativas pela letra.
- `dto`: classe simples com atributos, construtores, getters e setters para receber ou devolver JSON.

Por exemplo, ao criar uma dúvida, `DuvidaController.criar` recebe `DuvidaDto` e chama `DuvidaService.criar`. O service verifica a sala e o aluno, copia somente texto e anonimato para uma nova entidade e chama `DuvidaRepository.save`. Depois monta a resposta, ocultando o autor se a dúvida for anônima. O professor continua podendo consultar a autoria pela rota dele.

### DTOs reaproveitados

Há 20 DTOs. Os pedidos que usam os mesmos campos compartilham uma classe:

| DTO | Campos lidos em cada operação |
|---|---|
| `DuvidaDto` | Criar: `texto`, `anonima`; mudar estado: `estado`; fixar: `fixada` |
| `MaterialDto` | Criar: `titulo`, `tipo`, `conteudoOuUrl`, `detalhe`; fixar: `fixado`; publicar: `publico` |
| `PerfilProfessorDto` | Atualizar: `nome`, `descricao`, `fotoUrl`, `slugPerfilPublico` |
| `PerguntaDto` | Criar pergunta: `enunciado`, `dificuldade`, `tempoLimiteSegundos`, `alternativas` |
| `AlternativaDto` | Criar alternativa: `texto`, `correta` |

Esses DTOs também aparecem nas respostas. Cada service copia explicitamente os campos permitidos: enviar `id`, autor, votos, ordem ou outro campo de resposta não permite alterar esses dados. A ordem das perguntas e as letras das alternativas são definidas pelo servidor.

Nos DTOs compartilhados, `@JsonCreator` indica que o JSON deve usar o construtor vazio e os setters. O construtor completo serve para montar respostas no service. Assim, um pedido como `{"fixada":true}` não precisa enviar todos os campos da resposta.

DTOs diferentes continuam separados quando os dados mudam: cadastro recebe senha, perfil não devolve senha, perfil público não devolve e-mail, pergunta do aluno não inclui gabarito, entrada na sala devolve um token e resultado do quiz devolve contagens. `CriarQuizDto` mantém `salvarNoPerfil` na entrada e `QuizProfessorDto` mantém `salvoNoPerfil` na saída para preservar o JSON existente. O cronômetro recebe `segundos` e responde com totais, tempo restante e estado.

### Tipos das variáveis

| Tipo | Quando usar aqui |
|---|---|
| `Long` | IDs do banco; antes de salvar, o ID pode ser `null` |
| `int` | Índices, ordem, duração obrigatória e tamanho de uma lista |
| `Integer` | Duração que pode estar ausente, como um cronômetro parado |
| `long` | Contagens retornadas pelo banco e segundos calculados por `Duration` |
| `boolean` | Valores de sim/não, como `anonima`, `fixada` e `publico` |
| `List<T>` | Uma coleção de objetos; `new ArrayList<>()` cria a lista vazia |

`int` e `ArrayList` têm funções diferentes. A entidade `Duvida` guarda `List<Aluno> alunosQueVotaram` para impedir votos repetidos; o DTO devolve `int votos`, calculado com `alunosQueVotaram.size()`. Os nomes deixam explícita a diferença entre participantes e quantidade.

Os services usam `for`, `if` e métodos pequenos. As dependências seguem os nomes `duvidaService`, `duvidaRepository` e `acessoService`. Não há Lombok, mapeadores automáticos ou classes base genéricas.

### Como rastrear os campos entre classes

Os parâmetros que passam entre controller, service e repository usam `professorId`, `salaId`, `duvidaId`, `quizId`, `materialId` e `tokenAluno`. Dentro de cada entidade, a chave continua sendo `id`. Nas rotas com `{id}`, a anotação faz a ligação explícita: `@PathVariable("id") Long salaId`, por exemplo.

| Dado recebido ou devolvido | Origem ou destino no backend |
|---|---|
| `CadastrarProfessorDto.nome`, `email`, `senha`, `slugPerfilPublico` | `ProfessorService` valida e preenche `Professor`; e-mail e slug são normalizados, senha é codificada |
| `PerfilProfessorDto` | `Professor.id`, `nome`, `email`, `descricao`, `fotoUrl`, `slugPerfilPublico`; atualização altera apenas os campos permitidos |
| `PerfilPublicoProfessorDto` | Dados públicos de `Professor` e materiais filtrados por `professorId` e `publico` |
| `CriarSalaDto.professorId`, `nomeAula` | `Sala.professor`, `nomeAula`; código, estado e datas são definidos pelo service |
| `SalaDto.nomeProfessor` | `Sala.professor.nome`; os demais campos vêm diretamente de `Sala` |
| `EntrarSalaDto.codigo`, `nomeAluno` | Busca `Sala.codigo` e cria `Aluno.nome`; a participação é gravada em `Sala.alunos` |
| `EntradaSalaDto.sala`, `alunoId`, `tokenAluno` | `SalaDto`, `Aluno.id` e `Aluno.token`; o token volta no cabeçalho `X-Aluno-Token` |
| `IniciarCronometroDto.segundos` | `Sala.cronometroSegundos`; `Sala.cronometroIniciadoEm` guarda o início |
| `CronometroDto` | `segundosTotais` vem de `cronometroSegundos`; `segundosRestantes` e `ativo` são calculados |
| `DuvidaDto.texto`, `anonima`, `estado`, `fixada`, `criadaEm`, `id` | Campos correspondentes de `Duvida`; cada rota altera somente os campos daquela operação |
| `DuvidaDto.autorId`, `autorNome` | `Duvida.autor.id`, `nome`; ficam nulos para alunos quando a dúvida é anônima |
| `DuvidaDto.votos` | Tamanho de `Duvida.alunosQueVotaram`; a lista identifica quem já votou |
| `MaterialDto.id`, `titulo`, `tipo`, `detalhe`, `publico`, `criadoEm` | Campos correspondentes de `Material` |
| `MaterialDto.conteudoOuUrl` | Conteúdo ou URL de `Material`; para arquivos, o service devolve a rota de download e guarda o caminho físico só na entidade |
| `MaterialDto.fixado` | Presença do material em `Sala.materiaisFixados`; não é um atributo global de `Material` |
| Upload e download | `titulo` e `MultipartFile arquivo` entram no service; `ArquivoDownload.arquivo` e `nomeArquivo` voltam ao controller |
| `CriarQuizDto.titulo`, `salvarNoPerfil`, `perguntas` | `Quiz.titulo`, `salvoNoPerfil`, `perguntas`; a saída usa `QuizProfessorDto` |
| `PerguntaDto` | Campos de `QuizPergunta`; `id` e `ordem` são definidos pelo servidor; `alternativas` contém `AlternativaDto` |
| `AlternativaDto` | Campos de `QuizAlternativa`; `id` e `letra` são definidos pelo servidor |
| `PerguntaAlunoDto.quizId`, `perguntaId`, `enunciado`, `dificuldade` | Quiz em andamento e pergunta atual da `Sala` |
| `PerguntaAlunoDto.numeroPergunta`, `totalPerguntas`, `tempoRestanteSegundos` | `QuizPergunta.ordem`, tamanho de `Quiz.perguntas` e tempo calculado desde `Sala.quizPerguntaIniciadaEm` |
| `AlternativaAlunoDto` | `QuizAlternativa.id`, `letra`, `texto`; a resposta do aluno não recebe `correta` |
| `ResponderQuizDto.alternativaId` | Busca `QuizAlternativa` e grava `QuizResposta.alternativa`, `aluno`, `respondidoEm`, `sessaoId` |
| `QuizResposta.sessaoId` | Copia `Sala.quizSessaoId`; o repository usa esse valor para separar as execuções do quiz |
| `ResultadoAlternativaDto.totalRespostas` | Contagem `long` de respostas por sessão e alternativa; `letra` e `texto` vêm de `QuizAlternativa` |
| `ResultadoPerguntaDto` | ID e enunciado da pergunta, lista de resultados e soma `long` das contagens em `totalRespondentes` |

Os relacionamentos `Professor.salas`, `materiais`, `quizzes`, `Aluno.salas` e `Sala.duvidas` representam o lado inverso das associações JPA. Não precisam ter um campo correspondente em cada DTO. `Aluno.fotoUrl` está mapeado no banco, mas a API atual não recebe nem devolve esse campo.

## Executar

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

A API abre em `http://localhost:8080`. Para verificar o projeto:

```powershell
.\mvnw.cmd test
```

O H2 usa `data/astrochimp` e mantém os dados entre execuções. Arquivos enviados ficam em `uploads/`. Essas duas pastas não entram no Git. O limite de upload é 10 MB. As configurações estão em `src/main/resources/application.properties`.

O `schema.sql` mantém compatibilidade com bancos antigos: define valores padrão para as antigas colunas `material.fixado` e `quiz_alternativa.total_respostas`, se existirem. Isso é necessário porque `ddl-auto=update` não remove colunas. O script roda depois do JPA, pode ser repetido e não apaga registros. No código atual, a fixação pertence à sala e a quantidade de respostas é calculada por sessão.

Os testes de integração usam H2 em memória e gravam arquivos somente em `target/test-uploads`. Eles verificam os fluxos HTTP, permissões e a compatibilidade do banco, sem usar `data/astrochimp`.

## Acesso

1. Cadastre o professor com `POST /api/professores`:

```json
{"nome":"Ana","email":"ana@exemplo.com","senha":"Senha123","slugPerfilPublico":"ana"}
```

2. Nas ações do professor, envie autenticação **HTTP Basic** usando e-mail e senha. A senha é guardada codificada no banco. Para usar fora do computador de desenvolvimento, coloque a API atrás de HTTPS.
3. O professor cria uma sala. O aluno entra com `POST /api/salas/entrar`:

```json
{"codigo":"ABC123","nomeAluno":"João"}
```

A resposta traz `tokenAluno`. Nas ações do aluno, envie o cabeçalho `X-Aluno-Token` com esse valor. O aluno não precisa de conta nem senha.

## Rotas principais

| Quem | Método e rota | Função |
|---|---|---|
| Público | `POST /api/professores` | Cadastrar professor |
| Professor | `GET /api/professores/{id}` | Consultar o próprio perfil |
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

## Atualização das telas

Para acompanhar a aula, o aplicativo pode consultar periodicamente as rotas de dúvidas, materiais, cronômetro e pergunta atual (por exemplo, a cada 2 ou 3 segundos). Esta versão usa consultas HTTP simples.
