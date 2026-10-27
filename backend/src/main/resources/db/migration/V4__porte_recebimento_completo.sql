-- Porte do recebimento do Gestão CD: multi-CD, fornecedores, produtos/NF-e, descarga,
-- cobrança de paletes, recibos e movimentos entre lojas.

CREATE TABLE centro_distribuicao (
    id                        BIGSERIAL PRIMARY KEY,
    codigo                    VARCHAR(10)  NOT NULL UNIQUE,
    nome                      VARCHAR(80)  NOT NULL,
    cidade                    VARCHAR(80),
    capacidade_diaria_paletes INTEGER CHECK (capacidade_diaria_paletes > 0),
    capacidade_faixa_paletes  INTEGER CHECK (capacidade_faixa_paletes > 0),
    ativo                     BOOLEAN      NOT NULL DEFAULT TRUE
);
INSERT INTO centro_distribuicao (id, codigo, nome, cidade) VALUES (1, '806', 'CD Rede Krill', NULL);
SELECT setval('centro_distribuicao_id_seq', 1);

CREATE TABLE fornecedor (
    id            BIGSERIAL PRIMARY KEY,
    cnpj          VARCHAR(14)  NOT NULL UNIQUE,
    razao_social  VARCHAR(80)  NOT NULL,
    nome_fantasia VARCHAR(80),
    email         VARCHAR(80)  NOT NULL,
    telefone      VARCHAR(20),
    contato       VARCHAR(80),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE
);

ALTER TABLE agendamento
    ADD COLUMN cd_id BIGINT NOT NULL DEFAULT 1 REFERENCES centro_distribuicao (id),
    ADD COLUMN fornecedor_id BIGINT REFERENCES fornecedor (id);
CREATE INDEX ix_agendamento_cd_data ON agendamento (cd_id, data);
CREATE INDEX ix_agendamento_divergencia ON agendamento (data) WHERE divergencia IS NOT NULL;

CREATE TABLE produto (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(60)    NOT NULL UNIQUE,
    ean              VARCHAR(14),
    descricao        VARCHAR(120)   NOT NULL,
    unidade          VARCHAR(6),
    peso_bruto_kg    NUMERIC(12, 4) CHECK (peso_bruto_kg >= 0),
    itens_por_palete INTEGER CHECK (itens_por_palete > 0),
    ativo            BOOLEAN        NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_produto_ean ON produto (ean);

CREATE TABLE nota_fiscal (
    id                BIGSERIAL PRIMARY KEY,
    agendamento_id    BIGINT         NOT NULL REFERENCES agendamento (id),
    chave             VARCHAR(44)    NOT NULL UNIQUE,
    numero            VARCHAR(9)     NOT NULL,
    serie             VARCHAR(3),
    emitente_cnpj     VARCHAR(14),
    emitente_nome     VARCHAR(120),
    emissao           TIMESTAMPTZ,
    valor_total       NUMERIC(14, 2),
    peso_bruto_kg     NUMERIC(14, 3),
    peso_calculado_kg NUMERIC(14, 3),
    volumes           INTEGER,
    importado_por     VARCHAR(80)    NOT NULL,
    importado_em      TIMESTAMPTZ    NOT NULL
);
CREATE INDEX ix_nota_agendamento ON nota_fiscal (agendamento_id);

CREATE TABLE nota_fiscal_item (
    id             BIGSERIAL PRIMARY KEY,
    nota_id        BIGINT         NOT NULL REFERENCES nota_fiscal (id) ON DELETE CASCADE,
    numero_item    INTEGER        NOT NULL,
    codigo         VARCHAR(60)    NOT NULL,
    ean            VARCHAR(14),
    descricao      VARCHAR(120)   NOT NULL,
    ncm            VARCHAR(8),
    unidade        VARCHAR(6),
    quantidade     NUMERIC(15, 4) NOT NULL,
    valor_unitario NUMERIC(15, 4),
    valor_total    NUMERIC(14, 2),
    peso_kg        NUMERIC(14, 3),
    produto_id     BIGINT REFERENCES produto (id)
);

CREATE TABLE parametro_descarga (
    tipo_caminhao_id          BIGINT PRIMARY KEY REFERENCES tipo_caminhao (id),
    minutos_previstos         INTEGER       NOT NULL CHECK (minutos_previstos > 0),
    tolerancia_espera_minutos INTEGER       NOT NULL CHECK (tolerancia_espera_minutos >= 0),
    custo_por_palete          NUMERIC(10, 2) NOT NULL CHECK (custo_por_palete >= 0)
);
INSERT INTO parametro_descarga (tipo_caminhao_id, minutos_previstos, tolerancia_espera_minutos, custo_por_palete)
SELECT id, CASE nome WHEN 'Carreta' THEN 90 WHEN 'Truck' THEN 60 ELSE 20 END, 30, 3.50 FROM tipo_caminhao;

CREATE SEQUENCE recibo_palete_seq;
CREATE TABLE cobranca_palete (
    id               BIGSERIAL PRIMARY KEY,
    fornecedor       VARCHAR(80)    NOT NULL,
    quantidade       INTEGER        NOT NULL CHECK (quantidade > 0),
    valor_unitario   NUMERIC(10, 2) NOT NULL CHECK (valor_unitario >= 0),
    vencimento       DATE           NOT NULL,
    status           VARCHAR(10)    NOT NULL CHECK (status IN ('ABERTA', 'PAGA', 'CANCELADA')),
    observacao       VARCHAR(255),
    criado_por       VARCHAR(80)    NOT NULL,
    criado_em        TIMESTAMPTZ    NOT NULL,
    recibo_numero    VARCHAR(20) UNIQUE,
    forma_pagamento  VARCHAR(20),
    pago_em          TIMESTAMPTZ,
    recebido_por     VARCHAR(80),
    motivo_cancelamento VARCHAR(255)
);
CREATE INDEX ix_cobranca_fornecedor ON cobranca_palete (lower(fornecedor));

CREATE SEQUENCE romaneio_palete_seq;
CREATE TABLE movimento_palete (
    id           BIGSERIAL PRIMARY KEY,
    data         DATE         NOT NULL,
    tipo         VARCHAR(15)  NOT NULL CHECK (tipo IN ('ENVIO', 'RETORNO', 'SALDO_INICIAL')),
    cd_id        BIGINT       NOT NULL REFERENCES centro_distribuicao (id),
    loja         VARCHAR(80)  NOT NULL,
    romaneio     VARCHAR(20)  NOT NULL,
    pbr          INTEGER      NOT NULL CHECK (pbr >= 0),
    chep         INTEGER      NOT NULL CHECK (chep >= 0),
    chapatex     INTEGER      NOT NULL CHECK (chapatex >= 0),
    descartaveis INTEGER      NOT NULL CHECK (descartaveis >= 0),
    observacao   VARCHAR(255),
    criado_por   VARCHAR(80)  NOT NULL,
    criado_em    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_movimento_loja ON movimento_palete (lower(loja), data);
