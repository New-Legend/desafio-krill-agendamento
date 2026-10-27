package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Cobrança de paletes não devolvidos (porte de /paletes/cobranca, /pagar e /recibos).
 * Ao pagar, recebe número de recibo e compensa o saldo do fornecedor.
 */
public class CobrancaPalete {

    public enum Status { ABERTA, PAGA, CANCELADA }

    public enum FormaPagamento { PIX, BOLETO, DINHEIRO, DESCONTO_NF, DEVOLUCAO_FISICA }

    private final Long id;
    private final String fornecedor;
    private final int quantidade;
    private final BigDecimal valorUnitario;
    private final LocalDate vencimento;
    private final String observacao;
    private final String criadoPor;
    private final OffsetDateTime criadoEm;
    private Status status;
    private String reciboNumero;
    private FormaPagamento formaPagamento;
    private OffsetDateTime pagoEm;
    private String recebidoPor;
    private String motivoCancelamento;

    public CobrancaPalete(Long id, String fornecedor, int quantidade, BigDecimal valorUnitario, LocalDate vencimento,
                          String observacao, String criadoPor, OffsetDateTime criadoEm, Status status,
                          String reciboNumero, FormaPagamento formaPagamento, OffsetDateTime pagoEm,
                          String recebidoPor, String motivoCancelamento) {
        this.id = id;
        this.fornecedor = Textos.obrigatorio(fornecedor, "Fornecedor", 80);
        if (quantidade < 1) throw new RegraNegocioException("Quantidade deve ser maior que zero");
        if (valorUnitario == null || valorUnitario.signum() < 0) throw new RegraNegocioException("Valor unitário inválido");
        if (vencimento == null) throw new RegraNegocioException("Vencimento é obrigatório");
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario.setScale(2, RoundingMode.HALF_UP);
        this.vencimento = vencimento;
        this.observacao = Textos.opcional(observacao, "Observação", 255);
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.status = status == null ? Status.ABERTA : status;
        this.reciboNumero = reciboNumero;
        this.formaPagamento = formaPagamento;
        this.pagoEm = pagoEm;
        this.recebidoPor = recebidoPor;
        this.motivoCancelamento = motivoCancelamento;
    }

    public static CobrancaPalete nova(String fornecedor, int quantidade, BigDecimal valorUnitario, LocalDate vencimento,
                                      String observacao, int saldoDisponivel, String usuario, OffsetDateTime agora) {
        if (quantidade > saldoDisponivel) {
            throw new RegraNegocioException("Quantidade maior que o saldo em aberto do fornecedor (" + saldoDisponivel + ")");
        }
        if (vencimento != null && vencimento.isBefore(agora.toLocalDate())) {
            throw new RegraNegocioException("Vencimento não pode ser anterior a hoje");
        }
        return new CobrancaPalete(null, fornecedor, quantidade, valorUnitario, vencimento, observacao, usuario, agora,
                Status.ABERTA, null, null, null, null, null);
    }

    public void pagar(FormaPagamento forma, String numeroRecibo, String usuario, OffsetDateTime agora) {
        if (status != Status.ABERTA) throw new RegraNegocioException("Só cobranças em aberto podem ser pagas");
        if (forma == null) throw new RegraNegocioException("Forma de pagamento é obrigatória");
        this.status = Status.PAGA;
        this.formaPagamento = forma;
        this.reciboNumero = numeroRecibo;
        this.recebidoPor = usuario;
        this.pagoEm = agora;
    }

    public void cancelar(String motivo) {
        if (status != Status.ABERTA) throw new RegraNegocioException("Só cobranças em aberto podem ser canceladas");
        this.motivoCancelamento = Textos.obrigatorio(motivo, "Motivo do cancelamento", 255);
        this.status = Status.CANCELADA;
    }

    public BigDecimal valorTotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean vencida(LocalDate hoje) {
        return status == Status.ABERTA && vencimento.isBefore(hoje);
    }

    public boolean ativa() {
        return status != Status.CANCELADA;
    }

    public Long getId() { return id; }
    public String getFornecedor() { return fornecedor; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public LocalDate getVencimento() { return vencimento; }
    public String getObservacao() { return observacao; }
    public String getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public Status getStatus() { return status; }
    public String getReciboNumero() { return reciboNumero; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public OffsetDateTime getPagoEm() { return pagoEm; }
    public String getRecebidoPor() { return recebidoPor; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
}
