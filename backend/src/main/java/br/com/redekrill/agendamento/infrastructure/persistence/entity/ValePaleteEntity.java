package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vale_palete")
public class ValePaleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String codigo;
    @Column(name = "agendamento_id")
    public Long agendamentoId;
    public LocalDate data;
    public String fornecedor;
    public String placa;
    public String motorista;
    public int pbr;
    public int chep;
    public int chapatex;
    public int descartaveis;
    @Column(name = "devolvidos_pbr")
    public int devolvidosPbr;
    @Column(name = "devolvidos_chep")
    public int devolvidosChep;
    @Column(name = "devolvidos_chapatex")
    public int devolvidosChapatex;
    @Column(name = "devolvidos_descartaveis")
    public int devolvidosDescartaveis;
    public String status;
    public String observacao;
    @Column(name = "assinatura_motorista")
    public String assinaturaMotorista;
    @Column(name = "assinatura_motorista_nome")
    public String assinaturaMotoristaNome;
    @Column(name = "assinatura_motorista_em")
    public OffsetDateTime assinaturaMotoristaEm;
    @Column(name = "assinatura_conferente")
    public String assinaturaConferente;
    @Column(name = "assinatura_conferente_nome")
    public String assinaturaConferenteNome;
    @Column(name = "assinatura_conferente_em")
    public OffsetDateTime assinaturaConferenteEm;
    @Column(name = "criado_por")
    public String criadoPor;
    @Column(name = "criado_em")
    public OffsetDateTime criadoEm;
}
