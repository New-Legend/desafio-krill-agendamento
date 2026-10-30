package br.com.redekrill.agendamento.infrastructure.security;

import br.com.redekrill.agendamento.domain.model.Permissao;
import org.springframework.http.HttpMethod;

import java.util.List;

import static br.com.redekrill.agendamento.domain.model.Permissao.*;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * Tabela rota → permissão numerada. Toda escrita da API precisa estar aqui; o que não estiver é negado.
 * Leituras de operação ficam liberadas para qualquer usuário logado (as telas compartilham listas como CDs e faixas);
 * leituras administrativas (usuários, perfis, logs e auditoria) também exigem número.
 */
final class RegrasAcesso {

    record Regra(HttpMethod metodo, String caminho, Permissao... permissoes) {
        String[] authorities() {
            return java.util.Arrays.stream(permissoes).map(Permissao::authority).toArray(String[]::new);
        }
    }

    private static final String API = "/api/v1";

    static final List<Regra> REGRAS = List.of(
            new Regra(GET, API + "/usuarios/**", USUARIOS_VER, USUARIOS_EDITAR),
            new Regra(POST, API + "/usuarios/**", USUARIOS_EDITAR),
            new Regra(PUT, API + "/usuarios/**", USUARIOS_EDITAR),
            new Regra(GET, API + "/perfis/**", PERFIS, USUARIOS_VER, USUARIOS_EDITAR),
            new Regra(POST, API + "/perfis/**", PERFIS),
            new Regra(PUT, API + "/perfis/**", PERFIS),
            new Regra(GET, API + "/logs-acesso/**", LOGS_ACESSO),
            new Regra(GET, API + "/auditoria/**", AUDITORIA),

            new Regra(POST, API + "/agendamentos", AGENDAMENTO_CRIAR),
            new Regra(PATCH, API + "/agendamentos/*/cancelamento", AGENDAMENTO_ALTERAR),
            new Regra(PATCH, API + "/agendamentos/*/data", AGENDAMENTO_ALTERAR),
            new Regra(POST, API + "/agendamentos/*/link-fornecedor", LINK_FORNECEDOR),
            new Regra(PATCH, API + "/agendamentos/*/status", PORTARIA_CONFERENCIA),
            new Regra(PUT, API + "/agendamentos/*/doca", PATIO_DOCA),
            new Regra(PUT, API + "/agendamentos/*/logistica", PATIO_DOCA),
            new Regra(POST, API + "/agendamentos/*/notas", NOTAS_AVULSOS),
            new Regra(DELETE, API + "/notas/*", NOTAS_AVULSOS),
            new Regra(POST, API + "/agendamentos/*/itens-avulsos", NOTAS_AVULSOS),

            new Regra(POST, API + "/vales-palete", VALE_EMITIR),
            new Regra(PUT, API + "/vales-palete/configuracao", TIPOS_PALETE),
            new Regra(PUT, API + "/vales-palete/*/assinaturas/*", VALE_ASSINAR),
            new Regra(DELETE, API + "/vales-palete/*/assinaturas/*", VALE_ASSINAR),
            new Regra(POST, API + "/vales-palete/*/devolucoes", VALE_ASSINAR),
            new Regra(PATCH, API + "/vales-palete/*/cancelamento", VALE_ASSINAR),
            new Regra(POST, API + "/paletes/cobrancas/**", COBRANCAS),
            new Regra(PATCH, API + "/paletes/cobrancas/**", COBRANCAS),
            new Regra(POST, API + "/notificacoes/**", COBRANCA_ENVIO),
            new Regra(POST, API + "/paletes/movimentos", PALETES_MOVIMENTO),

            new Regra(null, API + "/cds/**", CDS),
            new Regra(null, API + "/fornecedores/**", FORNECEDORES),
            new Regra(null, API + "/produtos/**", PRODUTOS),
            new Regra(PUT, API + "/parametros/capacidade", CAPACIDADE_FAIXAS),
            new Regra(null, API + "/faixas-horarias/**", CAPACIDADE_FAIXAS),
            new Regra(null, API + "/capacidades-recebimento/**", CAPACIDADE_CD_LOJA),
            new Regra(null, API + "/docas/**", DOCAS),
            new Regra(null, API + "/tipos-caminhao/**", CAMINHOES_PALETIZACAO),
            new Regra(null, API + "/tipos-paletizacao/**", CAMINHOES_PALETIZACAO),
            new Regra(PUT, API + "/parametros/descarga/**", PARAMETROS_DESCARGA),
            new Regra(null, API + "/taxas-descarga/**", TAXAS_DESCARGA),
            new Regra(null, API + "/tipos-palete/**", TIPOS_PALETE),
            new Regra(null, API + "/conferentes/**", CONFERENTES));

    private RegrasAcesso() {
    }
}
