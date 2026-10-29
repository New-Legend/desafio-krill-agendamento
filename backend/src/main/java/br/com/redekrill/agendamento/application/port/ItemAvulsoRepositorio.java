package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.domain.model.ItemAvulso;

import java.util.List;

public interface ItemAvulsoRepositorio {
    List<ItemAvulso> listar(Long agendamentoId);

    ItemAvulso salvar(ItemAvulso item);
}
