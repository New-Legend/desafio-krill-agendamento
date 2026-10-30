# Relatório de segurança das dependências

Análise de 27/10/2026 (horário de Brasília).

## Ferramentas

| Ferramenta | O que analisa | Como rodar |
|---|---|---|
| **OWASP Dependency-Check** 12.1.6 (plugin Maven) | Bibliotecas Java da API | `cd backend && NVD_API_KEY=<chave> mvn org.owasp:dependency-check-maven:check` → `backend/target/dependency-check-report.html` |
| **Trivy** | Imagens Docker finais (sistema + jars) | `trivy image krill-agendamento-api` / `krill-agendamento-web` |
| **npm audit** | Pacotes do frontend | `cd frontend && npm audit` |

> O Dependency-Check depende da base do NVD (cerca de 415 mil registros). Sem chave de API, o NVD limita o download e a primeira carga ficou parada em 2% depois de vários minutos neste ambiente. Por isso o relatório oficial deve ser gerado com `NVD_API_KEY` (a chave é gratuita em nvd.nist.gov). Para não entregar sem análise, a mesma verificação foi feita com o Trivy, que usa as bases do GitHub Advisory/NVD. A saída completa está em `docs/seguranca/`.

## Resultado

| Alvo | Antes da correção | Depois |
|---|---|---|
| Imagem `web` (nginx) | 58 (56 altas, 2 críticas): base nginx 1.27 antiga | **0** |
| Imagem `api`: sistema (Alpine) | 1 alta (brotli) | **0** |
| Imagem `api`: bibliotecas Java | 63 achados em 19 jars | **4** (2 altas, 2 críticas) que não se aplicam, veja abaixo |
| Frontend em produção (`npm audit --omit=dev`) | 0 | **0** |
| Frontend em desenvolvimento | 4 (vitest/tinypool) | 4, ferramentas de teste que não vão para a imagem |

### O que foi feito

- Spring Boot 3.5.6 → **3.5.16** (Spring Security, Data, Tomcat, Jackson, Micrometer atualizados).
- Versões fixadas no `pom.xml`: Tomcat 10.1.61, Jackson 2.21.8, PostgreSQL JDBC 42.7.14, Netty 4.1.139, BouncyCastle 1.86, jose4j 0.9.6 e async-http-client 2.16.1 (estes três vêm da biblioteca de Web Push).
- Imagens finais com `apk upgrade` e nginx `stable-alpine` sem root.

### Achados restantes (risco aceito, com justificativa)

| CVE | Biblioteca | Por que não afeta | Ação |
|---|---|---|---|
| CVE-2026-47884 (crítica) | spring-webmvc 6.2.19 | Só ocorre com `XsltView`. A API só devolve JSON e não usa views. | Correção apenas no Spring 7 (Boot 4). Migrar quando o Boot 4 estiver estável no projeto |
| CVE-2026-47890 (crítica) | spring-webmvc 6.2.19 | Só ocorre com Server-Sent Events e fragmentos de view, que não são usados | Idem |
| CVE-2026-107227 (alta) | async-http-client 2.16.1 | Afeta WebSocket com compressão. O cliente só faz POST HTTPS para os serviços de push | Correção na 3.x, que não é compatível com `web-push` 5.1.1 |
| CVE-2026-107230 (alta) | async-http-client 2.16.1 | Pool com Kerberos/NTLM/SPNEGO, que não são usados | Idem |

## Outros controles

- Containers sem root (`app` na API, `nginx` uid 101 no web), imagens Alpine mínimas, healthcheck.
- Segredos só por variável de ambiente (`JWT_SECRET`, `DB_PASSWORD`, `VAPID_*`, `BLUESOFT_*`). Veja o `.env.example`.
- Revisão OWASP Top 10 no código: controle de acesso no backend, BCrypt, validação de entrada, consultas JPA parametrizadas, CSV sem injeção de fórmula, CSP e cabeçalhos de segurança, token do fornecedor guardado como hash, auditoria de login.
- Upload do XML da NF-e: parser DOM com `disallow-doctype-decl`, entidades externas e XInclude desligados (sem XXE nem "billion laughs"), limite de 2 MB no Spring (multipart) e na tela, só com a permissão 1207 (Notas fiscais e produto avulso). Teste automatizado com um XML malicioso (`PorteRecebimentoDominioTest`).
- Remarcação pelo link público: o token continua só como hash, a troca é auditada com IP e o servidor recusa faixa sem vaga.
