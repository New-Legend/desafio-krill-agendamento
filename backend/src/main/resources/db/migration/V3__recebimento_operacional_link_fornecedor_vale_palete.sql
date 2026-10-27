-- Porte do Gestão CD: fluxo operacional (portaria/conferência), link do fornecedor e vale-palete.
ALTER TABLE agendamento DROP CONSTRAINT agendamento_status_check;
ALTER TABLE agendamento ADD CONSTRAINT agendamento_status_check CHECK (status IN
    ('AGENDADO', 'CONFIRMADO', 'CHEGOU', 'EM_RECEBIMENTO', 'RECEBIDO', 'CANCELADO', 'NAO_COMPARECEU'));

ALTER TABLE agendamento
    ADD COLUMN placa                  VARCHAR(7),
    ADD COLUMN motorista              VARCHAR(80),
    ADD COLUMN chegada_em             TIMESTAMPTZ,
    ADD COLUMN conferente             VARCHAR(80),
    ADD COLUMN inicio_recebimento_em  TIMESTAMPTZ,
    ADD COLUMN fim_recebimento_em     TIMESTAMPTZ,
    ADD COLUMN paletes_recebidos      INTEGER CHECK (paletes_recebidos >= 0),
    ADD COLUMN divergencia            VARCHAR(255),
    ADD COLUMN token_fornecedor_hash  VARCHAR(64);
CREATE UNIQUE INDEX uk_agendamento_token ON agendamento (token_fornecedor_hash) WHERE token_fornecedor_hash IS NOT NULL;

CREATE SEQUENCE vale_palete_seq;
CREATE TABLE vale_palete (
    id                       BIGSERIAL PRIMARY KEY,
    codigo                   VARCHAR(20)  NOT NULL UNIQUE,
    agendamento_id           BIGINT REFERENCES agendamento (id) ON DELETE SET NULL,
    data                     DATE         NOT NULL,
    fornecedor               VARCHAR(80)  NOT NULL,
    placa                    VARCHAR(7),
    motorista                VARCHAR(80),
    pbr                      INTEGER NOT NULL DEFAULT 0 CHECK (pbr >= 0),
    chep                     INTEGER NOT NULL DEFAULT 0 CHECK (chep >= 0),
    chapatex                 INTEGER NOT NULL DEFAULT 0 CHECK (chapatex >= 0),
    descartaveis             INTEGER NOT NULL DEFAULT 0 CHECK (descartaveis >= 0),
    devolvidos_pbr           INTEGER NOT NULL DEFAULT 0,
    devolvidos_chep          INTEGER NOT NULL DEFAULT 0,
    devolvidos_chapatex      INTEGER NOT NULL DEFAULT 0,
    devolvidos_descartaveis  INTEGER NOT NULL DEFAULT 0,
    status                   VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'ASSINADO', 'QUITADO', 'CANCELADO')),
    observacao               VARCHAR(500),
    assinatura_motorista      TEXT,
    assinatura_motorista_nome VARCHAR(80),
    assinatura_motorista_em   TIMESTAMPTZ,
    assinatura_conferente      TEXT,
    assinatura_conferente_nome VARCHAR(80),
    assinatura_conferente_em   TIMESTAMPTZ,
    criado_por               VARCHAR(60) NOT NULL,
    criado_em                TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_vale_palete_data ON vale_palete (data);
CREATE INDEX ix_vale_palete_fornecedor ON vale_palete (lower(fornecedor), status);
