package br.com.redekrill.agendamento.application.port;

import br.com.redekrill.agendamento.application.dto.FiltroAuditoria;
import br.com.redekrill.agendamento.application.dto.Pagina;
import br.com.redekrill.agendamento.application.dto.RegistroAuditoria;

public interface AuditoriaRepositorio {
    void registrar(RegistroAuditoria registro);

    Pagina<RegistroAuditoria> listar(FiltroAuditoria filtro, int pagina, int tamanho);
}
