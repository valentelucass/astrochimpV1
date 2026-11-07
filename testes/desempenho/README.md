# Desempenho e qualidade mobile: procedimento reproduzível

**Estado em 02/11/2026:** procedimento preparado, ainda sem execução em Android ou carga da API. As metas são propostas, sujeitas à aprovação do grupo/professor. Checagem de sintaxe dos dois scripts aprovada; execução real depende das ferramentas e do ambiente abaixo.

## Ferramentas escolhidas

| Ferramenta conhecida | Finalidade | Limite |
| --- | --- | --- |
| Android Studio Profiler + ADB | CPU, memória, quadros, diagnóstico de abertura do app Android | Exige aparelho/emulador e pacote próprio instalado. Emulador deve ser identificado como tal. |
| React Native DevTools / React Profiler | Localizar renderizações e custo de JavaScript em desenvolvimento | Diagnóstico complementar; os tempos de debug/Expo Go não aprovam o desempenho release. |
| Grafana k6 OSS | Latência, erros e volume das consultas HTTP da API | Não mede desenho de tela, FPS ou desempenho do aparelho. |
| TalkBack | Conferir rótulos, foco e operação com leitor de tela Android | Não substitui avaliação de usabilidade com pessoas. |
| Expo CLI, TypeScript, Node Test Runner, pnpm audit | Compatibilidade de dependências, tipos, funções e alertas de segurança | Não equivalem a testes do aplicativo em aparelho. |

## Ambiente de teste isolado

1. Preserve o checkout em uso. Prepare uma cópia de trabalho do estado atual, incluindo alterações locais necessárias; registre commit base, diferenças e hashes. Um clone apenas do commit não contém mudanças ainda não commitadas.
2. Use um banco MySQL **novo e exclusivo**, por exemplo `astrochimp_perf_<sufixo_unico>`, e usuário que tenha acesso somente a ele. Não reutilize `astrochimp` nem `astrochimp_test`. Confira `SHOW GRANTS` do usuário antes de iniciar a API. Não copie dados reais.
3. Inicie outra instância da API em `127.0.0.1:18080`, com uploads em pasta própria. A senha fica apenas na configuração local/ambiente, fora do Git. Confira o banco e a porta efetivamente usados pelo processo. Não execute a classe de integração: ela fixa `astrochimp_test` e usa `create-drop`.
4. Use um artefato correspondente aos fontes registrados. O JAR encontrado em `backend/target` em 02/11 foi gerado em 27/10; não o trate como build desta revisão. Nenhum build foi feito nesta frente. Se for necessário gerar outro artefato futuramente, faça na cópia de trabalho e omita a execução da integração destrutiva.

Exemplo de configuração **futura**, no terminal exclusivo da API, depois da criação do banco e de seu usuário restrito:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = '<senha local do usuário exclusivo>'
java -jar '<JAR correspondente aos fontes registrados>' `
  '--server.address=127.0.0.1' '--server.port=18080' `
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/astrochimp_perf_<sufixo_unico>' `
  '--spring.datasource.username=<usuario_exclusivo>' `
  '--spring.jpa.hibernate.ddl-auto=update' `
  '--astrochimp.upload-dir=<pasta exclusiva de uploads sintéticos>'
```

Os valores entre `<...>` devem ser preenchidos. O exemplo não foi executado. `update` permite criar tabelas somente no banco novo ao qual o usuário tem acesso; não é autorização para usar um banco existente. Não há limpeza automática de banco nestes scripts.

## Preparar o Android

1. Disponibilize Android Studio e SDK Platform Tools. Prefira um aparelho físico de teste; para o Profiler, siga os requisitos atuais de versão do Android/SDK da documentação oficial. Anote Android Studio, ADB, SDK, modelo, Android/API, RAM, resolução, frequência de tela, bateria, rede e temperatura/estado térmico. Para emulador, registre também AVD, imagem, CPU/RAM alocada e host.
2. Conecte o aparelho e autorize depuração USB. Execute `adb devices -l`: o alvo deve aparecer como `device`, não `unauthorized` ou `offline`. Se houver mais de um, selecione sempre `-s <serial>`.
3. Na cópia de trabalho do frontend, configure um pacote de teste próprio em `expo.android.package` — sugestão: `edu.senacsp.astrochimp.perf` — e registre essa diferença de configuração. O projeto atual não declara `android.package` nem contém projeto Android nativo.
4. Para acessar a API isolada pelo cabo, execute `adb -s <serial> reverse tcp:18080 tcp:18080`. Defina `EXPO_PUBLIC_API_URL=http://127.0.0.1:18080` **antes de gerar o bundle**. Confira o endereço no aplicativo. Se a variante de teste bloquear HTTP, configure a permissão de tráfego local somente no manifesto dessa cópia/variante; registre a diferença. Esse ajuste não foi feito no projeto original.
5. Com o SDK preparado, a geração futura pode ser feita na cópia: `pnpm exec expo prebuild --platform android --no-install`. No manifesto nativo dessa variante, confira `<profileable android:shell="true" />` sob `<application>`. Abra o projeto Android no Android Studio, selecione `release` e use **Profile 'app' with low overhead**. O fluxo Expo `pnpm exec expo run:android --variant release --device` também gera/instala um release, mas o manifesto e o processo efetivamente perfilado precisam ser conferidos. Nenhum desses comandos de build foi executado nesta frente.
6. Não use o pacote `host.exp.exponent` do Expo Go como se fosse o release próprio. Se só houver Expo Go, registre apenas uma sessão exploratória de desenvolvimento. Xcode/Instruments são a alternativa futura para iOS em macOS; iOS não foi testado neste Windows.

