package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.ValePalete;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ValePaleteRepositorio {
    ValePalete salvar(ValePalete vale);

    Optional<ValePalete> buscarPorId(Long id);

    Optional<ValePalete> buscarPorCodigo(String codigo);

    List<ValePalete> listar(LocalDate inicio, LocalDate fim, ValePalete.Status status, String fornecedor);

    long proximaSequencia();
}
