# Agendamento de Recebimento — Rede Krill

Sistema web para agendar a entrega de caminhões no recebimento do CD, controlando a capacidade de paletes por dia e por faixa horária.

- **Backend:** Java 17+, Spring Boot 3.5 (Web, Validation, Data JPA, Security com JWT, Flyway), PostgreSQL 16, Swagger (springdoc), e-mail SMTP, Web Push (VAPID).
- **Frontend:** Vue 3 + Vite + PrimeVue, PWA instalável (manifest, service worker, tela offline, notificações push).
- **Infra:** Docker multi-stage (usuário não-root), nginx para o frontend, docker-compose com `db`, `api`, `web` e `mail` (Mailpit).

## Como rodar

Pré-requisito: Docker com o plugin Compose (Docker Desktop no Windows/macOS). Não precisa de Java, Node nem Postgres na máquina.

```bash
git clone https://github.com/New-Legend/desafio-krill-agendamento.git
cd desafio-krill-agendamento
docker compose up --build        # primeira vez leva alguns minutos (compila API e tela)
```

Quando os quatro serviços ficarem `healthy` (`docker compose ps`), abra:

| Serviço | Endereço |
|---|---|
| Aplicação (PWA) | http://localhost:8081 |
| API / Swagger | http://localhost:8080/swagger-ui/index.html (também em http://localhost:8081/swagger-ui/index.html) |
| Caixa de e-mail (Mailpit) | http://localhost:8025 |

Para parar: `docker compose down` (os dados ficam no volume; `docker compose down -v` apaga tudo e recria do zero na próxima subida).

Acesso de outro computador ou do celular na mesma rede: troque `localhost` pelo IP da máquina que roda o compose (ex.: `http://192.168.0.10:8081`). Para acesso pela internet é preciso publicar em um servidor ou usar um túnel (ex.: Cloudflare Tunnel) apontando para a porta 8081.

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
cd frontend && npm test           # Vitest + Vue Test Utils (npm run test:coverage: mínimo 80% de linhas e ramos nas telas críticas)
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

### Recebimento completo (porte do Gestão CD)

- **Remarcação:** o fornecedor remarca pelo link público, só em faixa com vaga (o servidor recusa se estourar). O CD remarca pelo detalhe e pode exceder, com alerta.
- **Multi-CD:** cada centro de distribuição pode ter capacidade própria por dia e por faixa. O CD 1 segue as regras do desafio (480/120) e é o padrão de tudo.
- **Cadastros:** fornecedores (CNPJ validado), produtos com peso, tempos de descarga por tipo de caminhão (previsto, tolerância de espera, custo por palete).
- **NF-e:** importação do XML no detalhe do agendamento (parser sem XXE, até 2 MB), com criação dos produtos, peso calculado × peso da nota e avisos.
- **Relatórios:** divergências (CSV), tempos de espera e descarga (CSV) e **rotina do dia** com as pendências do recebimento.
- **Paletes:** conta-corrente por fornecedor, cobrança com recibo impresso, romaneios de envio/retorno para as lojas e saldo por loja.
- **Tipos de palete configuráveis** (PBR, CHEP, Chapatex, Descartável + os que o CD criar), usados no vale, na devolução e nos romaneios.
- **E-mail da cobrança de paletes** (ou do recibo) pelo SMTP/Mailpit, com histórico de envios.
- **WhatsApp:** botão que abre o `wa.me` com a mensagem pronta (agendamento e cobrança). Envio automático pela API do WhatsApp Business é opcional (`WHATSAPP_PROVEDOR=cloud-api`), desligado por padrão.
- **Conferentes por CD:** a conferência só inicia com um conferente ativo do CD (quando houver cadastro).
- **Pátio e docas:** cadastro de docas por CD e quadro do pátio (previstos, aguardando doca, na doca, descarregando, liberados), atribuição/liberação de doca e indicador de **CD lotado agora** (docas) e **capacidade do dia**.
- **Capacidade diária por CD e por loja:** limite de paletes e/ou caminhões por dia, geral ou por dia da semana. O novo agendamento mostra o % do destino (CD ou loja da entrega direta) e alerta ao passar.
- **Taxa de descarga por modal** (sider/carreta, baú, granel e outros), deduzida do tipo de caminhão quando não informada, exibida em cada caminhão do pátio.
- **Produtos:** exportação do catálogo em CSV e **produto avulso** lançado na conferência (fora da NF-e).
- **Vale-palete:** tipos de palete por CD, opção **exigir as duas assinaturas** antes da devolução e conta-corrente agrupada por **transportadora**.

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
