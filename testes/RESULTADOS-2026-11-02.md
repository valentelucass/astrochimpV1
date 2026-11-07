# Resultados dos testes em 2 de novembro de 2026

Esta página registra a execução local realizada para a ADO 3. Os resultados se referem ao código presente na data da execução e não comprovam o funcionamento em todas as situações.

| Verificação | Resultado |
| --- | --- |
| Backend: testes de integração | 36 aprovados, 0 falhas, 0 erros, 0 ignorados |
| Backend: testes unitários de acesso | 5 aprovados, 0 falhas, 0 erros, 0 ignorados |
| Frontend: testes unitários de formatação | 3 aprovados, 0 falhas, 0 ignorados |
| Frontend: verificação de tipos | Concluída sem erros |

## Como a execução foi feita

No backend, foi executado `mvnw.cmd test` com Java 25, Spring Boot e MySQL 8.4.13. A configuração do projeto compila o código para Java 17. Antes da execução, foi confirmado que o banco exclusivo `astrochimp_test` não tinha tabelas. Os relatórios de execução estão em `backend/target/surefire-reports`; eles são recriados nas próximas execuções.

No frontend, foram executados `pnpm test` e `pnpm run typecheck`. Os três testes de formatação verificam tempo, iniciais e ausência de data. Cada teste pode reunir vários exemplos; portanto, o resultado é de três testes, não de um teste separado para cada exemplo.

## O que foi verificado

Os testes de integração enviaram pedidos de teste à API e verificaram respostas e dados no MySQL. Entre os cenários estão entrada em sala, anonimato e voto único, limite de 280 caracteres em dúvidas, compartilhamento de materiais, quiz, encerramento da sala, proteção de dados e acessos indevidos. O código completo de cada cenário está em `backend/src/test/java/edu/senacsp/astrochimp/BackendIntegrationTest.java`.

Os cinco testes unitários em `AcessoServiceTest.java` verificaram uma sala ativa, salas encerradas ou expiradas, tokens ausentes ou inválidos, diferença entre letras maiúsculas e minúsculas no token e participação do aluno na sala. Esses testes usam repositórios simulados, sem iniciar a API nem acessar o banco.

## Limites da rodada

Não foram executados, nesta rodada, testes de desempenho com metas de tempo ou carga, testes de usabilidade com pessoas, testes de aceitação com o professor, nem testes de compatibilidade em aparelhos físicos. A aprovação dos testes automatizados não prova que todos os requisitos ou todas as telas foram verificados. As falas, a divisão de responsabilidades e as datas restantes devem ser decididas pelo grupo.
