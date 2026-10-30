package br.com.redekrill.agendamento.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Catálogo de permissões numeradas (padrão Bluesoft/Gestão CD): cada tela ou ação tem um número fixo,
 * agrupado por módulo (10xx administração, 11xx operação, 12xx recebimento, 13xx paletes, 14xx cadastros).
 * {@code telas} são os nomes de rota do front que exibem a permissão no botão "Permissões".
 */
public enum Permissao {
    USUARIOS_VER(1001, "Administração", "Usuários: consultar", "usuarios"),
    USUARIOS_EDITAR(1002, "Administração", "Usuários: incluir, alterar e redefinir senha", "usuarios"),
    PERFIS(1003, "Administração", "Perfis e permissões", "perfis", "usuarios"),
    LOGS_ACESSO(1004, "Administração", "Logs de acesso", "logs-acesso"),
    AUDITORIA(1005, "Administração", "Auditoria de ações", "auditoria"),

    OCUPACAO(1101, "Operação", "Ocupação da semana", "painel"),
    AGENDAMENTOS_VER(1102, "Operação", "Agendamentos: consultar", "agendamentos", "detalhe"),
    AGENDAMENTO_CRIAR(1103, "Operação", "Agendamentos: incluir", "novo", "agendamentos"),
    AGENDAMENTO_ALTERAR(1104, "Operação", "Agendamentos: remarcar e cancelar", "agendamentos", "detalhe"),
    LINK_FORNECEDOR(1105, "Operação", "Gerar link do fornecedor", "detalhe"),
    RELATORIO(1106, "Operação", "Relatório de agendamentos", "relatorio"),

    ROTINA(1201, "Recebimento", "Rotina do dia", "rotina"),
    DIVERGENCIAS(1202, "Recebimento", "Divergências", "divergencias"),
    TEMPOS_DESCARGA(1203, "Recebimento", "Tempos de descarga", "descarga"),
    PORTARIA_CONFERENCIA(1204, "Recebimento", "Registrar portaria e conferência", "detalhe", "rotina", "patio"),
    PATIO_VER(1205, "Recebimento", "Pátio e docas: consultar", "patio"),
    PATIO_DOCA(1206, "Recebimento", "Atribuir doca e logística", "patio", "detalhe"),
    NOTAS_AVULSOS(1207, "Recebimento", "Notas fiscais e produto avulso", "detalhe"),

    VALE_VER(1301, "Paletes", "Vale-palete: consultar", "vales", "vale"),
    VALE_EMITIR(1302, "Paletes", "Vale-palete: emitir", "vale-novo", "vales"),
    VALE_ASSINAR(1303, "Paletes", "Vale-palete: assinar, devolver e cancelar", "vale"),
    CONTA_CORRENTE(1304, "Paletes", "Conta-corrente e cobranças: consultar", "saldos", "cobrancas", "recibo"),
    COBRANCAS(1305, "Paletes", "Cobranças: registrar, baixar e cancelar", "cobrancas", "saldos"),
    COBRANCA_ENVIO(1306, "Paletes", "Enviar cobrança por e-mail e WhatsApp", "cobrancas"),
    PALETES_LOJAS(1307, "Paletes", "Paletes nas lojas: consultar", "lojas"),
    PALETES_MOVIMENTO(1308, "Paletes", "Paletes nas lojas: lançar movimento", "lojas"),

    CDS(1401, "Cadastros", "Centros de distribuição", "cds"),
    FORNECEDORES(1402, "Cadastros", "Fornecedores", "fornecedores"),
    PRODUTOS(1403, "Cadastros", "Produtos e pesos", "produtos"),
    CAPACIDADE_FAIXAS(1404, "Cadastros", "Capacidade diária e faixas horárias", "capacidade", "faixas"),
    CAPACIDADE_CD_LOJA(1405, "Cadastros", "Capacidade por CD e loja", "capacidade-recebimento"),
    DOCAS(1406, "Cadastros", "Docas", "docas"),
    CAMINHOES_PALETIZACAO(1407, "Cadastros", "Tipos de caminhão e paletização", "caminhoes", "paletizacao"),
    PARAMETROS_DESCARGA(1408, "Cadastros", "Tempos de descarga previstos", "parametros-descarga"),
    TAXAS_DESCARGA(1409, "Cadastros", "Taxa de descarga por modal", "taxas-descarga"),
    TIPOS_PALETE(1410, "Cadastros", "Tipos de palete e regras do vale", "tipos-palete"),
    CONFERENTES(1411, "Cadastros", "Conferentes", "conferentes");

    private final int codigo;
    private final String modulo;
    private final String nome;
    private final List<String> telas;

    Permissao(int codigo, String modulo, String nome, String... telas) {
        this.codigo = codigo;
        this.modulo = modulo;
        this.nome = nome;
        this.telas = List.of(telas);
    }

    public int codigo() { return codigo; }
    public String modulo() { return modulo; }
    public String nome() { return nome; }
    public List<String> telas() { return telas; }

    /** Authority usada pelo Spring Security, ex.: PERM_1103. */
    public String authority() { return "PERM_" + codigo; }

    public static Optional<Permissao> porCodigo(int codigo) {
        return Arrays.stream(values()).filter(p -> p.codigo == codigo).findFirst();
    }
}
