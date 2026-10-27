package br.com.redekrill.agendamento.infrastructure.persistence.jpa;

import br.com.redekrill.agendamento.infrastructure.persistence.entity.TipoCaminhaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TipoCaminhaoJpa extends JpaRepository<TipoCaminhaoEntity, Long> {
    @Query("select count(t) > 0 from TipoCaminhaoEntity t where lower(t.nome) = lower(:nome) and (:id is null or t.id <> :id)")
    boolean existeNome(String nome, Long id);
}
