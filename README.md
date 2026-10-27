# AstroChimp

O AstroChimp é um projeto acadêmico de apoio à participação dos alunos durante as aulas. A proposta é reunir dúvidas, materiais e quizzes em uma sala acessada por código, para que o professor acompanhe a turma pelo aplicativo.

O projeto tem duas partes: um aplicativo React Native com TypeScript e uma API em Java com Spring Boot. O MySQL guarda os cadastros, as salas e as atividades realizadas.

## Objetivo

O objetivo é praticar o desenvolvimento de uma aplicação móvel integrada a um servidor e a um banco de dados. O trabalho envolve criação de telas, formulários, requisições HTTP, validação, autenticação e relacionamentos entre tabelas.

Para facilitar o estudo, o código utiliza funções, estruturas condicionais e listas. No aplicativo, os dados das telas usam `useState`, e as consultas periódicas usam `useEffect`. Na API, as responsabilidades são divididas entre controllers, services e repositories.

## Funcionamento

1. O professor cria uma conta e entra com e-mail e senha.
2. No painel, cria uma sala com o nome da aula e recebe um código de seis caracteres.
3. O aluno informa seu nome e esse código para participar, sem cadastrar uma conta.
4. A turma envia dúvidas, vota nas perguntas e acessa os materiais compartilhados.
5. O professor acompanha as dúvidas, inicia um cronômetro e aplica quizzes com tempo para responder.
6. Ao encerrar a sala, novas interações são bloqueadas, e o professor mantém acesso ao histórico.

## Funcionalidades

| Área | O que pode ser feito |
|---|---|
| Professor | Cadastrar conta, entrar, editar o perfil e criar salas |
| Controle da aula | Acompanhar participantes, fixar dúvidas, marcar respostas e arquivar perguntas |
| Aluno | Entrar por código, enviar dúvidas com nome ou de forma anônima e votar uma vez por pergunta |
| Materiais | Compartilhar links, trechos de código e arquivos de até 10 MB |
| Quiz | Criar perguntas, definir alternativas e duração, aplicar desafios e acompanhar as respostas |
| Perfil público | Consultar os dados de apresentação do professor e os materiais publicados |

Uma sala dura até quatro horas e pode ser encerrada pelo professor. Cada pergunta de quiz aceita de duas a seis alternativas e precisa ter exatamente uma correta. O aluno pode responder uma vez a cada pergunta em uma rodada.

O anonimato esconde o autor da dúvida para os outros alunos. O professor continua podendo identificar quem a enviou. A quantidade de participantes representa as entradas na sala, sem indicar presença online em tempo real.

## Tecnologias

| Parte | Tecnologias utilizadas |
|---|---|
| Aplicativo | React Native, TypeScript e Expo SDK 57 |
| Interface | Componentes nativos, StyleSheet e fonte Nunito |
| Comunicação | Requisições HTTP com `fetch` e dados em JSON |
| API | Java 17, Spring Boot 4.1.1 e Maven |
| Persistência | Spring Data JPA, Hibernate e MySQL |
| Acesso | Spring Security, HTTP Basic para professor e token de participação para aluno |
| Testes | JUnit, MockMvc e MySQL em banco separado |

## Organização do projeto

```text
astrochimp/
├── backend/
│   ├── src/main/java/edu/senacsp/astrochimp/
│   │   ├── controller/     rotas HTTP
│   │   ├── service/        regras e validações
│   │   ├── repository/     consultas ao banco
│   │   ├── entity/         entidades e relacionamentos
│   │   ├── dto/            dados de entrada e saída
│   │   └── security/       configuração de acesso
│   └── src/test/           testes de integração
└── frontend/
    ├── App.tsx             sessão aberta e escolha da tela
    ├── src/pages/          telas do aplicativo
    ├── src/components/     campos, botões, cartões e cabeçalhos
    ├── src/services/       comunicação com a API e arquivos
    ├── src/styles/         cores e estilos compartilhados
    ├── src/types/          tipos dos dados
    └── assets/             imagens
```

Quando um aluno envia uma dúvida, a tela chama o serviço de API do aplicativo. O controller recebe o pedido, o service confere a sala e o aluno e o repository salva a pergunta no MySQL. A resposta volta à tela com os campos que aquele usuário pode consultar.

