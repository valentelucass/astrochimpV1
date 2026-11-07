# Resultados complementares dos testes da ADO 3

Em 2 de novembro de 2026, a suíte completa do backend terminou às 16h47min04s, no horário de São Paulo, com **54 testes aprovados, zero falhas, zero erros e zero ignorados**. Foram acrescentados 13 testes distintos: 12 de integração por HTTP real e um unitário. A rodada também repetiu os 41 testes anteriores do backend.

Com os três testes de formatação do frontend já registrados, a base consolidada passa de **44 para 57 testes distintos**. O frontend não foi executado por esta frente. Repetições e verificações de tipos não aumentam essa contagem.

## Resultado por classe

| Classe | Testes aprovados | Papel nesta rodada |
| --- | ---: | --- |
| `BackendIntegrationTest` | 36 | Regressão existente, incluindo dúvidas com 280 e 281 caracteres |
| `AcessoServiceTest` | 5 | Regressão unitária existente |
| `LimitesHttpIntegrationTest` | 12 | Novos cenários por HTTP, com Tomcat e MySQL reais |
| `MaterialServiceTest` | 1 | Nova regra de tamanho do arquivo, sem servidor nem banco |
| **Backend executado nesta rodada** | **54** | **Zero falhas, erros ou ignorados** |

## Casos novos executados

| Cenário | Resultado confirmado | Método |
| --- | --- | --- |
| Dúvida com 1 caractere acentuado | 201; resposta e MySQL preservam o texto | `duvidaComUmCaractereEPersistidaSemAlterarTexto` |
| Dúvida com 279 caracteres acentuados | 201; resposta e MySQL preservam o texto | `duvidaCom279CaracteresEPersistidaSemAlterarTexto` |
| Dúvida com texto nulo | 400; contagem de dúvidas permanece igual | `duvidaNulaRetorna400SemGravarRegistro` |
| Quiz com 6 alternativas | 201; letras A a F; sexta opção correta; aluno recebe seis opções sem gabarito, responde F e resultado conta uma resposta | `quizComSeisAlternativasPermiteResponderALetraF` |
| Quiz com 7 alternativas | 400; não salva quiz, pergunta ou alternativa, inclusive quando a primeira pergunta do pedido é válida | `quizComSeteAlternativasRetorna400SemSalvarPerguntasOuOpcoes` |
| Criar e enviar quiz com 7 alternativas | 400; não salva quiz nem inicia rodada; pergunta atual continua 404 | `criarEEnviarQuizComSeteAlternativasNaoIniciaRodada` |
| Upload de arquivo de 1 byte | 201; download idêntico e tamanho correto no disco | `uploadDeUmBytePreservaConteudoNoDownload` |
| Requisição inteira com exatamente 10 MiB | 201; arquivo de 10.485.517 bytes e download idêntico | `uploadComRequisicaoExatamente10MiBEAceito` |
| Requisição inteira com 10 MiB mais 1 byte | 201; arquivo ainda abaixo de 10 MiB e download idêntico | `uploadComRequisicaoUmByteAcimaDe10MiBAceitaEspacoDoFormulario` |
| Arquivo de exatamente 10 MiB | 201; 10.485.760 bytes preservados no disco e no download | `uploadDeArquivoExatamente10MiBPreservaConteudoNoDownload` |
| Arquivo de 10 MiB mais 1 byte | 413; sem novo material nem arquivo | `uploadDeArquivoUmByteAcimaDe10MiBRetorna413` |
| Requisição inteira com 11 MiB mais 1 byte | 413; arquivo respeita 10 MiB e um campo adicional ultrapassa somente o limite total; sem novo material nem arquivo | `uploadComRequisicaoUmByteAcimaDe11MiBRetorna413` |
| Chamada direta ao serviço com arquivo acima de 10 MiB | 413 antes de gravar; repositórios não recebem operações e pasta continua vazia | `arquivoAcimaDe10MiBERecusadoAntesDeGravarMesmoSemServidorHttp` |

Os vários pedidos e verificações dentro de um mesmo método contam como um teste. As fronteiras de 280 e 281 caracteres foram reexecutadas no método existente `duvidaAceita280CaracteresRecusa281EPermiteTodosOsEstados`; não receberam testes duplicados. O caso existente de upload vazio também passou novamente.

## Correção do limite de upload

A primeira execução encontrou um limite configurado de 10 MiB tanto para o arquivo quanto para a requisição inteira. O formulário usado acrescenta 243 bytes, por isso um arquivo de exatamente 10 MiB recebia 413. Essa rodada inicial aprovou 52 testes que descreviam o comportamento observado e está preservada apenas como evidência anterior à correção.

Após solicitação do supervisor, `spring.servlet.multipart.max-file-size` permaneceu em `10MB` e `spring.servlet.multipart.max-request-size` passou para `11MB`. `MaterialService.enviarArquivo` também passou a recusar `arquivo.getSize() > 10 * 1024 * 1024` com HTTP 413 antes de criar ou copiar o arquivo. Assim, chamadas ao serviço também respeitam o limite.

Os testes HTTP foram ajustados para exigir aceitação de arquivo de exatamente 10 MiB. O caso de requisição acima de 11 MiB e o teste unitário do serviço foram acrescentados. A suíte completa foi então executada novamente, com 54 aprovações. Não se somam os 52 testes da primeira rodada aos 54 finais. Nesta configuração, 1 MiB corresponde a 1.048.576 bytes.

