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
    private LocalDate data;
    private Long faixaHorariaId;
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
    private String placa;
    private String motorista;
    private OffsetDateTime chegadaEm;
    private String conferente;
    private OffsetDateTime inicioRecebimentoEm;
    private OffsetDateTime fimRecebimentoEm;
    private Integer paletesRecebidos;
    private String divergencia;
    private String tokenFornecedorHash;
    private Long cdId;
    private Long fornecedorId;

    public static final long CD_PADRAO = 1L;
    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

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
        this.placa = b.placa;
        this.motorista = b.motorista;
        this.chegadaEm = b.chegadaEm;
        this.conferente = b.conferente;
        this.inicioRecebimentoEm = b.inicioRecebimentoEm;
        this.fimRecebimentoEm = b.fimRecebimentoEm;
        this.paletesRecebidos = b.paletesRecebidos;
        this.divergencia = b.divergencia;
        this.tokenFornecedorHash = b.tokenFornecedorHash;
        this.cdId = b.cdId == null ? CD_PADRAO : b.cdId;
        this.fornecedorId = b.fornecedorId;
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
        exigirAntesDaChegada("cancelar");
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Motivo do cancelamento é obrigatório");
        }
        this.status = StatusAgendamento.CANCELADO;
        this.canceladoPor = usuario;
        this.canceladoEm = agora;
        this.motivoCancelamento = motivo.trim();
    }

    /** Fornecedor (pelo link) ou CD confirma que a entrega vai acontecer. */
    public void confirmar() {
        exigirStatus(StatusAgendamento.AGENDADO, "confirmar");
        this.status = StatusAgendamento.CONFIRMADO;
    }

    /** Portaria: caminhão chegou ao pátio. */
    public void registrarChegada(String placa, String motorista, OffsetDateTime agora) {
        exigirAntesDaChegada("registrar chegada");
        this.placa = normalizarPlaca(placa, true);
        this.motorista = textoOpcional(motorista, 80, "Motorista");
        this.chegadaEm = agora;
        this.status = StatusAgendamento.CHEGOU;
    }

    public void iniciarRecebimento(String conferente, OffsetDateTime agora) {
        exigirStatus(StatusAgendamento.CHEGOU, "iniciar o recebimento");
        if (conferente == null || conferente.isBlank()) throw new RegraNegocioException("Informe o conferente");
        this.conferente = textoOpcional(conferente, 80, "Conferente");
        this.inicioRecebimentoEm = agora;
        this.status = StatusAgendamento.EM_RECEBIMENTO;
    }

    /** Conclui a conferência; divergência (falta, sobra, avaria) é registrada sem bloquear. */
    public void concluirRecebimento(Integer paletesRecebidos, String divergencia, OffsetDateTime agora) {
        exigirStatus(StatusAgendamento.EM_RECEBIMENTO, "concluir o recebimento");
        if (paletesRecebidos == null || paletesRecebidos < 0) throw new RegraNegocioException("Informe os paletes recebidos");
        this.paletesRecebidos = paletesRecebidos;
        String div = textoOpcional(divergencia, 255, "Divergência");
        if (div == null && paletesRecebidos != quantidadePaletes) {
            div = "Paletes recebidos (" + paletesRecebidos + ") diferente do agendado (" + quantidadePaletes + ")";
        }
        this.divergencia = div;
        this.fimRecebimentoEm = agora;
        this.status = StatusAgendamento.RECEBIDO;
    }

    public void marcarNaoCompareceu(String usuario, String motivo, OffsetDateTime agora) {
        exigirAntesDaChegada("marcar não comparecimento");
        if (data.isAfter(agora.toLocalDate())) {
            throw new RegraNegocioException("Só é possível marcar não comparecimento no dia ou após a data agendada");
        }
        this.status = StatusAgendamento.NAO_COMPARECEU;
        this.canceladoPor = usuario;
        this.canceladoEm = agora;
        this.motivoCancelamento = motivo == null || motivo.isBlank() ? "Fornecedor não compareceu" : motivo.trim();
    }

    /** Dados que o fornecedor pode ajustar pelo link público antes da chegada. */
    public void atualizarPeloFornecedor(String placa, String motorista) {
        exigirAntesDaChegada("alterar pelo link do fornecedor");
        this.placa = normalizarPlaca(placa, false);
        this.motorista = textoOpcional(motorista, 80, "Motorista");
    }

    public void definirTokenFornecedor(String hash) {
        this.tokenFornecedorHash = hash;
    }

    public boolean divergente() {
        return divergencia != null;
    }

    public static String normalizarPlaca(String raw, boolean obrigatoria) {
        String p = raw == null ? "" : raw.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (p.isEmpty()) {
            if (obrigatoria) throw new RegraNegocioException("Informe a placa do veículo");
            return null;
        }
        if (!PLACA.matcher(p).matches()) throw new RegraNegocioException("Placa inválida: " + raw);
        return p;
    }

    private static String textoOpcional(String v, int max, String campo) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        if (t.length() > max) throw new RegraNegocioException(campo + " deve ter no máximo " + max + " caracteres");
        return t;
    }

    private void exigirStatus(StatusAgendamento esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException("Não é possível " + acao + " com status " + status.rotulo());
        }
    }

    private void exigirAntesDaChegada(String acao) {
        if (!status.antesDaChegada()) {
            throw new RegraNegocioException("Não é possível " + acao + " com status " + status.rotulo());
        }
    }

    public boolean ativo() {
        return status.ocupaCapacidade();
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
    public String getPlaca() { return placa; }
    public String getMotorista() { return motorista; }
    public OffsetDateTime getChegadaEm() { return chegadaEm; }
    public String getConferente() { return conferente; }
    public OffsetDateTime getInicioRecebimentoEm() { return inicioRecebimentoEm; }
    public OffsetDateTime getFimRecebimentoEm() { return fimRecebimentoEm; }
    public Integer getPaletesRecebidos() { return paletesRecebidos; }
    public String getDivergencia() { return divergencia; }
    public String getTokenFornecedorHash() { return tokenFornecedorHash; }
    public Long getCdId() { return cdId; }
    public Long getFornecedorId() { return fornecedorId; }

    /** Define o CD e o fornecedor cadastrado (opcional) do agendamento. */
    public void vincular(Long cd, Long fornecedor) {
        if (cd != null) this.cdId = cd;
        this.fornecedorId = fornecedor;
    }

    /**
     * Remarcação de data/faixa (pelo CD ou pelo fornecedor no link público). Só antes da chegada,
     * nunca para o passado; a confirmação anterior é desfeita.
     */
    public void reagendar(LocalDate novaData, FaixaHoraria faixa, OffsetDateTime agora) {
        exigirAntesDaChegada("remarcar");
        if (novaData == null) throw new RegraNegocioException("Data é obrigatória");
        if (faixa == null || !faixa.ativo()) throw new RegraNegocioException("Faixa horária inválida ou inativa");
        if (novaData.isBefore(agora.toLocalDate())) {
            throw new RegraNegocioException("Data do agendamento não pode ser anterior a hoje");
        }
        if (novaData.equals(data) && faixa.id().equals(faixaHorariaId)) {
            throw new RegraNegocioException("Escolha uma data ou faixa diferente da atual");
        }
        this.data = novaData;
        this.faixaHorariaId = faixa.id();
        this.status = StatusAgendamento.AGENDADO;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long cdId;
        private Long fornecedorId;
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
        private String placa;
        private String motorista;
        private OffsetDateTime chegadaEm;
        private String conferente;
        private OffsetDateTime inicioRecebimentoEm;
        private OffsetDateTime fimRecebimentoEm;
        private Integer paletesRecebidos;
        private String divergencia;
        private String tokenFornecedorHash;

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
        public Builder placa(String v) { placa = v; return this; }
        public Builder motorista(String v) { motorista = v; return this; }
        public Builder chegadaEm(OffsetDateTime v) { chegadaEm = v; return this; }
        public Builder conferente(String v) { conferente = v; return this; }
        public Builder inicioRecebimentoEm(OffsetDateTime v) { inicioRecebimentoEm = v; return this; }
        public Builder fimRecebimentoEm(OffsetDateTime v) { fimRecebimentoEm = v; return this; }
        public Builder paletesRecebidos(Integer v) { paletesRecebidos = v; return this; }
        public Builder divergencia(String v) { divergencia = v; return this; }
        public Builder tokenFornecedorHash(String v) { tokenFornecedorHash = v; return this; }
        public Builder cdId(Long v) { cdId = v; return this; }
        public Builder fornecedorId(Long v) { fornecedorId = v; return this; }
        public Agendamento build() { return new Agendamento(this); }
    }
}
