# Agendamento de Recebimento — Rede Krill

Sistema web para agendar a entrega de caminhões no recebimento do CD, controlando a capacidade de paletes por dia e por faixa horária.

- **Backend:** Java 17+, Spring Boot 3.5 (Web, Validation, Data JPA, Security com JWT, Flyway), PostgreSQL 16, Swagger (springdoc), e-mail SMTP, Web Push (VAPID).
- **Frontend:** Vue 3 + Vite + PrimeVue, PWA instalável (manifest, service worker, tela offline, notificações push).
- **Infra:** Docker multi-stage (usuário não-root), nginx para o frontend, docker-compose com `db`, `api`, `web` e `mail` (Mailpit).

## Como rodar

```bash
docker compose up --build
```

| Serviço | Endereço |
|---|---|
| Aplicação (PWA) | http://localhost:8081 |
| API / Swagger | http://localhost:8080/swagger-ui/index.html (também em http://localhost:8081/swagger-ui/index.html) |
| Caixa de e-mail (Mailpit) | http://localhost:8025 |

### Usuários padrão

| Usuário | Senha | Perfil |
|---|---|---|
| `admin` | `admin123` | ADMIN — acesso total |
| `usuario` | `usuario123` | USUARIO — apenas consulta |

As senhas podem ser trocadas pelas variáveis `ADMIN_PASSWORD` e `USUARIO_PASSWORD` (veja `.env.example`).

### Desenvolvimento local

```bash
# banco e e-mail
docker compose up -d db mailpit
# API (porta 8080)
cd backend && DB_URL=jdbc:postgresql://localhost:5432/agendamento mvn spring-boot:run
# frontend (porta 5173, proxy /api -> 8080)
cd frontend && npm ci && npm run dev
```

### Testes

```bash
cd backend && mvn verify          # JUnit 5 + Mockito + Testcontainers + JaCoCo (mínimo 80% em domain/application)
cd frontend && npm test           # Vitest
```

Relatório de cobertura: `backend/target/site/jacoco/index.html`.

## Regras de negócio

- Capacidade padrão de **480 paletes/dia**, em **4 faixas de 2 horas** com **120 paletes** cada (tudo editável nos cadastros).
- **Não paletizado conta em dobro** (fator multiplicador do tipo de paletização).
- Ultrapassar a capacidade **não bloqueia**: o sistema mostra alerta na tela, no e-mail interno e via push.
- Ao criar um agendamento, o fornecedor recebe e-mail com os dados e um **link público** para confirmar presença e informar placa/motorista.
- Fluxo da portaria: Agendado → Confirmado → Chegou → Em recebimento → Recebido (com divergência automática se a contagem diferir), ou Cancelado / Não compareceu.
- **Vale-palete** com assinatura do motorista e do conferente na tela (dedo ou mouse), impressão A4 e controle de devolução.
- Auditoria (somente ADMIN) de toda ação, incluindo login: quem, quando, ação, entidade e IP.

## Arquitetura

Clean Architecture, com dependências apontando para dentro:

```
backend/src/main/java/br/com/redekrill/agendamento
├── domain          regras puras (Agendamento, ValePalete, CalculadoraOcupacao, ValidadorFaixas) — sem Spring
├── application     casos de uso + portas (interfaces de repositório, e-mail, push, ERP, transação)
├── infrastructure  adaptadores: JPA, segurança/JWT, SMTP, Web Push, ERP (Bluesoft e local), configuração
└── web             controllers REST /api/v1, DTOs e tratamento de erros
```

- A API é versionada em **`/api/v1`** e documentada no Swagger com exemplos.
- A regra de capacidade fica no domínio e é testada sem banco; os casos de uso usam portas, então trocar o banco, o e-mail ou o ERP não mexe na regra.
- Criação de agendamento usa `pg_advisory_xact_lock` por dia para dois usuários não estourarem a mesma faixa sem ver o alerta.

## Integração com o ERP (Bluesoft)

O formulário preenche fornecedor, e-mail, paletes e paletização a partir do **ID do pedido**. A consulta passa pela porta `PedidoCompraGateway`, que tem dois adaptadores:

- `local` (padrão): base de demonstração com os pedidos 450001, 450002 e 450003.
- `bluesoft`: OAuth `client_credentials` em `erp.bluesoft.com.br/<tenant>/oauth2/token` + `GET /api/compras/pedidos-de-compra`. Ative com `ERP_PROVEDOR=bluesoft` e as variáveis `BLUESOFT_*`.

Detalhes em [docs/INTEGRACAO-BLUESOFT.md](docs/INTEGRACAO-BLUESOFT.md).

## Documentos

- [docs/ENTREGA.md](docs/ENTREGA.md): resumo da entrega, decisões e segurança
- [docs/PORTE-GESTAO-CD.md](docs/PORTE-GESTAO-CD.md): o que veio do Gestão CD
- [docs/INTEGRACAO-BLUESOFT.md](docs/INTEGRACAO-BLUESOFT.md)
- [docs/SEGURANCA.md](docs/SEGURANCA.md): relatório OWASP Dependency-Check / npm audit
- [docs/ROTEIRO-VIDEO-E-DEFESA.md](docs/ROTEIRO-VIDEO-E-DEFESA.md)
- [docs/prints/](docs/prints): telas desktop e celular