## Ambiente e isolamento

Execução em Windows, Java Temurin 25.0.5, compilação para Java 17, Spring Boot 4.1.1 e MySQL 8.4.13. O Maven terminou com `BUILD SUCCESS` em 36,957 segundos. Esse tempo é da suíte e não é uma medição de desempenho da aplicação.

Antes de cada rodada, consultas somente de leitura em `information_schema.tables` confirmaram zero tabelas em `astrochimp_test` e `astrochimp_casos_20261102`, ambos no MySQL local da porta 3306. As credenciais vieram da configuração privada existente e não foram copiadas para as evidências.

Os testes anteriores mantiveram seu banco exclusivo `astrochimp_test`. A classe nova fixou `astrochimp_casos_20261102`, conferiu o banco em uso e incluiu uma barreira antes da inicialização do Hibernate: se esse banco já tiver tabelas, a execução é interrompida. Ambos usam `create-drop` somente em seus bancos de teste. Nenhum teste desta frente apontou para o banco principal `astrochimp`.

O servidor HTTP novo escutou somente em `127.0.0.1`, na porta temporária 57006 na execução final. Os uploads novos usaram uma subpasta exclusiva criada em `backend/target/casos-test-uploads`. Os downloads foram comparados byte a byte. Ao terminar, o servidor e os pools encerraram, as consultas mostraram novamente zero tabelas nos dois bancos de teste e havia zero arquivos na pasta de uploads dos casos novos. As pastas vazias foram preservadas.

O uso do backend e dos bancos foi coordenado com a frente mobile. Ela reservou outra instância MySQL, porta 13307, e uma cópia do backend com saída de compilação própria. As janelas de Maven desta frente foram concluídas antes da liberação para a carga medida.

## Comandos e evidências

Na pasta `C:/Users/lucas/OneDrive/Documentos/projetos/astrochimp/backend`, após conferir que os dois bancos exclusivos estavam sem tabelas:

```powershell
.\mvnw.cmd test
```

Para executar somente os testes novos, a mesma conferência prévia deve preceder:

```powershell
.\mvnw.cmd "-Dtest=LimitesHttpIntegrationTest,MaterialServiceTest" test
```

Para executar somente o novo teste unitário, sem banco:

```powershell
.\mvnw.cmd "-Dtest=MaterialServiceTest" test
```

A consulta de isolamento foi `SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?`, usando exclusivamente os dois nomes de bancos de teste. Não houve limpeza manual de tabelas ou exclusão de banco.

As evidências preservadas estão em `C:/Users/lucas/Documents/Codex/2026-11-02/astrochimp-ado-casos/outputs/evidencias`:

- `maven-test-correcao-upload.log`: execução final e `BUILD SUCCESS` com 54 testes.
- `surefire-54-testes-final/`: TXT e XML finais das quatro classes.
- `resumo-54-testes-final.json`: classes, contagens e nomes dos métodos extraídos dos XML.
- `preflight-correcao-banco.txt` e `postflight-correcao-banco.txt`: ausência de tabelas antes e depois da rodada final.
- `maven-test-rodada1.log`, `surefire-52-testes/` e `resumo-52-testes.json`: rodada anterior à correção; não representam o comportamento final do upload.
- `surefire-anterior/`, quando havia arquivos: cópia dos relatórios encontrados antes das novas execuções.
- `fontes-finais/`: cópias dos quatro fontes alterados ou criados nesta frente.
- `manifesto-sha256.csv`: hashes dos arquivos de evidência e da cópia final do relatório.

Os logs incluem avisos do Hibernate ao tentar remover restrições de tabelas ainda inexistentes no banco vazio e avisos dos pedidos inválidos que os testes provocam. Os relatórios finais registram zero falhas e zero erros de teste.

## Cobertura do grafo

O Graphify foi consultado com os termos presentes no vocabulário `duvida`, `quiz`, `material`, `upload` e `alternativas`; os percursos exibidos foram limitados por orçamento e os fontes foram lidos para verificar as regras. O comando nativo `graphify update .` atualizou a extração estrutural local após a mudança: 1.141 nós, 2.798 relações e 55 comunidades. Essa extração não utilizou LLM, não teve consumo de tokens de API e não renovou a análise semântica dos documentos. O extrator informou ausência de símbolos em `backend/pom.xml`, `frontend/index.ts` e `frontend/tests/formatacao.test.ts`; isso limita o grafo e não substitui as verificações executadas. Os logs do grafo estão preservados nas evidências.

## Limitações e continuidade

Esta frente concluiu os limites de dúvida, quantidade de alternativas, tamanho de upload e verificações de persistência associadas. As fronteiras HTTP do upload foram testadas pela rota de materiais do professor; a rota da sala compartilha o mesmo serviço, mas não recebeu nesta rodada uma matriz independente de todos os tamanhos.

Nenhuma jornada foi executada pela interface do aplicativo nesta frente. Não houve sessão com usuários, teste de usabilidade com pessoas, aceite pelo professor ou execução em aparelho físico. A frente mobile e a frente de dependências possuem evidências próprias; seus resultados devem ser consolidados separadamente. A aprovação destes testes não representa aceite do produto.

O arquivo histórico `testes/RESULTADOS-2026-11-02.md` e os PDFs finais foram preservados. Este complemento e os logs finais devem orientar a atualização do checklist, dos números e das limitações pela revisão. Não houve commit nem publicação nesta frente.
