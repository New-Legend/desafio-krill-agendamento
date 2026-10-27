package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "agendamento")
public class AgendamentoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public LocalDate data;
    @Column(name = "faixa_horaria_id")
    public Long faixaHorariaId;
    @Column(name = "id_pedido")
    public Long idPedido;
    public String fornecedor;
    @Column(name = "email_fornecedor")
    public String emailFornecedor;
    @Column(name = "tipo_caminhao_id")
    public Long tipoCaminhaoId;
    @Column(name = "tipo_paletizacao_id")
    public Long tipoPaletizacaoId;
    @Column(name = "quantidade_paletes")
    public int quantidadePaletes;
    @Column(name = "paletes_equivalentes")
    public int paletesEquivalentes;
    public String observacao;
    @Enumerated(EnumType.STRING)
    public StatusAgendamento status;
    @Column(name = "criado_por")
    public String criadoPor;
    @Column(name = "criado_em")
    public OffsetDateTime criadoEm;
    @Column(name = "cancelado_por")
    public String canceladoPor;
    @Column(name = "cancelado_em")
    public OffsetDateTime canceladoEm;
    @Column(name = "motivo_cancelamento")
    public String motivoCancelamento;
    public String placa;
    public String motorista;
    @Column(name = "chegada_em")
    public OffsetDateTime chegadaEm;
    public String conferente;
    @Column(name = "inicio_recebimento_em")
    public OffsetDateTime inicioRecebimentoEm;
    @Column(name = "fim_recebimento_em")
    public OffsetDateTime fimRecebimentoEm;
    @Column(name = "paletes_recebidos")
    public Integer paletesRecebidos;
    public String divergencia;
    @Column(name = "token_fornecedor_hash")
    public String tokenFornecedorHash;
}
