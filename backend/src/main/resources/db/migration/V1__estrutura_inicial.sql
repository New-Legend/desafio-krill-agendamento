-- Parâmetros globais de capacidade (linha única)
CREATE TABLE parametro_capacidade (
    id                     SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    capacidade_diaria      INTEGER  NOT NULL CHECK (capacidade_diaria > 0),
    quantidade_faixas      INTEGER  NOT NULL CHECK (quantidade_faixas > 0),
    duracao_faixa_minutos  INTEGER  NOT NULL CHECK (duracao_faixa_minutos >= 15),
    atualizado_em          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE faixa_horaria (
    id                  BIGSERIAL PRIMARY KEY,
    hora_inicio         TIME    NOT NULL,
    hora_fim            TIME    NOT NULL,
    capacidade_paletes  INTEGER NOT NULL CHECK (capacidade_paletes > 0),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_faixa_intervalo CHECK (hora_fim > hora_inicio),
    CONSTRAINT uk_faixa_inicio UNIQUE (hora_inicio, hora_fim)
);

CREATE TABLE tipo_caminhao (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(40)  NOT NULL,
    descricao           VARCHAR(120),
    capacidade_paletes  INTEGER CHECK (capacidade_paletes > 0),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uk_tipo_caminhao_nome ON tipo_caminhao (lower(nome));

CREATE TABLE tipo_paletizacao (
    id                   BIGSERIAL PRIMARY KEY,
    nome                 VARCHAR(40) NOT NULL,
    fator_multiplicador  INTEGER NOT NULL CHECK (fator_multiplicador >= 1),
    ativo                BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uk_tipo_paletizacao_nome ON tipo_paletizacao (lower(nome));

CREATE TABLE agendamento (
    id                     BIGSERIAL PRIMARY KEY,
    data                   DATE        NOT NULL,
    faixa_horaria_id       BIGINT      NOT NULL REFERENCES faixa_horaria (id),
    id_pedido              BIGINT      NOT NULL CHECK (id_pedido > 0),
    fornecedor             VARCHAR(80) NOT NULL,
    email_fornecedor       VARCHAR(80) NOT NULL,
    tipo_caminhao_id       BIGINT      NOT NULL REFERENCES tipo_caminhao (id),
    tipo_paletizacao_id    BIGINT      NOT NULL REFERENCES tipo_paletizacao (id),
    quantidade_paletes     INTEGER     NOT NULL CHECK (quantidade_paletes > 0),
    paletes_equivalentes   INTEGER     NOT NULL CHECK (paletes_equivalentes > 0),
    observacao             TEXT,
    status                 VARCHAR(20) NOT NULL CHECK (status IN ('AGENDADO', 'CANCELADO')),
    criado_por             VARCHAR(60) NOT NULL,
    criado_em              TIMESTAMPTZ NOT NULL,
    cancelado_por          VARCHAR(60),
    cancelado_em           TIMESTAMPTZ,
    motivo_cancelamento    VARCHAR(255)
);
CREATE INDEX ix_agendamento_data_status ON agendamento (data, status);
CREATE INDEX ix_agendamento_pedido ON agendamento (id_pedido);

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(60)  NOT NULL UNIQUE,
    nome        VARCHAR(120) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL CHECK (perfil IN ('ADMIN', 'USUARIO')),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Auditoria / log de acesso. Detalhes em JSONB para cada ação guardar o seu contexto.
CREATE TABLE auditoria (
    id           BIGSERIAL PRIMARY KEY,
    data_hora    TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario      VARCHAR(60) NOT NULL,
    acao         VARCHAR(40) NOT NULL,
    entidade     VARCHAR(40) NOT NULL,
    entidade_id  VARCHAR(40),
    ip           VARCHAR(64),
    sucesso      BOOLEAN     NOT NULL DEFAULT TRUE,
    detalhes     JSONB       NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_auditoria_data_hora ON auditoria (data_hora DESC);
CREATE INDEX ix_auditoria_usuario ON auditoria (usuario);

CREATE TABLE push_inscricao (
    id         BIGSERIAL PRIMARY KEY,
    usuario    VARCHAR(60) NOT NULL,
    endpoint   TEXT        NOT NULL UNIQUE,
    p256dh     VARCHAR(255) NOT NULL,
    auth       VARCHAR(255) NOT NULL,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE configuracao (
    chave  VARCHAR(60) PRIMARY KEY,
    valor  TEXT NOT NULL
);
