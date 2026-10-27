package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "parametro_capacidade")
public class ParametroCapacidadeEntity {
    @Id
    public Short id = 1;
    @Column(name = "capacidade_diaria")
    public int capacidadeDiaria;
    @Column(name = "quantidade_faixas")
    public int quantidadeFaixas;
    @Column(name = "duracao_faixa_minutos")
    public int duracaoFaixaMinutos;
    @Column(name = "atualizado_em")
    public OffsetDateTime atualizadoEm;
}
