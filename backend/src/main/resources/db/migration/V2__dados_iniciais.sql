INSERT INTO parametro_capacidade (id, capacidade_diaria, quantidade_faixas, duracao_faixa_minutos)
VALUES (1, 480, 4, 120);

INSERT INTO faixa_horaria (hora_inicio, hora_fim, capacidade_paletes) VALUES
    ('08:00', '10:00', 120),
    ('10:00', '12:00', 120),
    ('13:00', '15:00', 120),
    ('15:00', '17:00', 120);

INSERT INTO tipo_caminhao (nome, descricao, capacidade_paletes) VALUES
    ('Truck', 'Caminhão truck (3 eixos)', 14),
    ('Carreta', 'Carreta (cavalo + semirreboque)', 28),
    ('Van', 'Van / utilitário', 4);

INSERT INTO tipo_paletizacao (nome, fator_multiplicador) VALUES
    ('Paletizado', 1),
    ('Não paletizado', 2);
