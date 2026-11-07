# Resolução dos quatro alertas restantes do AstroChimp

Os quatro alertas foram encerrados tecnicamente em **2 de novembro de 2026**: **três corrigidos por atualização seletiva validada e um classificado como aviso de CVE oficialmente rejeitado**, com exceção explícita e rastreável. A auditoria configurada passa, sem avisos acionáveis. A auditoria sem a exceção ainda reproduz um aviso baixo de `deepmerge` na base npm; isso não foi apresentado como correção do pacote.

Considerando a primeira etapa, o resultado dos 16 avisos originais é **15 corrigidos por atualizações e 1 encerrado por rejeição oficial do CVE**. Não há mais correção de dependências pendente para esses quatro achados na configuração validada.

| Aviso | Mudança e resultado |
| --- | --- |
| [Zod GHSA-mrhx-c7xc-m3w5](https://github.com/advisories/GHSA-mrhx-c7xc-m3w5) | `3.25.76 → 4.6.7`, somente no consumidor `@expo/cli@57.0.32`. Aviso alto deixou de aparecer. |
| [Glob GHSA-3g3m-2fjr-6pw4](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4) | `13.0.6 → 14.0.3` nos cinco consumidores do Expo. Aviso moderado deixou de aparecer. |
| [Minimatch GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249) | `10.2.6 → 11.0.4`, pelo Glob corrigido e em `@expo/fingerprint@0.20.14`. Aviso moderado deixou de aparecer. |
| [Deepmerge GHSA-gcqx-w48m-w2h4](https://github.com/advisories/GHSA-gcqx-w48m-w2h4) | Pacote mantido em `4.3.1`. O CVE-2026-93753 está oficialmente `REJECTED`; o ID do aviso foi excepcionado com justificativa e fonte na configuração. |

## Rejeição oficial do CVE de Deepmerge

O [registro oficial CVE-2026-93753](https://raw.githubusercontent.com/CVEProject/cvelistV5/main/cves/2026/93xxx/CVE-2026-93753.json) informa estado `REJECTED`, com rejeição em **02/11/2026 às 13h33 no horário de Brasília**. A CNA VulnCheck registra que o caso não descreve uma vulnerabilidade de segurança. O JSON da [API oficial do programa CVE](https://cveawg.mitre.org/api/cve/CVE-2026-93753) foi salvo integralmente como evidência.

Foi adicionada uma exceção somente para **GHSA-gcqx-w48m-w2h4**, com o motivo e a URL oficial em `frontend/pnpm-workspace.yaml`. Não houve filtro geral por gravidade, exclusão de todos os avisos sem correção nem alteração da versão de Deepmerge para disfarçar a ocorrência. A exceção deve ser removida quando a base npm retirar o aviso rejeitado. A discussão no [repositório de Deepmerge](https://github.com/TehShrike/deepmerge/issues/273) também registra a rejeição; o registro CVE é a evidência decisiva.

O efeito de mesclagem discutido no aviso não foi confundido com modificação global de `Object.prototype` ou demonstração de exploração no AstroChimp. O encerramento deste achado decorre da rejeição do registro; não certifica todos os usos possíveis da biblioteca.

## Substituições seletivas e compatibilidade

As versões corrigidas ficam fora das faixas originais dos consumidores. Por isso, a primeira análise as deixou abertas. Nesta reavaliação, foram testadas explicitamente como `overrides`, que é uma alternativa documentada pelo [pnpm para avisos que uma atualização comum não resolve](https://pnpm.io/cli/audit).

Foram fixadas sete regras em `frontend/pnpm-workspace.yaml`, restritas aos consumidores e versões atuais:

```yaml
overrides:
  '@expo/cli@57.0.32>zod': '4.6.7'
  '@expo/cli@57.0.32>glob': '14.0.3'
  '@expo/config@57.0.10>glob': '14.0.3'
  '@expo/config-plugins@57.0.11>glob': '14.0.3'
  '@expo/metro-config@57.0.13>glob': '14.0.3'
  '@expo/fingerprint@0.20.14>glob': '14.0.3'
  '@expo/fingerprint@0.20.14>minimatch': '11.0.4'
```

O SDK continua em **Expo 57.0.31**, CLI **57.0.32**, React Native **0.86.3**, Node **24.22.1** e pnpm **11.25.0**. O `node-forge` permanece corrigido em **1.4.2**. O lockfile preserva versões e integridades. A resolução do Glob também trouxe `path-scurry 3.0.0` e `@isaacs/nonrecursive-glob-match 1.0.3`, sem alterações em código da interface.

O pnpm acrescentou automaticamente uma exceção de idade mínima para a versão exata `minimatch@11.0.4`, registrada no mesmo arquivo. Não foi reduzida globalmente a idade mínima nem desativada a verificação de integridade. A versão está fixa no lockfile e foi exercitada nas verificações descritas abaixo.

Há mudanças reais entre as versões principais: o [changelog oficial de Glob 14](https://raw.githubusercontent.com/isaacs/node-glob/v14.0.3/changelog.md) documenta alteração no mecanismo de correspondência, APIs internas e padrões de exclusão; o [guia do Zod 4](https://zod.dev/v4/changelog) documenta mudanças de schema e erros. A compatibilidade foi avaliada nos consumidores concretos usados pelo Expo, incluindo os parsers de plugins. O teste não se limitou a verificar se o número de avisos caiu.

## Validações realizadas

Primeiro foi usada uma cópia isolada do frontend. As substituições passaram nela antes de serem aplicadas. Depois que a frente mobile informou ter concluído sua instalação, a configuração foi aplicada no projeto compartilhado sobre o lockfile já atualizado por ela. O manifesto foi comparado byte a byte antes e depois desta aplicação: **preservado, inclusive `react-native-safe-area-context ~5.7.0`**. Nenhum arquivo de tela, componente, cor, serviço ou banco foi editado nesta reavaliação.

| Verificação | Resultado real |
| --- | --- |
| Instalação das substituições no projeto | Aprovada, saída 0 |
| TypeScript estrito após a aplicação | Aprovado, saída 0 |
| `expo install --check` após a aplicação | Dependências compatíveis, saída 0 |
| Três testes frontend existentes | 3 aprovados, 0 falhas |
| Exportação de produção Android, iOS e web | Aprovada na cópia isolada e novamente no projeto atualizado, saída 0 |
| Consumidores de Zod na CLI | Schemas de plugins, construtor real de plugin, leitura de resultados e schema de parâmetros MCP aceitaram e rejeitaram as entradas esperadas |
| Regressão da correção de Zod | Array de 1.000 elementos com limite máximo de 10 foi recusado com um erro, sem visitar elementos |
| Seleção de arquivos | 61 verificações de padrões Glob e regras de fingerprint; antes da aplicação, comparação entre consumidores antigos e novos preservou os resultados da amostra |
| Caminhos síncronos e assíncronos de Glob | Resultados iguais nos padrões de teste |
| Cancelamento de Glob | Sinal previamente cancelado rejeitado sem nenhuma chamada a `readdir`; utilitário real `anyMatchAsync` do Expo retornou um arquivo |
| Geração real de fingerprint Expo | Aprovada na cópia isolada, 34 fontes, hash `e1e26922124865001d6ce39130171c9666ff67b6` |
| Auditoria normal e de produção configuradas | Saída 0 e objeto `advisories` vazio |
| Auditoria normal e de produção sem exceção | Saída 1; somente o aviso baixo de Deepmerge cujo CVE foi rejeitado |

A validação dos consumidores inclui casos válidos e inválidos, tipos dos parâmetros, comando inexistente e propriedade extra, além da leitura de saídas de plugin em texto, imagem e áudio. Não foram executados comandos de plugins remotos nem feitas conexões com serviços externos para essas verificações.

As 61 verificações de padrões são uma amostra de regressão de dependências, e não 61 novos casos funcionais da ADO 3. Os três testes de aplicação já existiam. O total de casos distintos do trabalho deve continuar sob controle da revisão. Não foram executados testes de banco, teste de carga nem avaliação de usabilidade nesta frente.

### Como interpretar o JSON do pnpm 11

Nesta versão do pnpm, a exceção retira o aviso do objeto `advisories` e permite saída 0, mas `metadata.vulnerabilities.low` continua mostrando **1**. Portanto, o resultado correto é **zero avisos acionáveis e um aviso rejeitado excepcionado**, e não “todos os campos do JSON são zero”. Os relatórios configurados e os relatórios sem exceção foram salvos separadamente para tornar essa diferença verificável.

## Limites e manutenção

As verificações confirmam o comportamento exercitado no ambiente Windows com Node 24 e os consumidores atuais do Expo 57. Não foi realizado build nativo completo, teste em aparelho ou teste de todos os padrões e plugins possíveis nesta frente. A exportação gera bundles de produção; não equivale a instalar um APK/IPA. A frente mobile conduz sua própria avaliação a partir do snapshot que identificou.

Ao atualizar Expo, CLI ou fingerprint, revisar os seletores de versão exata e repetir essas verificações; não transportar as substituições automaticamente para outro SDK. Caso um fluxo de ferramenta não exercitado apresente regressão, retirar somente a regra responsável e reavaliar, preservando as mudanças de interface e `react-native-safe-area-context`. Os arquivos anteriores desta intervenção estão nas evidências. Uma instalação com lockfile congelado deve continuar usando as versões verificadas.

## Evidências e atualização dos documentos

As evidências estão em `evidencias-remanescentes`: auditoria inicial com quatro avisos, auditorias finais configuradas e sem exceção, registro CVE oficial, versões resolvidas, configuração aplicada, lockfile antes/depois, diferença exclusiva desta intervenção, logs de instalação, tipos, Expo, testes, exportações e verificações dos consumidores. `resumo-final.json` separa avisos corrigidos, rejeitado, acionáveis e contagens brutas; `manifesto-final-sha256.json` identifica os arquivos finais.

No projeto compartilhado, o relatório está em `testes/dependencias/RESOLUCAO-REMANESCENTES-2026-11-02.md`. A análise anterior dos 16 avisos continua como histórico das cadeias e dos primeiros testes. Este relatório substitui seu estado de quatro pendências.

**Texto factual para a revisão dos PDFs e checklist:** os 16 alertas originais foram tratados; 15 corrigidos por atualização, 1 encerrado por rejeição oficial do CVE. Auditoria passa com exceção exclusiva documentada; a consulta sem exceção ainda retorna esse único aviso rejeitado. Expo 57 mantido, tipos e exportação Android/iOS/web aprovados. Não aumentar o total de testes funcionais por repetir as verificações.
