package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.ConfiguracaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoJpa extends JpaRepository<ConfiguracaoEntity, String> {
}
