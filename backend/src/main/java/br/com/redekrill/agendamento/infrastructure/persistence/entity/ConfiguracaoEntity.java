package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao")
public class ConfiguracaoEntity {
    @Id
    public String chave;
    public String valor;
}
