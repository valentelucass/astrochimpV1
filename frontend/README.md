# Aplicativo AstroChimp

O aplicativo reúne as telas usadas pelo professor e pelo aluno durante a aula. Foi organizado em React Native com Expo e TypeScript, usando componentes como `View`, `Text`, `TextInput`, `FlatList` e `Modal`. A comunicação com a API Spring utiliza `fetch` e dados em JSON.

## Iniciar

Use Node.js 24 e mantenha o backend e o MySQL abertos. Na pasta `backend`, execute `.\mvnw.cmd spring-boot:run`. Em outro terminal, na pasta `frontend`:

```powershell
npm install
npm start
```

Leia o QR Code com o Expo Go no celular. Computador e celular precisam estar na mesma rede. O projeto usa Expo SDK 57; o Expo Go deve aceitar essa versão.

O comando `npm run android` abre um emulador Android já configurado. `npm run ios` precisa de macOS para abrir o simulador. `npm run web` abre uma prévia no navegador para conferir telas e fluxos.

O backend usa a porta 8080. No celular, `src/services/api.ts` usa o endereço do computador informado pelo Expo. Para escolher outro endereço, copie `.env.example` para `.env.local`, ajuste a URL e reinicie o Expo. Use o IP do computador; `localhost` no celular aponta para o próprio celular.

## Pastas

```text
App.tsx                escolhe a tela e guarda a sessão aberta
index.ts               inicia o aplicativo no Expo
src/pages/             telas do aluno e do professor
src/components/        botões, campos, cartões e cabeçalhos reutilizados
src/services/          pedidos à API, arquivos e formatação de textos
src/styles/            cores e estilos usados por várias telas
src/types/             tipos dos dados enviados e recebidos
assets/                imagens do aplicativo
```

Arquivos com JSX usam `.tsx`. Serviços, estilos e tipos usam `.ts`. O TypeScript ajuda a conferir campos e propriedades durante o desenvolvimento. A API continua responsável por validar os pedidos e proteger os dados.

O estado `tela`, no `App.tsx`, escolhe qual componente aparece. As páginas recebem dados e funções por props. A entrada do aluno, por exemplo, devolve a sala e seu token ao `App`, que abre o feed com essa participação.

## Telas

| Fluxo | Telas |
|---|---|
| Entrada | `Inicio`, `LoginProfessor` e `CadastroProfessor` |
| Professor | `PainelProfessor`, `SalaProfessor`, `PerfilProfessor` e `PerfilPublico` |
| Aluno | `SalaAluno` e `QuizAluno` |
| Conteúdos | `MateriaisSala`, `CriarMaterial`, `EscolherQuiz` e `CriarQuiz` |

O professor cria uma conta, entra, cria salas, acompanha dúvidas, envia materiais e aplica quizzes. O aluno entra com nome e código da sala, envia dúvidas, vota, acessa materiais e responde ao quiz. A consulta das salas acontece a cada três segundos e para ao sair da tela.

O e-mail e a senha do professor ficam somente na memória da sessão aberta, para autenticar os pedidos. O token do aluno também fica em memória. Ao sair ou recarregar o aplicativo, será preciso entrar novamente.

A senha do MySQL é configurada somente no backend. O arquivo de ambiente do aplicativo recebe apenas o endereço da API.

## Verificar

```powershell
npm run typecheck
npx expo install --check
npx expo export --platform all
```

O primeiro comando confere os tipos. O último gera os pacotes de código para Android, iOS e a prévia web; ele não instala o app em um celular. Estes comandos também funcionam com pnpm, usado na validação local deste projeto.

As 13 telas foram exercitadas na prévia com a API e o MySQL. A execução em aparelho físico ou emulador ainda deve ser conferida, especialmente para arquivos, compartilhamento, cópia e abertura de links.

Consulte o [README principal](../README.md) para configurar o banco e iniciar o backend.
