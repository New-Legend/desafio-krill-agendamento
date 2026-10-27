package br.com.redekrill.agendamento.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/** NF-e importada do XML e vinculada a um agendamento (porte de /recebimento-produtos/importar-nf). */
public record NotaFiscal(Long id, Long agendamentoId, String chave, String numero, String serie, String emitenteCnpj,
                         String emitenteNome, OffsetDateTime emissao, BigDecimal valorTotal, BigDecimal pesoBrutoKg,
                         Integer volumes, List<Item> itens, String importadoPor, OffsetDateTime importadoEm) {

    /** Diferença tolerada entre o peso informado na NF e o calculado pelos produtos. */
    public static final BigDecimal TOLERANCIA_PESO = new BigDecimal("0.05");

    public record Item(int numeroItem, String codigo, String ean, String descricao, String ncm, String unidade,
                       BigDecimal quantidade, BigDecimal valorUnitario, BigDecimal valorTotal, BigDecimal pesoKg,
                       Long produtoId) {
        public Item comPeso(BigDecimal peso, Long produto) {
            return new Item(numeroItem, codigo, ean, descricao, ncm, unidade, quantidade, valorUnitario, valorTotal,
                    peso, produto);
        }
    }

    public NotaFiscal comVinculo(Long agendamento, List<Item> novosItens, String usuario, OffsetDateTime agora) {
        return new NotaFiscal(id, agendamento, chave, numero, serie, emitenteCnpj, emitenteNome, emissao, valorTotal,
                pesoBrutoKg, volumes, novosItens, usuario, agora);
    }

    /** Soma dos pesos dos itens com produto pesado; nulo se nenhum item tem peso. */
    public BigDecimal pesoCalculadoKg() {
        List<BigDecimal> pesos = itens.stream().map(Item::pesoKg).filter(Objects::nonNull).toList();
        return pesos.isEmpty() ? null : pesos.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(3, RoundingMode.HALF_UP);
    }

    public long itensSemPeso() {
        return itens.stream().filter(i -> i.pesoKg() == null).count();
    }

    /** Peso da NF diverge do calculado em mais de 5%. */
    public boolean pesoDivergente() {
        BigDecimal calc = pesoCalculadoKg();
        if (calc == null || pesoBrutoKg == null || pesoBrutoKg.signum() == 0 || itensSemPeso() > 0) return false;
        BigDecimal dif = calc.subtract(pesoBrutoKg).abs().divide(pesoBrutoKg, 4, RoundingMode.HALF_UP);
        return dif.compareTo(TOLERANCIA_PESO) > 0;
    }
}
