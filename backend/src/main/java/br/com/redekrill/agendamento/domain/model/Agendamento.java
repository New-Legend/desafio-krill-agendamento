package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/** Agendamento de entrega de caminhão para o recebimento. */
public class Agendamento {

    public static final int TAMANHO_MAXIMO_TEXTO = 80;
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final Long id;
    private final LocalDate data;
    private final Long faixaHorariaId;
    private final Long idPedido;
    private final String fornecedor;
    private final String emailFornecedor;
    private final Long tipoCaminhaoId;
    private final Long tipoPaletizacaoId;
    private final int quantidadePaletes;
    private final int paletesEquivalentes;
    private final String observacao;
    private StatusAgendamento status;
    private final String criadoPor;
    private final OffsetDateTime criadoEm;
    private String canceladoPor;
    private OffsetDateTime canceladoEm;
    private String motivoCancelamento;

    private Agendamento(Builder b) {
        this.id = b.id;
        this.data = b.data;
        this.faixaHorariaId = b.faixaHorariaId;
        this.idPedido = b.idPedido;
        this.fornecedor = b.fornecedor == null ? null : b.fornecedor.trim();
        this.emailFornecedor = b.emailFornecedor == null ? null : b.emailFornecedor.trim().toLowerCase();
        this.tipoCaminhaoId = b.tipoCaminhaoId;
        this.tipoPaletizacaoId = b.tipoPaletizacaoId;
        this.quantidadePaletes = b.quantidadePaletes;
        this.paletesEquivalentes = b.paletesEquivalentes;
        this.observacao = b.observacao == null || b.observacao.isBlank() ? null : b.observacao.trim();
        this.status = b.status == null ? StatusAgendamento.AGENDADO : b.status;
        this.criadoPor = b.criadoPor;
        this.criadoEm = b.criadoEm;
        this.canceladoPor = b.canceladoPor;
        this.canceladoEm = b.canceladoEm;
        this.motivoCancelamento = b.motivoCancelamento;
        validar();
    }

    /** Cria um novo agendamento calculando a ocupação conforme o tipo de paletização. */
    public static Agendamento novo(LocalDate data, FaixaHoraria faixa, Long idPedido, String fornecedor,
                                   String emailFornecedor, TipoCaminhao caminhao, TipoPaletizacao paletizacao,
                                   int quantidadePaletes, String observacao, String usuario, OffsetDateTime agora) {
        if (faixa == null || !faixa.ativo()) {
            throw new RegraNegocioException("Faixa horária inválida ou inativa");
        }
        if (caminhao == null || !caminhao.ativo()) {
            throw new RegraNegocioException("Tipo de caminhão inválido ou inativo");
        }
        if (paletizacao == null || !paletizacao.ativo()) {
            throw new RegraNegocioException("Tipo de paletização inválido ou inativo");
        }
        if (data != null && data.isBefore(agora.toLocalDate())) {
            throw new RegraNegocioException("Data do agendamento não pode ser anterior a hoje");
        }
        return builder()
                .data(data).faixaHorariaId(faixa.id()).idPedido(idPedido)
                .fornecedor(fornecedor).emailFornecedor(emailFornecedor)
                .tipoCaminhaoId(caminhao.id()).tipoPaletizacaoId(paletizacao.id())
                .quantidadePaletes(quantidadePaletes)
                .paletesEquivalentes(paletizacao.paletesEquivalentes(quantidadePaletes))
                .observacao(observacao).status(StatusAgendamento.AGENDADO)
                .criadoPor(usuario).criadoEm(agora)
                .build();
    }