Documentação: [Android Studio Profiler](https://developer.android.com/studio/profile), [Expo e compilação local](https://docs.expo.dev/guides/local-app-development/), [desempenho do React Native 0.86](https://reactnative.dev/docs/0.86/performance).

## Cenários e coletas no Android

Prepare somente dados fictícios: professor, sala ativa, dez alunos, dez dúvidas, cinco materiais e quiz ativo. Registre a quantidade efetiva; ela influencia o custo. Aquecimento, medições sem Profiler e diagnóstico com Profiler devem ficar em séries separadas.

### Abertura

- Com o pacote próprio instalado, faça uma série exploratória de 30 aberturas com processo interrompido entre tentativas. Mantenha caches/dados persistidos; isso não é uma primeira instalação. Anote também uma abertura com processo já existente separadamente.
- Exemplo, a partir da raiz do projeto:

```powershell
.\testes\desempenho\Coletar-Android.ps1 `
  -DeviceSerial '<serial>' -PackageName 'edu.senacsp.astrochimp.perf' `
  -PastaSaida '<pasta de evidências>' -Etapa depois `
  -MedirAbertura -Repeticoes 30 -ConfirmoAplicativoDeTeste
```

- Guarde todos os `am start -S -W`, o CSV e os casos sem tempo/sucesso. Calcule mediana e p95 pelo método do posto mais próximo (`ceil(0,95*n)`) e informe `n`. Amostra pequena não estabelece SLA.
- `TotalTime`/primeira exibição da Activity não comprova que o React, as fontes e os dados estão prontos. `App.tsx` aguarda Nunito e pode mostrar uma espera inicial. Para tempo até uso real, grave uma série separada em vídeo com referência de tempo, do lançamento até aparecerem os campos e o botão **ENTRAR NA SALA** responsivos. Informe resolução temporal, método e eventual custo da gravação. O projeto não ganhou instrumentação `reportFullyDrawn`; não atribua TTFD automático a esta coleta.

Veja [primeira exibição e tela completamente pronta no Android](https://developer.android.com/topic/performance/issues/launch-time).

### Navegação, quadros e memória

1. Abra a sala com o aluno sintético. Execute o coletor com `-Etapa antes`, sem `-MedirAbertura`: ele registra memória e reinicia somente as estatísticas de quadros do pacote.
2. Repita dez vezes o mesmo percurso: sala → materiais → sala → quiz → sala. Faça a mesma rolagem e registre duração, dados carregados e condições de rede. Não junte abertura com a medição de navegação.
3. Execute o coletor com `-Etapa depois`, sem `-MedirAbertura`. Guarde `memoria.txt`, `quadros.txt`, `pacote.txt` e `ambiente.json` de ambas as etapas.
4. Em sessão separada do Android Studio Profiler, grave o mesmo percurso em **System Trace** e acompanhe CPU/memória. Exporte a gravação original e uma imagem com os trechos selecionados. Anote tempo de gravação e configuração. O coletor ADB não captura CPU nem faz análise automática de travamentos.
5. Compare PSS antes/depois e procure crescimento contínuo em três séries equivalentes após estabilização. A diferença isolada não prova vazamento; o runtime e o cache variam.
6. Para quadros, use a janela do cenário e o orçamento associado à frequência real da tela. `gfxinfo` contém apenas os quadros recentes disponíveis: reporte cobertura e perda de amostras. Se houver dados incompletos ou renderização não coberta, use o trace; não converta ausência de quadros em zero travamentos. Não trate o FPS de um monitor de debug como aprovação do release.

Veja [ADB/dumpsys para memória e quadros](https://developer.android.com/tools/dumpsys). O script não avalia metas automaticamente e ainda precisa ser exercitado em um dispositivo real.

## Carga da API com Grafana k6

O k6 não foi instalado nesta rodada. Depois de instalar sua distribuição oficial e preparar a API isolada, anote `k6 version` e configure os dados abaixo. O script aceita somente `http://127.0.0.1:18080`, desabilita redirecionamentos e faz somente GET. Isso reduz erros de destino, mas não comprova qual banco está atrás do servidor.

1. Pela interface ligada à API isolada, crie o professor/sala e dados fictícios. Mantenha um quiz em andamento; a rota da pergunta atual devolve 404 sem quiz, e o cenário espera 200. A sala deve continuar ativa durante toda a coleta.
2. Registre os tokens de alunos sintéticos. Um usuário virtual usa um token distinto. Para criar dez participações novas na **sala sintética já conferida**, este trecho futuro faz apenas o preparo da massa, antes de medir:

```powershell
$codigoSintetico = '<código da sala isolada>'
$entradas = @(1..10 | ForEach-Object {
  $pedido = @{ codigo = $codigoSintetico; nomeAluno = "AlunoTeste$_" } | ConvertTo-Json
  Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:18080/api/salas/entrar' `
    -ContentType 'application/json' -Body $pedido
})
$env:BASE_URL = 'http://127.0.0.1:18080'
$env:CONFIRMO_AMBIENTE_ISOLADO = 'SIM'
$env:SALA_ID = '<id da sala sintética>'
$env:TOKENS_ALUNOS = ConvertTo-Json -InputObject @($entradas.tokenAluno) -Compress
```

3. Não publique o ambiente com tokens ou senhas. Guarde apenas dados desidentificados, configuração da massa e métricas.
4. Rode primeiro um teste curto: `VUS=1` e `SEGUNDOS=10`. Ele verifica autorização, respostas e massa. Faça um aquecimento separado, que não entra nos resultados reportados. Em seguida, três execuções medidas, cada uma com `VUS=10` e `SEGUNDOS=60`:

```powershell
$env:VUS = '10'
$env:SEGUNDOS = '60'
k6 version
k6 run --out json=metricas-k6-rodada1.json `
  .\testes\desempenho\api-leitura.k6.js 2>&1 |
  Tee-Object -FilePath console-k6-rodada1.txt
$codigoSaidaK6 = $LASTEXITCODE
```

