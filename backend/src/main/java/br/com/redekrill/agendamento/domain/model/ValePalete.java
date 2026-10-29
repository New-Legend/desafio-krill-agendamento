package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Vale-palete (porte do Gestão CD): registra paletes trocados com fornecedor/transportadora
 * por tipo configurável (PBR, CHEP, Chapatex, descartável...), quanto foi devolvido e o saldo devido.
 * Fica PENDENTE até ter as duas assinaturas (motorista e conferente).
 */
public class ValePalete {

    public enum Status { PENDENTE, ASSINADO, QUITADO, CANCELADO }

    public enum Assinante { MOTORISTA, CONFERENTE }

    /** Quantidades por tipo de palete. */
    /**
     * Quantidade por tipo de palete ({CODIGO: quantidade}). Os tipos vêm do cadastro configurável
     * (PBR, CHEP, Chapatex, descartável e outros que o CD criar). Zeros são descartados.
     */
    public record Quantidades(java.util.SortedMap<String, Integer> itens) {
        public static final Quantidades ZERO = new Quantidades(java.util.Map.of());

        public Quantidades(java.util.Map<String, Integer> itens) {
            this(normalizar(itens));
        }

        public Quantidades {
            itens = java.util.Collections.unmodifiableSortedMap(normalizar(itens));
        }

        private static java.util.SortedMap<String, Integer> normalizar(java.util.Map<String, Integer> m) {
            java.util.TreeMap<String, Integer> r = new java.util.TreeMap<>();
            if (m == null) return r;
            m.forEach((k, v) -> {
                if (k == null || k.isBlank()) throw new RegraNegocioException("Tipo de palete é obrigatório");
                int q = v == null ? 0 : v;
                if (q < 0) throw new RegraNegocioException("Quantidade de paletes não pode ser negativa");
                if (q > 0) r.merge(k.trim().toUpperCase(java.util.Locale.ROOT), q, Integer::sum);
            });
            return r;
        }

        public static Quantidades de(String codigo, int quantidade) {
            return new Quantidades(java.util.Map.of(codigo, quantidade));
        }

        public int de(String codigo) {
            return itens.getOrDefault(codigo, 0);
        }

        public java.util.Set<String> tipos() {
            return itens.keySet();
        }

        public int total() {
            return itens.values().stream().mapToInt(Integer::intValue).sum();
        }

        public Quantidades mais(Quantidades o) {
            java.util.Map<String, Integer> r = new java.util.TreeMap<>(itens);
            o.itens.forEach((k, v) -> r.merge(k, v, Integer::sum));
            return new Quantidades(r);
        }

        public Quantidades menos(Quantidades o) {
            java.util.Map<String, Integer> r = new java.util.TreeMap<>();
            itens.forEach((k, v) -> r.put(k, Math.max(0, v - o.de(k))));
            return new Quantidades(r);
        }

        boolean excede(Quantidades limite) {
            return itens.entrySet().stream().anyMatch(e -> e.getValue() > limite.de(e.getKey()));
        }
    }

    public record Assinatura(String nome, String imagem, OffsetDateTime em) {
    }

    private final Long id;
    private final String codigo;
    private final Long agendamentoId;
    private final LocalDate data;
    private final String fornecedor;
    private final String placa;
    private final String motorista;
    private final Quantidades entregues;
    private Quantidades devolvidos;
    private Status status;
    private final String observacao;
    private Assinatura assinaturaMotorista;
    private Assinatura assinaturaConferente;
    private final String criadoPor;
    private final OffsetDateTime criadoEm;

