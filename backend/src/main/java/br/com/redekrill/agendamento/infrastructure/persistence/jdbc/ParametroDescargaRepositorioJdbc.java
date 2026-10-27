package br.com.redekrill.agendamento.infrastructure.persistence.jdbc;

import br.com.redekrill.agendamento.application.port.ParametroDescargaRepositorio;
import br.com.redekrill.agendamento.domain.model.ParametroDescarga;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParametroDescargaRepositorioJdbc implements ParametroDescargaRepositorio {
    private final JdbcClient db;

    public ParametroDescargaRepositorioJdbc(JdbcClient db) {
        this.db = db;
    }

    @Override
    public List<ParametroDescarga> listar() {
        return db.sql("select * from parametro_descarga order by tipo_caminhao_id").query((r, i) -> new ParametroDescarga(
                r.getLong("tipo_caminhao_id"), r.getInt("minutos_previstos"), r.getInt("tolerancia_espera_minutos"),
                r.getBigDecimal("custo_por_palete"))).list();
    }

    @Override
    public ParametroDescarga salvar(ParametroDescarga p) {
        db.sql("insert into parametro_descarga (tipo_caminhao_id, minutos_previstos, tolerancia_espera_minutos, custo_por_palete) "
                        + "values (?, ?, ?, ?) on conflict (tipo_caminhao_id) do update set minutos_previstos = excluded.minutos_previstos, "
                        + "tolerancia_espera_minutos = excluded.tolerancia_espera_minutos, custo_por_palete = excluded.custo_por_palete")
                .params(p.tipoCaminhaoId(), p.minutosPrevistos(), p.toleranciaEsperaMinutos(), p.custoPorPalete()).update();
        return p;
    }
}
