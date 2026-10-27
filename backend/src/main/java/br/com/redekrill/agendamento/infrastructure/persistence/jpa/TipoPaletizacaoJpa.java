package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.TipoPaletizacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TipoPaletizacaoJpa extends JpaRepository<TipoPaletizacaoEntity, Long> {
    @Query("select count(t) > 0 from TipoPaletizacaoEntity t where lower(t.nome) = lower(:nome) and (:id is null or t.id <> :id)")
    boolean existeNome(String nome, Long id);
}
