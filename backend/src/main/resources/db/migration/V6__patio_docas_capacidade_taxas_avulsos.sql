-- Porte de /yms/docas e /yms/quadro (pátio), capacidade diária de recebimento por CD e por loja,
-- taxa de descarga por modal (recebimento-cobranca.ts), produto avulso no recebimento,
-- tipos de palete por CD e configurações do vale-palete.

CREATE TABLE doca (
    id      BIGSERIAL PRIMARY KEY,
    cd_id   BIGINT      NOT NULL REFERENCES centro_distribuicao (id),
    codigo  VARCHAR(10) NOT NULL,
    nome    VARCHAR(40) NOT NULL,
    ativa   BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_doca_cd_codigo ON doca (cd_id, upper(codigo));
INSERT INTO doca (cd_id, codigo, nome) VALUES (1, 'D01', 'Doca 1'), (1, 'D02', 'Doca 2'), (1, 'D03', 'Doca 3'), (1, 'D04', 'Doca 4');

-- Dados logísticos do agendamento: entrega direta na loja, transportadora, doca e modal de descarga.
CREATE TABLE agendamento_logistica (
    agendamento_id  BIGINT PRIMARY KEY REFERENCES agendamento (id),
    loja            VARCHAR(80),
    transportadora  VARCHAR(80),
    doca_id         BIGINT REFERENCES doca (id),
    doca_em         TIMESTAMPTZ,
    modal_descarga  VARCHAR(20)
);
CREATE INDEX ix_logistica_loja ON agendamento_logistica (lower(loja));
CREATE INDEX ix_logistica_doca ON agendamento_logistica (doca_id);

-- Capacidade diária de recebimento: por CD ou por loja, opcionalmente por dia da semana (1 = segunda ... 7 = domingo).
CREATE TABLE capacidade_recebimento (
    id          BIGSERIAL PRIMARY KEY,
    cd_id       BIGINT REFERENCES centro_distribuicao (id),
    loja        VARCHAR(80),
    dia_semana  SMALLINT CHECK (dia_semana BETWEEN 1 AND 7),
    paletes     INTEGER CHECK (paletes > 0),
    caminhoes   INTEGER CHECK (caminhoes > 0),
    CHECK ((cd_id IS NULL) <> (loja IS NULL)),
    CHECK (paletes IS NOT NULL OR caminhoes IS NOT NULL)
);
CREATE UNIQUE INDEX ux_capacidade_destino ON capacidade_recebimento (coalesce(cd_id, 0), lower(coalesce(loja, '')), coalesce(dia_semana, 0));

-- Taxa de descarga por modal (tarifas padrão do Gestão CD: sider/carreta 25, baú 30, granel 60 por palete).
CREATE TABLE taxa_descarga (
    id                BIGSERIAL PRIMARY KEY,
    modal             VARCHAR(20)    NOT NULL UNIQUE CHECK (modal ~ '^[A-Z0-9_]{2,20}$'),
    nome              VARCHAR(40)    NOT NULL,
    valor_por_palete  NUMERIC(10, 2) NOT NULL CHECK (valor_por_palete >= 0),
    ativo             BOOLEAN        NOT NULL DEFAULT TRUE
);
INSERT INTO taxa_descarga (modal, nome, valor_por_palete) VALUES
    ('SIDER', 'Sider / carreta', 25.00),
    ('BAU', 'Baú', 30.00),
    ('GRANEL', 'Granel (palete gerado)', 60.00);

-- Produto avulso lançado na conferência (fora da NF-e).
CREATE TABLE recebimento_item_avulso (
    id              BIGSERIAL PRIMARY KEY,
    agendamento_id  BIGINT         NOT NULL REFERENCES agendamento (id),
    produto_id      BIGINT REFERENCES produto (id),
    codigo          VARCHAR(60)    NOT NULL,
    descricao       VARCHAR(120)   NOT NULL,
    quantidade      NUMERIC(14, 3) NOT NULL CHECK (quantidade > 0),
    unidade         VARCHAR(6),
    observacao      VARCHAR(255),
    criado_por      VARCHAR(60)    NOT NULL,
    criado_em       TIMESTAMPTZ    NOT NULL
);
CREATE INDEX ix_item_avulso_agendamento ON recebimento_item_avulso (agendamento_id);

-- Tipo de palete pode ser de um CD só (nulo = todos os CDs).
ALTER TABLE tipo_palete ADD COLUMN cd_id BIGINT REFERENCES centro_distribuicao (id);

-- Configurações do vale-palete (porte de /vale-palete/configuracoes).
INSERT INTO configuracao (chave, valor) VALUES
    ('vale_palete_exigir_duas_assinaturas', '0'),
    ('paletes_agrupamento', 'fornecedor')
ON CONFLICT (chave) DO NOTHING;