As principais entidades são `Professor`, `Sala`, `Aluno`, `Duvida`, `Material`, `Quiz`, `QuizPergunta`, `QuizAlternativa` e `QuizResposta`. O professor possui salas, materiais e quizzes. A sala reúne alunos e dúvidas. O quiz possui perguntas e alternativas, e cada resposta registra a escolha do aluno naquela rodada.

## Executar no computador

O ambiente utilizado tem JDK 17 ou superior, Node.js 24 e MySQL Server na porta 3306. O Maven Wrapper está na pasta do backend. O Workbench pode ser usado para consultar o banco, mas o serviço do MySQL Server precisa estar iniciado.

### Configurar e iniciar a API

Na pasta `backend`, copie o exemplo de configuração:

```powershell
cd backend
Copy-Item application-local.example.properties application-local.properties
```

Abra `application-local.properties` e informe o usuário e a senha do seu MySQL. Esse arquivo fica somente no computador e é ignorado pelo Git. A senha do banco pertence à configuração do backend e não deve ser colocada no aplicativo.

O endereço padrão é `localhost:3306/astrochimp`. O usuário precisa de permissão para criar o banco e as tabelas. Depois de configurar, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080`. As variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` também podem fornecer a conexão quando não houver configuração local.

### Iniciar o aplicativo

Abra outro terminal na raiz do projeto:

```powershell
cd frontend
npm install
npm start
```

Leia o QR Code com uma versão do Expo Go compatível com o SDK 57. O celular e o computador devem estar na mesma rede. O aplicativo usa o endereço informado pelo Expo para encontrar a API na porta 8080.

Se a API estiver em outro endereço, copie `frontend/.env.example` para `frontend/.env.local`, ajuste `EXPO_PUBLIC_API_URL` e reinicie o Expo. Essa variável guarda somente o endereço da API. No celular, `localhost` aponta para o próprio aparelho.

`npm run android` usa um emulador Android já configurado. `npm run ios` precisa de macOS para abrir o simulador. A prévia para navegador pode ser iniciada com `npm run web`.

## Dados e regras de acesso

As senhas dos professores são armazenadas como hash BCrypt. A API não devolve senha, o perfil público omite e-mail e a pergunta enviada ao aluno não inclui o gabarito. Os DTOs selecionam os dados permitidos em cada resposta.

As ações do professor exigem seu login e a conferência de propriedade dos dados. Ao entrar na sala, o aluno recebe um token, enviado nas próximas requisições pelo cabeçalho `X-Aluno-Token`. Ele não permite controlar a aula nem acessar outra sala.

A sessão do aplicativo fica em memória. Ao sair ou recarregar o app, é preciso entrar novamente. Os arquivos enviados ficam em `backend/uploads`, e os dados da aplicação ficam no banco `astrochimp`.

## Testes e verificação

Na pasta `backend`:

```powershell
.\mvnw.cmd clean test
```

Os testes usam o banco separado `astrochimp_test`, com as credenciais do backend. Suas tabelas são recriadas e removidas durante a execução. Esse banco deve ser reservado aos testes.

Na pasta `frontend`:

```powershell
npm run typecheck
npx expo install --check
npx expo export --platform all
```

A verificação registrada nesta versão inclui 36 testes de integração da API, a checagem de tipos e a exportação dos pacotes de código. As 13 telas foram utilizadas na prévia, com sessões separadas de professor e aluno, nos fluxos de entrada, dúvidas, votos, materiais, cronômetro, quiz e perfil.

## Limitações da versão

As telas acompanham a aula por consultas HTTP a cada três segundos. Ainda não há WebSocket, armazenamento de sessão entre aberturas do app ou recuperação de senha.

Os pacotes de Android e iOS foram gerados, mas a execução em aparelho físico ou emulador ainda precisa ser validada. Seleção de arquivos, compartilhamento, cópia e abertura de links também devem ser conferidos no celular. Fora do ambiente de desenvolvimento, a API deve usar HTTPS.

## Documentação complementar

- [Backend: estrutura, configuração e rotas](backend/README.md)
- [Aplicativo: telas, pastas e execução](frontend/README.md)
- [Documentação do React Native](https://reactnative.dev/docs/getting-started)
- [Documentação do Expo para TypeScript](https://docs.expo.dev/guides/typescript/)
- [Configuração externa do Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html)
