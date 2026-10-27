package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "push_inscricao")
public class PushInscricaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String usuario;
    public String endpoint;
    public String p256dh;
    public String auth;
    @Column(name = "criado_em")
    public OffsetDateTime criadoEm;
}
