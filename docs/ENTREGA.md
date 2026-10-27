# Entrega — Teste técnico Rede Krill (Agendamento de Recebimento)

- **Repositório:** `<link do repositório a preencher>`
- **Prazo:** 04/11/2026

## Como rodar

```bash
docker compose up --build
```

- Aplicação: http://localhost:8081. Login `admin` / `admin123` (ADMIN) ou `usuario` / `usuario123` (USUARIO).
- Swagger: http://localhost:8080/swagger-ui/index.html
- E-mails enviados (Mailpit): http://localhost:8025
- Testes: `cd backend && mvn verify` e `cd frontend && npm test`.

## O que foi entregue

| Item pedido | Situação |
|---|---|
| Capacidade 480/dia e 120 por faixa, 4 faixas de 2h, só alerta (sem bloqueio) | ✅ |
| Paletizado / não paletizado (dobro), tipos de caminhão, cadastro de tudo | ✅ |
| Agendamento com todos os campos validados e % de ocupação na data e na faixa | ✅ |
| E-mail ao fornecedor (SMTP por variável de ambiente, Mailpit no compose) | ✅ |
| Relatório por período + CSV | ✅ |
| Perfis ADMIN / USUARIO garantidos no backend | ✅ |
| Auditoria (quem, quando, ação, entidade, IP, inclusive login) | ✅ |
| Java 17+ / Spring Boot 3 / JPA / Security JWT / Flyway / PostgreSQL / Swagger | ✅ |
| Vue 3 + Vite + PrimeVue | ✅ |
| Dockerfiles multi-stage, não-root, nginx + compose (db, api, web, mail) | ✅ |
| JUnit 5, Mockito, Testcontainers, JaCoCo ≥ 80% em domain/application, Vitest | ✅ |
| Clean Architecture, README em pt-BR | ✅ |
| Relatório de segurança (OWASP Dependency-Check) | ✅ configurado, veja [SEGURANCA.md](SEGURANCA.md) |
| **Extra:** PWA instalável + Web Push (novo, cancelado, capacidade excedida, chegada, divergência) | ✅ |
| **Extra:** Início/dashboard, portaria e conferência, link público do fornecedor, vale-palete com assinatura e impressão | ✅ (porte do Gestão CD, veja [PORTE-GESTAO-CD.md](PORTE-GESTAO-CD.md)) |
| **Extra:** Preenchimento pelo ID do pedido via ERP (Bluesoft pronto, desligado) | ✅ [INTEGRACAO-BLUESOFT.md](INTEGRACAO-BLUESOFT.md) |

## Por que PostgreSQL

- É o banco relacional open source mais usado com Spring/JPA e com Flyway, e roda igual no Docker e em qualquer nuvem.
- **Trava por dia** (`pg_advisory_xact_lock`): dois usuários agendando a mesma faixa ao mesmo tempo não geram uma soma errada.
- **JSONB** para os detalhes da auditoria: guarda o "antes/depois" sem criar uma tabela por entidade, e ainda permite consulta.
- Índices parciais (por exemplo, o token do fornecedor só quando existe) e `CHECK` de status no próprio banco.
- Testcontainers sobe o mesmo Postgres 16 nos testes de integração, então o teste roda contra o banco real, não contra H2.

## Decisões de arquitetura

- **Clean Architecture:** `domain` (regra pura), `application` (casos de uso e portas), `infrastructure` (JPA, SMTP, push, ERP, segurança), `web` (REST). O domínio não importa Spring. A transação entra por uma porta (`Transacao`).
- **Alerta em vez de bloqueio:** a `CalculadoraOcupacao` devolve a lista de alertas. O caso de uso salva o agendamento e repassa os alertas para a tela, o e-mail e o push.
- **Não paletizado em dobro** é o fator do tipo de paletização (1 ou 2), editável. O agendamento grava os "paletes equivalentes", então mudar o fator depois não altera o histórico.
- **API versionada** em `/api/v1`, com Swagger e exemplos nos DTOs.
- **E-mail e push assíncronos** (`@Async`): uma falha no SMTP não derruba o agendamento.
- **Link público do fornecedor:** token aleatório de 32 bytes, guardado só o hash SHA-256. Gerar um novo invalida o anterior, e o link para de funcionar depois da chegada.
- **ERP por porta e adaptador:** `PedidoCompraGateway` tem um adaptador local (padrão) e um Bluesoft (OAuth `client_credentials`, desligado). A API real não foi chamada porque não há credenciais.
- **PWA:** `vite-plugin-pwa` com service worker próprio (cache da casca, rota offline, push). As chaves VAPID vêm do ambiente ou são geradas e salvas no banco na primeira subida.

## Segurança

- JWT HS256 (segredo por variável), sessão stateless, senhas com BCrypt.
- Perfil validado no backend (`@PreAuthorize` + regras por URL). USUARIO recebe 403 em qualquer escrita.
- Login auditado com sucesso e falha. Na falha o hash fictício é comparado mesmo assim, para não revelar se o usuário existe pelo tempo de resposta.
- Bean Validation em todas as entradas. Erros voltam padronizados, sem stack trace.
- CORS restrito, cabeçalhos de segurança (CSP, X-Frame-Options, nosniff) na API e no nginx.
- CSV protegido contra injeção de fórmula. Assinatura do vale aceita só PNG em data URL, até 300 KB.
- Containers sem root e imagens mínimas (JRE Alpine, nginx-unprivileged).
- Análise de dependências: [SEGURANCA.md](SEGURANCA.md).

## Telas

Em [docs/prints](prints), versões desktop e celular: login, início/dashboard, novo agendamento (já preenchido pelo ERP), lista, detalhe com portaria, ocupação da semana, relatório, vale-palete (lista e impressão), cadastros, auditoria, página pública do fornecedor, Swagger e e-mail no Mailpit.
