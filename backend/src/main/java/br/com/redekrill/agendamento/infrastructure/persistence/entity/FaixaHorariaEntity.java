package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "faixa_horaria")
public class FaixaHorariaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "hora_inicio")
    public LocalTime horaInicio;
    @Column(name = "hora_fim")
    public LocalTime horaFim;
    @Column(name = "capacidade_paletes")
    public int capacidadePaletes;
    public boolean ativo;
}
