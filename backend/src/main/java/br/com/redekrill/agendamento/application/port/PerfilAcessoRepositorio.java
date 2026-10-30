package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.PerfilAcesso;

import java.util.List;
import java.util.Optional;

public interface PerfilAcessoRepositorio {
    List<PerfilAcesso> listar();

    Optional<PerfilAcesso> buscarPorId(Long id);

    Optional<PerfilAcesso> buscarPorNome(String nome);

    PerfilAcesso salvar(PerfilAcesso p);
}