    private void validar() {
        if (data == null) throw new RegraNegocioException("Data é obrigatória");
        if (faixaHorariaId == null) throw new RegraNegocioException("Faixa horária é obrigatória");
        if (idPedido == null || idPedido <= 0) throw new RegraNegocioException("ID do pedido deve ser um número positivo");
        exigirTexto(fornecedor, "Fornecedor");
        exigirTexto(emailFornecedor, "E-mail do fornecedor");
        if (!EMAIL.matcher(emailFornecedor).matches()) throw new RegraNegocioException("E-mail do fornecedor inválido");
        if (tipoCaminhaoId == null) throw new RegraNegocioException("Tipo de caminhão é obrigatório");
        if (tipoPaletizacaoId == null) throw new RegraNegocioException("Tipo de paletização é obrigatório");
        if (quantidadePaletes < 1) throw new RegraNegocioException("Quantidade de paletes deve ser maior que zero");
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new RegraNegocioException(campo + " é obrigatório");
        if (valor.length() > TAMANHO_MAXIMO_TEXTO) {
            throw new RegraNegocioException(campo + " deve ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres");
        }
    }

    public void cancelar(String usuario, String motivo, OffsetDateTime agora) {
        if (status == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Agendamento já está cancelado");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Motivo do cancelamento é obrigatório");
        }
        this.status = StatusAgendamento.CANCELADO;
        this.canceladoPor = usuario;
        this.canceladoEm = agora;
        this.motivoCancelamento = motivo.trim();
    }

    public boolean ativo() {
        return status == StatusAgendamento.AGENDADO;
    }

    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public Long getFaixaHorariaId() { return faixaHorariaId; }
    public Long getIdPedido() { return idPedido; }
    public String getFornecedor() { return fornecedor; }
    public String getEmailFornecedor() { return emailFornecedor; }
    public Long getTipoCaminhaoId() { return tipoCaminhaoId; }
    public Long getTipoPaletizacaoId() { return tipoPaletizacaoId; }
    public int getQuantidadePaletes() { return quantidadePaletes; }
    public int getPaletesEquivalentes() { return paletesEquivalentes; }
    public String getObservacao() { return observacao; }
    public StatusAgendamento getStatus() { return status; }
    public String getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public String getCanceladoPor() { return canceladoPor; }
    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private LocalDate data;
        private Long faixaHorariaId;
        private Long idPedido;
        private String fornecedor;
        private String emailFornecedor;
        private Long tipoCaminhaoId;
        private Long tipoPaletizacaoId;
        private int quantidadePaletes;
        private int paletesEquivalentes;
        private String observacao;
        private StatusAgendamento status;
        private String criadoPor;
        private OffsetDateTime criadoEm;
        private String canceladoPor;
        private OffsetDateTime canceladoEm;
        private String motivoCancelamento;

        public Builder id(Long v) { id = v; return this; }
        public Builder data(LocalDate v) { data = v; return this; }
        public Builder faixaHorariaId(Long v) { faixaHorariaId = v; return this; }
        public Builder idPedido(Long v) { idPedido = v; return this; }
        public Builder fornecedor(String v) { fornecedor = v; return this; }
        public Builder emailFornecedor(String v) { emailFornecedor = v; return this; }
        public Builder tipoCaminhaoId(Long v) { tipoCaminhaoId = v; return this; }
        public Builder tipoPaletizacaoId(Long v) { tipoPaletizacaoId = v; return this; }
        public Builder quantidadePaletes(int v) { quantidadePaletes = v; return this; }
        public Builder paletesEquivalentes(int v) { paletesEquivalentes = v; return this; }
        public Builder observacao(String v) { observacao = v; return this; }
        public Builder status(StatusAgendamento v) { status = v; return this; }
        public Builder criadoPor(String v) { criadoPor = v; return this; }
        public Builder criadoEm(OffsetDateTime v) { criadoEm = v; return this; }
        public Builder canceladoPor(String v) { canceladoPor = v; return this; }
        public Builder canceladoEm(OffsetDateTime v) { canceladoEm = v; return this; }
        public Builder motivoCancelamento(String v) { motivoCancelamento = v; return this; }
        public Agendamento build() { return new Agendamento(this); }
    }
}
