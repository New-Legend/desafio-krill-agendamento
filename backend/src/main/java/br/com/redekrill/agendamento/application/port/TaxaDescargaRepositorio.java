package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.TaxaDescarga;

import java.util.List;
import java.util.Optional;

public interface TaxaDescargaRepositorio {
    List<TaxaDescarga> listar();

    Optional<TaxaDescarga> buscarPorId(Long id);

    boolean existeModal(String modal, Long ignorarId);

    TaxaDescarga salvar(TaxaDescarga t);
}