    public ValePalete(Long id, String codigo, Long agendamentoId, LocalDate data, String fornecedor, String placa,
                      String motorista, Quantidades entregues, Quantidades devolvidos, Status status, String observacao,
                      Assinatura assinaturaMotorista, Assinatura assinaturaConferente, String criadoPor,
                      OffsetDateTime criadoEm) {
        if (codigo == null || codigo.isBlank()) throw new RegraNegocioException("Código do vale é obrigatório");
        if (data == null) throw new RegraNegocioException("Data do vale é obrigatória");
        if (fornecedor == null || fornecedor.isBlank()) throw new RegraNegocioException("Fornecedor é obrigatório");
        if (fornecedor.trim().length() > 80) throw new RegraNegocioException("Fornecedor deve ter no máximo 80 caracteres");
        if (entregues == null || entregues.total() == 0) throw new RegraNegocioException("Informe ao menos um palete no vale");
        Quantidades dev = devolvidos == null ? Quantidades.ZERO : devolvidos;
        if (dev.excede(entregues)) throw new RegraNegocioException("Devolução maior que a quantidade do vale");
        this.id = id;
        this.codigo = codigo;
        this.agendamentoId = agendamentoId;
        this.data = data;
        this.fornecedor = fornecedor.trim();
        this.placa = Agendamento.normalizarPlaca(placa, false);
        this.motorista = motorista == null || motorista.isBlank() ? null : motorista.trim();
        this.entregues = entregues;
        this.devolvidos = dev;
        this.status = status == null ? Status.PENDENTE : status;
        this.observacao = observacao == null || observacao.isBlank() ? null : observacao.trim();
        this.assinaturaMotorista = assinaturaMotorista;
        this.assinaturaConferente = assinaturaConferente;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
    }

    /** Código no padrão do Gestão CD: VP-AAAAMMDD-NNNN. */
    public static String gerarCodigo(LocalDate data, long sequencia) {
        return "VP-" + data.toString().replace("-", "") + "-" + String.format("%04d", sequencia % 10000);
    }

    public void assinar(Assinante quem, String nome, String imagem, OffsetDateTime agora) {
        exigirAberto();
        if (nome == null || nome.isBlank()) throw new RegraNegocioException("Informe o nome de quem assina");
        if (imagem == null || !imagem.startsWith("data:image/png;base64,")) {
            throw new RegraNegocioException("Assinatura inválida");
        }
        if (imagem.length() > 300_000) throw new RegraNegocioException("Imagem da assinatura muito grande");
        Assinatura a = new Assinatura(nome.trim(), imagem, agora);
        if (quem == Assinante.MOTORISTA) assinaturaMotorista = a;
        else assinaturaConferente = a;
        atualizarStatus();
    }

    /** Refazer: apaga a assinatura para coletar de novo (antes de quitar). */
    public void removerAssinatura(Assinante quem) {
        exigirAberto();
        if (quem == Assinante.MOTORISTA) assinaturaMotorista = null;
        else assinaturaConferente = null;
        atualizarStatus();
    }

    /** Registra devolução (total ou parcial). Quitado quando não resta saldo. */
    public void registrarDevolucao(Quantidades devolucao) {
        exigirAberto();
        Quantidades novo = devolvidos.mais(devolucao);
        if (devolucao.total() == 0) throw new RegraNegocioException("Informe a quantidade devolvida");
        if (novo.excede(entregues)) throw new RegraNegocioException("Devolução maior que o saldo do vale");
        devolvidos = novo;
        atualizarStatus();
    }

    public void cancelar() {
        if (status == Status.QUITADO) throw new RegraNegocioException("Vale quitado não pode ser cancelado");
        status = Status.CANCELADO;
    }

    private void exigirAberto() {
        if (status == Status.QUITADO || status == Status.CANCELADO) {
            throw new RegraNegocioException("Vale " + status.name().toLowerCase() + " não pode ser alterado");
        }
    }

    private void atualizarStatus() {
        if (saldo().total() == 0) status = Status.QUITADO;
        else if (assinaturaMotorista != null && assinaturaConferente != null) status = Status.ASSINADO;
        else status = Status.PENDENTE;
    }

    public Quantidades saldo() {
        return entregues.menos(devolvidos);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Long getAgendamentoId() { return agendamentoId; }
    public LocalDate getData() { return data; }
    public String getFornecedor() { return fornecedor; }
    public String getPlaca() { return placa; }
    public String getMotorista() { return motorista; }
    public Quantidades getEntregues() { return entregues; }
    public Quantidades getDevolvidos() { return devolvidos; }
    public Status getStatus() { return status; }
    public String getObservacao() { return observacao; }
    public Assinatura getAssinaturaMotorista() { return assinaturaMotorista; }
    public Assinatura getAssinaturaConferente() { return assinaturaConferente; }
    public String getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}