Use nomes distintos nas três rodadas. Guarde o código de saída, p50/p95, taxa de erro, checks, duração, requisições/segundo e distribuição por rota. Não use `--http-debug`, que pode revelar tokens. A saída JSON também deve ser conferida antes de compartilhar.

O percurso consulta sala, dúvidas/materiais em paralelo, depois cronômetro/quiz em paralelo. Há um intervalo mínimo de três segundos por ciclo. Esse modelo foi inspirado em `SalaAluno.tsx`; ele não executa JavaScript de UI, renderização ou todo o comportamento do aplicativo. Se a API atrasar, a carga obtida pode cair. Reporte volume efetivo e saturação do gerador; dez VUs não provam suporte a uma escola inteira. O cenário não mede login, criação, votos, envio de respostas, upload ou download de arquivos. Esses fluxos ficam para cenários separados em ambiente isolado.

Documentação: [execução local k6](https://grafana.com/docs/k6/latest/using-k6/run-k6-test-script/), [critérios de aprovação](https://grafana.com/docs/k6/latest/using-k6/thresholds/), [consultas paralelas](https://grafana.com/docs/k6/latest/javascript-api/k6-http/batch/).

## Metas propostas, ainda sem resultado

| Aspecto | Proposta inicial | Como avaliar |
| --- | --- | --- |
| API de leitura | p95 < 500 ms e erros HTTP < 1%, com todos os checks aprovados | k6, 10 VUs por 60 s, três execuções após aquecimento; informar por rota |
| Primeira exibição Android | p95 ≤ 3 s | 30 aberturas, processo parado, pacote release; informar dispositivo e amostras |
| Tela inicial pronta | p95 ≤ 3 s | Série separada, botão/campos responsivos; vídeo cronometrado ou instrumentação futura |
| Fluidez | menos de 5% de quadros acima do orçamento da tela na janela definida | Trace do mesmo percurso, frequência da tela e cobertura informadas |
| Memória | sem crescimento contínuo sem estabilização em três séries equivalentes | PSS e trace; investigar tendências, sem teto arbitrário universal |
| Estabilidade | nenhum crash/ANR durante o roteiro observado | Monitorar Logcat/relatórios, registrar janela e falhas; não extrapolar taxa de produção |
| Acessibilidade | concluir entrada, abrir materiais e responder quiz com TalkBack | Verificar foco, rótulos e leitura de erros manualmente; registrar barreiras |

Esses números são critérios escolhidos para iniciar a discussão do trabalho, não limites universais do Android, da ISO ou resultados já alcançados. Antes do aceite, o grupo precisa aprová-los e definir o aparelho-alvo.

## Outras verificações de qualidade

Em um aparelho de teste, ative TalkBack em Configurações → Acessibilidade. Percorra os três fluxos acima e registre elementos sem nome, foco fora de ordem, informação transmitida só por cor e mensagens de erro não anunciadas. Confira ainda teclado cobrindo campos, fonte ampliada, botão Voltar e retomada após ir ao segundo plano. Depois desative o serviço se ele não for usado pelo responsável. Ainda não houve essa execução nem sessão de usabilidade com pessoas.

Referência: [testes de acessibilidade Android e TalkBack](https://developer.android.com/guide/topics/ui/accessibility/testing).

Para repetir as checagens sem aparelho, na pasta `frontend`, use `CI=1` na sessão do terminal antes de `pnpm exec expo install --check`, além de `pnpm run typecheck`, `pnpm test` e `pnpm audit --prod --json`. No PowerShell, configure `$env:CI='1'`. Não execute `install --fix` ou `audit --fix` apenas para produzir um resultado sem alertas: atualizações exigem análise de compatibilidade. O total de testes distintos não aumenta ao repetir os mesmos casos.
