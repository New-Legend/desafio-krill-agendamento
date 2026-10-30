-- Gestão de acessos no padrão do Gestão CD: perfis com permissões numeradas (1001+),
-- usuário ligado a um perfil, troca de senha obrigatória e log de acessos.

CREATE TABLE perfil_acesso (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(40)  NOT NULL,
    descricao  VARCHAR(160),
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uk_perfil_acesso_nome ON perfil_acesso (lower(nome));

CREATE TABLE perfil_permissao (
    perfil_id  BIGINT  NOT NULL REFERENCES perfil_acesso (id) ON DELETE CASCADE,
    codigo     INTEGER NOT NULL CHECK (codigo BETWEEN 1001 AND 9999),
    PRIMARY KEY (perfil_id, codigo)
);

INSERT INTO perfil_acesso (nome, descricao) VALUES
    ('Administrador', 'Todas as telas, inclusive usuários, perfis e logs de acesso'),
    ('Recebimento', 'Agenda, portaria, conferência, docas, notas fiscais e paletes'),
    ('Portaria', 'Chegada de caminhões, pátio e docas, vale-palete na saída'),
    ('Consulta', 'Leitura das telas de operação, recebimento e paletes');

INSERT INTO perfil_permissao (perfil_id, codigo)
SELECT p.id, c.codigo FROM perfil_acesso p
CROSS JOIN (VALUES (1101), (1102), (1201), (1204), (1205), (1206), (1301), (1302), (1303)) AS c(codigo)
WHERE p.nome = 'Portaria';

-- Administrador recebe todos os números do catálogo (Permissao.java)
INSERT INTO perfil_permissao (perfil_id, codigo)
SELECT p.id, c.codigo FROM perfil_acesso p
CROSS JOIN (SELECT generate_series(1001, 1005) AS codigo UNION ALL SELECT generate_series(1101, 1106)
            UNION ALL SELECT generate_series(1201, 1207) UNION ALL SELECT generate_series(1301, 1308)
            UNION ALL SELECT generate_series(1401, 1411)) c
WHERE p.nome = 'Administrador';

INSERT INTO perfil_permissao (perfil_id, codigo)
SELECT p.id, c.codigo FROM perfil_acesso p
CROSS JOIN (SELECT generate_series(1101, 1106) AS codigo UNION ALL SELECT generate_series(1201, 1207)
            UNION ALL SELECT generate_series(1301, 1308)) c
WHERE p.nome = 'Recebimento';

INSERT INTO perfil_permissao (perfil_id, codigo)
SELECT p.id, c.codigo FROM perfil_acesso p
CROSS JOIN (VALUES (1101), (1102), (1106), (1201), (1202), (1203), (1205), (1301), (1304), (1307)) AS c(codigo)
WHERE p.nome = 'Consulta';

ALTER TABLE usuario ADD COLUMN perfil_id BIGINT REFERENCES perfil_acesso (id);
ALTER TABLE usuario ADD COLUMN trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN ultimo_acesso_em TIMESTAMPTZ;
ALTER TABLE usuario ADD COLUMN criado_em TIMESTAMPTZ NOT NULL DEFAULT now();
UPDATE usuario SET perfil_id = (SELECT id FROM perfil_acesso WHERE nome = 'Administrador') WHERE perfil = 'ADMIN';
UPDATE usuario SET perfil_id = (SELECT id FROM perfil_acesso WHERE nome = 'Consulta') WHERE perfil <> 'ADMIN';
ALTER TABLE usuario ALTER COLUMN perfil_id SET NOT NULL;
ALTER TABLE usuario DROP COLUMN perfil;

CREATE TABLE acesso_log (
    id          BIGSERIAL PRIMARY KEY,
    usuario     VARCHAR(60)  NOT NULL,
    usuario_id  BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    evento      VARCHAR(20)  NOT NULL CHECK (evento IN ('ENTRADA', 'SAIDA', 'SENHA_RECUSADA', 'SENHA_TROCADA')),
    ip          VARCHAR(45),
    navegador   VARCHAR(200),
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_acesso_log_criado ON acesso_log (criado_em DESC);
CREATE INDEX ix_acesso_log_usuario ON acesso_log (lower(usuario));
