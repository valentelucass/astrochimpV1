# Testes do AstroChimp

Os testes ficam junto das partes que verificam:

- `backend/src/test/java/edu/senacsp/astrochimp/AcessoServiceTest.java`: cinco testes unitários de regras de acesso, sem banco de dados.
- `backend/src/test/java/edu/senacsp/astrochimp/BackendIntegrationTest.java`: testes das rotas da API com MySQL, incluindo salas, dúvidas, materiais, quizzes e segurança.
- `frontend/tests/formatacao.test.ts`: três testes unitários de formatação, com listas de exemplos.

## Executar o backend

Na pasta `backend`, execute:

```powershell
.\mvnw.cmd test
```

Os testes de integração usam o banco **`astrochimp_test`** e recriam suas tabelas. Esse banco deve ser exclusivo para testes e não deve conter dados a preservar. Eles usam as credenciais configuradas no backend e a pasta `backend/target/test-uploads`. Não executam testes no banco `astrochimp` das aulas. Para rodar apenas os testes unitários, sem banco:

```powershell
.\mvnw.cmd "-Dtest=AcessoServiceTest" test
```

O Maven grava os resultados em `backend/target/surefire-reports`.

## Executar o frontend

Na pasta `frontend`, use Node.js 24 ou superior e execute:

```powershell
pnpm test
pnpm run typecheck
```

`pnpm test` executa nove testes de formatação e de preparação/validação de textos, com exemplos válidos e limites. Inclui e-mail, código da sala, links, endereço público e nomes de arquivo. `typecheck` confere os tipos do aplicativo e dos testes. Estes testes não substituem a verificação visual e de interação do aplicativo em celulares ou com usuários.

## Resultado registrado em 02/11/2026

- Backend: 36 testes de integração e 5 unitários, todos aprovados.
- Frontend: 3 testes unitários aprovados e verificação de tipos concluída sem erros.
- Testes de desempenho, usabilidade com usuários e compatibilidade em aparelhos físicos não foram executados nesta rodada.

## Complemento dos casos pendentes em 02/11/2026

`RESULTADOS-CASOS-PENDENTES-2026-11-02.md` registra a nova execução completa: 54 testes backend aprovados, zero falhas, erros ou ignorados. São os 41 anteriores e 13 novos: 12 em `LimitesHttpIntegrationTest.java` por HTTP real e um em `MaterialServiceTest.java` sem banco. Somados aos três testes frontend já registrados, são 57 testes distintos; repetições não são somadas.

Os novos testes HTTP usam o banco exclusivo `astrochimp_casos_20261102` e interrompem a inicialização se ele já tiver tabelas. A suíte anterior continua usando `astrochimp_test`; confira que ambos estão sem tabelas antes de executar a suíte completa. Os uploads novos usam subpastas exclusivas em `backend/target/casos-test-uploads`.

O upload agora aceita arquivo de exatamente 10 MiB, reserva até 11 MiB para a requisição com os campos do formulário e recusa arquivo acima de 10 MiB também no serviço. O complemento contém os comandos, as evidências finais, a contagem por cenário e os limites de cobertura. Jornadas pela interface, usabilidade com pessoas e aceite não foram executados por esta frente.
