package br.com.redekrill.agendamento.domain;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;
import br.com.redekrill.agendamento.domain.model.Agendamento;
import br.com.redekrill.agendamento.domain.model.CentroDistribuicao;
import br.com.redekrill.agendamento.domain.model.CobrancaPalete;
import br.com.redekrill.agendamento.domain.model.FaixaHoraria;
import br.com.redekrill.agendamento.domain.model.Fornecedor;
import br.com.redekrill.agendamento.domain.model.MovimentoPalete;
import br.com.redekrill.agendamento.domain.model.NotaFiscal;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;
import br.com.redekrill.agendamento.domain.model.Produto;
import br.com.redekrill.agendamento.domain.model.StatusAgendamento;
import br.com.redekrill.agendamento.domain.model.Textos;
import br.com.redekrill.agendamento.domain.model.ValePalete;
import br.com.redekrill.agendamento.domain.service.CalculadoraDescarga;
import br.com.redekrill.agendamento.domain.service.LeitorNfeXml;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PorteRecebimentoDominioTest {

    static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 11, 2, 9, 0, 0, 0, ZoneOffset.ofHours(-3));
    static final FaixaHoraria F1 = new FaixaHoraria(1L, LocalTime.of(8, 0), LocalTime.of(10, 0), 120, true);
    static final FaixaHoraria F2 = new FaixaHoraria(2L, LocalTime.of(10, 0), LocalTime.of(12, 0), 120, true);

    static Agendamento.Builder base() {
        return Agendamento.builder().id(9L).data(LocalDate.of(2026, 11, 3)).faixaHorariaId(1L).idPedido(1L)
                .fornecedor("Fornecedor X").emailFornecedor("a@b.com").tipoCaminhaoId(1L).tipoPaletizacaoId(1L)
                .quantidadePaletes(10).paletesEquivalentes(10);
    }

    static byte[] xml() throws Exception {
        try (var in = PorteRecebimentoDominioTest.class.getResourceAsStream("/nfe/nfe-exemplo.xml")) {
            return in.readAllBytes();
        }
    }

    @Test
    void cnpjEFornecedor() {
        assertThat(Fornecedor.cnpjValido("11222333000181")).isTrue();
        assertThat(Fornecedor.cnpjValido("11222333000182")).isFalse();
        assertThat(Fornecedor.cnpjValido("11111111111111")).isFalse();
        assertThat(Fornecedor.cnpjValido("123")).isFalse();
        assertThat(Fornecedor.cnpjValido(null)).isFalse();
        Fornecedor f = new Fornecedor(null, "11.222.333/0001-81", " Litoral ", "", "AGENDA@X.COM", null, null, true);
        assertThat(f.cnpj()).isEqualTo("11222333000181");
        assertThat(f.cnpjFormatado()).isEqualTo("11.222.333/0001-81");
        assertThat(f.razaoSocial()).isEqualTo("Litoral");
        assertThat(f.nomeFantasia()).isNull();
        assertThat(f.email()).isEqualTo("agenda@x.com");
        assertThatThrownBy(() -> new Fornecedor(null, "11222333000182", "X", null, "a@b.com", null, null, true))
                .hasMessage("CNPJ inválido");
        assertThatThrownBy(() -> new Fornecedor(null, "11222333000181", "X", null, "sem-arroba", null, null, true))
                .hasMessage("E-mail inválido");
        assertThatThrownBy(() -> new Fornecedor(null, "11222333000181", "X".repeat(81), null, "a@b.com", null, null, true))
                .hasMessageContaining("no máximo 80");
        assertThatThrownBy(() -> Textos.obrigatorio(" ", "Campo", 5)).hasMessage("Campo é obrigatório");
        assertThat(Textos.email(null, "E", false)).isNull();
    }

    @Test
    void centroDistribuicaoHerdaOuSobrescreveCapacidade() {
        CentroDistribuicao padrao = new CentroDistribuicao(1L, "806", "CD", null, null, null, true);
        assertThat(padrao.capacidadeDia(480)).isEqualTo(480);
        assertThat(padrao.ajustar(List.of(F1))).containsExactly(F1);
        CentroDistribuicao menor = new CentroDistribuicao(2L, "807", "CD 2", "X", 200, 50, true);
        assertThat(menor.capacidadeDia(480)).isEqualTo(200);
        assertThat(menor.ajustar(List.of(F1)).get(0).capacidadePaletes()).isEqualTo(50);
        assertThatThrownBy(() -> new CentroDistribuicao(null, "1", "x", null, 0, null, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new CentroDistribuicao(null, "1", "x", null, null, 0, true)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void reagendarEVincular() {
        Agendamento a = base().status(StatusAgendamento.CONFIRMADO).build();
        assertThat(a.getCdId()).isEqualTo(Agendamento.CD_PADRAO);
        a.vincular(2L, 5L);
        assertThat(a.getCdId()).isEqualTo(2L);
        assertThat(a.getFornecedorId()).isEqualTo(5L);
        a.vincular(null, null);
        assertThat(a.getCdId()).isEqualTo(2L);
        a.reagendar(LocalDate.of(2026, 11, 4), F2, AGORA);
        assertThat(a.getData()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(a.getFaixaHorariaId()).isEqualTo(2L);
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThatThrownBy(() -> a.reagendar(LocalDate.of(2026, 11, 4), F2, AGORA)).hasMessageContaining("diferente");
        assertThatThrownBy(() -> a.reagendar(LocalDate.of(2026, 11, 1), F1, AGORA)).hasMessageContaining("anterior");
        assertThatThrownBy(() -> a.reagendar(null, F1, AGORA)).hasMessage("Data é obrigatória");
        assertThatThrownBy(() -> a.reagendar(LocalDate.of(2026, 11, 5), new FaixaHoraria(3L, LocalTime.of(13, 0),
                LocalTime.of(15, 0), 120, false), AGORA)).hasMessageContaining("inativa");
        Agendamento chegou = base().status(StatusAgendamento.CHEGOU).build();
        assertThatThrownBy(() -> chegou.reagendar(LocalDate.of(2026, 11, 5), F2, AGORA)).hasMessageContaining("remarcar");
    }

    @Test
    void produtoPesoEPaletes() {
        Produto p = new Produto(1L, "ARZ5", "789-100", "Arroz", "FD", new BigDecimal("5.1"), 40, true);
        assertThat(p.ean()).isEqualTo("789100");
        assertThat(p.pesoPara(new BigDecimal("3"))).isEqualByComparingTo("15.300");
        assertThat(p.paletesPara(new BigDecimal("81"))).isEqualTo(3);
        Produto semPeso = p.comPeso(null, null);
        assertThat(semPeso.pesoPara(BigDecimal.ONE)).isNull();
        assertThat(semPeso.paletesPara(BigDecimal.ONE)).isNull();
        assertThatThrownBy(() -> new Produto(null, "x", null, "y", null, new BigDecimal("-1"), null, true)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new Produto(null, "x", null, "y", null, null, 0, true)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void leNfeEPesoDivergente() throws Exception {
        NotaFiscal n = new LeitorNfeXml().ler(xml());
        assertThat(n.chave()).hasSize(44).startsWith("3526");
        assertThat(n.numero()).isEqualTo("12345");
        assertThat(n.serie()).isEqualTo("1");
        assertThat(n.emitenteCnpj()).isEqualTo("11222333000181");
        assertThat(n.emissao()).isNotNull();
        assertThat(n.valorTotal()).isEqualByComparingTo("16000");
        assertThat(n.pesoBrutoKg()).isEqualByComparingTo("3600");
        assertThat(n.volumes()).isEqualTo(150);
        assertThat(n.itens()).hasSize(2);
        assertThat(n.itens().get(0).ean()).isEqualTo("7891000100103");
        assertThat(n.itens().get(1).ean()).isNull();
        assertThat(n.pesoCalculadoKg()).isNull();
        assertThat(n.pesoDivergente()).isFalse();

        var itens = List.of(n.itens().get(0).comPeso(new BigDecimal("2500"), 1L), n.itens().get(1).comPeso(new BigDecimal("500"), 2L));
        NotaFiscal pesada = n.comVinculo(7L, itens, "admin", AGORA);
        assertThat(pesada.agendamentoId()).isEqualTo(7L);
        assertThat(pesada.pesoCalculadoKg()).isEqualByComparingTo("3000");
        assertThat(pesada.itensSemPeso()).isZero();
        assertThat(pesada.pesoDivergente()).isTrue();
        var proximos = List.of(n.itens().get(0).comPeso(new BigDecimal("3000"), 1L), n.itens().get(1).comPeso(new BigDecimal("550"), 2L));
        assertThat(n.comVinculo(7L, proximos, "admin", AGORA).pesoDivergente()).isFalse();
    }

    @Test
    void nfeInvalidaOuComXxeERecusada() {
        LeitorNfeXml l = new LeitorNfeXml();
        assertThatThrownBy(() -> l.ler(new byte[0])).hasMessage("Arquivo XML vazio");
        assertThatThrownBy(() -> l.ler(new byte[LeitorNfeXml.TAMANHO_MAXIMO + 1])).hasMessageContaining("2 MB");
        assertThatThrownBy(() -> l.ler("isso não é xml".getBytes(StandardCharsets.UTF_8))).hasMessageContaining("XML inválido");
        assertThatThrownBy(() -> l.ler("<a><b/></a>".getBytes(StandardCharsets.UTF_8))).hasMessageContaining("infNFe");
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><NFe><infNFe Id=\"&e;\"/></NFe>";
        assertThatThrownBy(() -> l.ler(xxe.getBytes(StandardCharsets.UTF_8))).hasMessageContaining("não permitido");
        assertThatThrownBy(() -> l.ler("<NFe><infNFe Id=\"NFe123\"/></NFe>".getBytes(StandardCharsets.UTF_8))).hasMessageContaining("Chave");
        String semItens = "<NFe><infNFe Id=\"NFe" + "1".repeat(44) + "\"><ide><nNF>1</nNF></ide></infNFe></NFe>";
        assertThatThrownBy(() -> l.ler(semItens.getBytes(StandardCharsets.UTF_8))).hasMessage("NF-e sem itens");
        String numeroRuim = "<NFe><infNFe Id=\"NFe" + "1".repeat(44) + "\"><ide><nNF>1</nNF><dhEmi>ontem</dhEmi></ide>"
                + "<det nItem=\"x\"><prod><cProd>A</cProd><xProd>B</xProd><qCom>abc</qCom></prod></det></infNFe></NFe>";
        assertThatThrownBy(() -> l.ler(numeroRuim.getBytes(StandardCharsets.UTF_8))).hasMessageContaining("numérico");
        String semCodigo = "<NFe><infNFe Id=\"NFe" + "1".repeat(44) + "\"><ide><nNF>1</nNF></ide>"
                + "<det><prod><xProd>B</xProd></prod></det></infNFe></NFe>";
        assertThatThrownBy(() -> l.ler(semCodigo.getBytes(StandardCharsets.UTF_8))).hasMessage("NF-e sem o campo cProd");
        String minima = "<NFe><infNFe Id=\"NFe" + "1".repeat(44) + "\"><ide><nNF>1</nNF><dhEmi>ontem</dhEmi></ide>"
                + "<det nItem=\"x\"><prod><cProd>A</cProd><xProd>" + "B".repeat(130) + "</xProd><qCom>2</qCom></prod></det>"
                + "<det><outro/></det></infNFe></NFe>";
        NotaFiscal n = l.ler(minima.getBytes(StandardCharsets.UTF_8));
        assertThat(n.emissao()).isNull();
        assertThat(n.itens()).hasSize(1);
        assertThat(n.itens().get(0).numeroItem()).isEqualTo(1);
        assertThat(n.itens().get(0).descricao()).hasSize(120);
        assertThat(n.volumes()).isNull();
    }

    @Test
    void calculaDescarga() {
        ParametroDescarga p = new ParametroDescarga(1L, 60, 30, new BigDecimal("3.50"));
        CalculadoraDescarga c = new CalculadoraDescarga();
        Agendamento aguardando = base().chegadaEm(AGORA).build();
        var i1 = c.calcular(aguardando, p, AGORA.plusMinutes(45));
        assertThat(i1.situacao()).isEqualTo(CalculadoraDescarga.Situacao.AGUARDANDO);
        assertThat(i1.esperaMinutos()).isEqualTo(45);
        assertThat(i1.esperaAcimaTolerancia()).isTrue();
        assertThat(i1.descargaMinutos()).isNull();
        Agendamento descarregando = base().chegadaEm(AGORA).inicioRecebimentoEm(AGORA.plusMinutes(10)).build();
        assertThat(c.calcular(descarregando, p, AGORA.plusMinutes(40)).situacao()).isEqualTo(CalculadoraDescarga.Situacao.EM_DESCARGA);
        Agendamento longo = base().chegadaEm(AGORA).inicioRecebimentoEm(AGORA.plusMinutes(10))
                .fimRecebimentoEm(AGORA.plusMinutes(100)).paletesRecebidos(20).build();
        var i3 = c.calcular(longo, p, AGORA);
        assertThat(i3.situacao()).isEqualTo(CalculadoraDescarga.Situacao.ACIMA_DO_PREVISTO);
        assertThat(i3.excessoMinutos()).isEqualTo(30);
        assertThat(i3.custo()).isEqualByComparingTo("70.00");
        Agendamento rapido = base().chegadaEm(AGORA).inicioRecebimentoEm(AGORA).fimRecebimentoEm(AGORA.plusMinutes(30)).build();
        assertThat(c.calcular(rapido, p, AGORA).situacao()).isEqualTo(CalculadoraDescarga.Situacao.NO_PRAZO);
        var semParam = c.calcular(rapido, null, AGORA);
        assertThat(semParam.previstoMinutos()).isNull();
        assertThat(semParam.custo()).isNull();
        assertThat(c.calcular(base().build(), p, AGORA).esperaMinutos()).isNull();
        assertThatThrownBy(() -> new ParametroDescarga(null, 1, 0, BigDecimal.ONE)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ParametroDescarga(1L, 0, 0, BigDecimal.ONE)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ParametroDescarga(1L, 1, -1, BigDecimal.ONE)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new ParametroDescarga(1L, 1, 0, new BigDecimal("-1"))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cobrancaPagaECancelada() {
        LocalDate hoje = AGORA.toLocalDate();
        assertThatThrownBy(() -> CobrancaPalete.nova("F", 30, BigDecimal.TEN, hoje, null, 20, "admin", AGORA))
                .hasMessageContaining("saldo em aberto");
        assertThatThrownBy(() -> CobrancaPalete.nova("F", 5, BigDecimal.TEN, hoje.minusDays(1), null, 20, "admin", AGORA))
                .hasMessageContaining("anterior");
        CobrancaPalete c = CobrancaPalete.nova("F", 5, new BigDecimal("45"), hoje, "obs", 20, "admin", AGORA);
        assertThat(c.valorTotal()).isEqualByComparingTo("225.00");
        assertThat(c.vencida(hoje)).isFalse();
        assertThat(c.vencida(hoje.plusDays(1))).isTrue();
        assertThat(c.ativa()).isTrue();
        assertThatThrownBy(() -> c.pagar(null, "RP", "admin", AGORA)).hasMessageContaining("Forma");
        c.pagar(CobrancaPalete.FormaPagamento.PIX, "RP-1", "admin", AGORA);
        assertThat(c.getStatus()).isEqualTo(CobrancaPalete.Status.PAGA);
        assertThat(c.getReciboNumero()).isEqualTo("RP-1");
        assertThat(c.getRecebidoPor()).isEqualTo("admin");
        assertThat(c.getPagoEm()).isEqualTo(AGORA);
        assertThat(c.getFormaPagamento()).isEqualTo(CobrancaPalete.FormaPagamento.PIX);
        assertThat(c.vencida(hoje.plusDays(9))).isFalse();
        assertThatThrownBy(() -> c.pagar(CobrancaPalete.FormaPagamento.PIX, "RP", "admin", AGORA)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> c.cancelar("x")).isInstanceOf(RegraNegocioException.class);
        CobrancaPalete d = CobrancaPalete.nova("F", 1, BigDecimal.ONE, hoje, null, 1, "admin", AGORA);
        assertThatThrownBy(() -> d.cancelar(" ")).isInstanceOf(RegraNegocioException.class);
        d.cancelar("erro de digitação");
        assertThat(d.ativa()).isFalse();
        assertThat(d.getMotivoCancelamento()).isEqualTo("erro de digitação");
        assertThatThrownBy(() -> new CobrancaPalete(null, "F", 0, BigDecimal.ONE, hoje, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new CobrancaPalete(null, "F", 1, null, hoje, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new CobrancaPalete(null, "F", 1, BigDecimal.ONE, null, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void movimentoComSinal() {
        var q = new ValePalete.Quantidades(10, 2, 1, 0);
        MovimentoPalete envio = new MovimentoPalete(null, LocalDate.of(2026, 11, 2), MovimentoPalete.Tipo.ENVIO, 1L, "Loja 1",
                "RM-1", q, null, "admin", AGORA);
        assertThat(envio.total()).isEqualTo(13);
        MovimentoPalete retorno = new MovimentoPalete(null, LocalDate.of(2026, 11, 2), MovimentoPalete.Tipo.RETORNO, 1L, "Loja 1",
                "RM-2", q, null, "admin", AGORA);
        assertThat(retorno.pbr()).isEqualTo(-10);
        assertThat(retorno.chep()).isEqualTo(-2);
        assertThat(retorno.chapatex()).isEqualTo(-1);
        assertThat(retorno.descartaveis()).isZero();
        assertThatThrownBy(() -> new MovimentoPalete(null, null, MovimentoPalete.Tipo.ENVIO, 1L, "L", "R", q, null, null, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new MovimentoPalete(null, LocalDate.now(), null, 1L, "L", "R", q, null, null, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new MovimentoPalete(null, LocalDate.now(), MovimentoPalete.Tipo.ENVIO, null, "L", "R", q, null, null, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new MovimentoPalete(null, LocalDate.now(), MovimentoPalete.Tipo.ENVIO, 1L, "L", "R",
                ValePalete.Quantidades.ZERO, null, null, null)).hasMessageContaining("ao menos um");
    }
}
