-- Porte de /vale-palete/configuracoes, /paletes/email, /recebimento-whatsapp e /recebimento-conferentes do Gestão CD.

-- Tipos de palete configuráveis (substitui as quatro colunas fixas).
CREATE TABLE tipo_palete (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20)    NOT NULL UNIQUE CHECK (codigo ~ '^[A-Z0-9_]{2,20}$'),
    nome            VARCHAR(40)    NOT NULL,
    valor_reposicao NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (valor_reposicao >= 0),
    ordem           INTEGER        NOT NULL DEFAULT 0,
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE
);
INSERT INTO tipo_palete (codigo, nome, valor_reposicao, ordem) VALUES
    ('PBR', 'PBR', 45.00, 1),
    ('CHEP', 'CHEP', 0.00, 2),
    ('CHAPATEX', 'Chapatex', 25.00, 3),
    ('DESCARTAVEL', 'Descartável', 0.00, 4);

-- Quantidades por tipo passam a ser um mapa {CODIGO: quantidade}.
ALTER TABLE vale_palete ADD COLUMN entregues  JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE vale_palete ADD COLUMN devolvidos JSONB NOT NULL DEFAULT '{}'::jsonb;
UPDATE vale_palete SET
    entregues  = jsonb_strip_nulls(jsonb_build_object(
        'PBR', NULLIF(pbr, 0), 'CHEP', NULLIF(chep, 0), 'CHAPATEX', NULLIF(chapatex, 0), 'DESCARTAVEL', NULLIF(descartaveis, 0))),
    devolvidos = jsonb_strip_nulls(jsonb_build_object(
        'PBR', NULLIF(devolvidos_pbr, 0), 'CHEP', NULLIF(devolvidos_chep, 0),
        'CHAPATEX', NULLIF(devolvidos_chapatex, 0), 'DESCARTAVEL', NULLIF(devolvidos_descartaveis, 0)));
ALTER TABLE vale_palete DROP COLUMN pbr, DROP COLUMN chep, DROP COLUMN chapatex, DROP COLUMN descartaveis,
    DROP COLUMN devolvidos_pbr, DROP COLUMN devolvidos_chep, DROP COLUMN devolvidos_chapatex, DROP COLUMN devolvidos_descartaveis;

ALTER TABLE movimento_palete ADD COLUMN quantidades JSONB NOT NULL DEFAULT '{}'::jsonb;
UPDATE movimento_palete SET quantidades = jsonb_strip_nulls(jsonb_build_object(
    'PBR', NULLIF(pbr, 0), 'CHEP', NULLIF(chep, 0), 'CHAPATEX', NULLIF(chapatex, 0), 'DESCARTAVEL', NULLIF(descartaveis, 0)));
ALTER TABLE movimento_palete DROP COLUMN pbr, DROP COLUMN chep, DROP COLUMN chapatex, DROP COLUMN descartaveis;

-- Registro de envios (e-mail de cobrança, WhatsApp) — porte de palete_email_envios.
CREATE TABLE notificacao_envio (
    id              BIGSERIAL PRIMARY KEY,
    canal           VARCHAR(10)  NOT NULL CHECK (canal IN ('EMAIL', 'WHATSAPP')),
    documento_tipo  VARCHAR(20)  NOT NULL,
    documento_id    BIGINT       NOT NULL,
    destinatario    VARCHAR(120) NOT NULL,
    assunto         VARCHAR(200) NOT NULL,
    provedor        VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('ENVIADO', 'FALHOU', 'NAO_CONFIGURADO', 'LINK_GERADO')),
    erro            VARCHAR(255),
    usuario         VARCHAR(80)  NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_notificacao_documento ON notificacao_envio (documento_tipo, documento_id, criado_em DESC);

-- Conferentes por CD.
CREATE TABLE conferente (
    id     BIGSERIAL PRIMARY KEY,
    cd_id  BIGINT      NOT NULL REFERENCES centro_distribuicao (id),
    nome   VARCHAR(80) NOT NULL,
    ativo  BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_conferente_cd_nome ON conferente (cd_id, lower(nome));
