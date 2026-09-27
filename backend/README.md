# AstroChimp — backend

API em Java 17, Spring Boot 4.1.1, Maven, Spring Data JPA e H2. Os controllers recebem as requisições; os services fazem as regras; os repositories acessam o banco.

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
