package br.com.redekrill.agendamento.domain.model;

import br.com.redekrill.agendamento.domain.exception.RegraNegocioException;

import java.util.Set;
import java.util.TreeSet;

/** Perfil de acesso com as permissões numeradas que concede. */
public record PerfilAcesso(Long id, String nome, String descricao, boolean ativo, Set<Integer> permissoes, int usuarios) {
    public PerfilAcesso {
        nome = Textos.obrigatorio(nome, "Nome do perfil", 40).replaceAll("\\s+", " ");
        descricao = Textos.opcional(descricao, "Descrição", 160);
        if (nome.length() < 3) throw new RegraNegocioException("Nome do perfil deve ter ao menos 3 caracteres");
        Set<Integer> codigos = new TreeSet<>();
        for (Integer c : permissoes == null ? Set.<Integer>of() : permissoes) {
            if (c == null || Permissao.porCodigo(c).isEmpty()) throw new RegraNegocioException("Permissão desconhecida: " + c);
            codigos.add(c);
        }
        permissoes = Set.copyOf(codigos);
    }

    public boolean concede(Permissao p) {
        return ativo && permissoes.contains(p.codigo());
    }
}
