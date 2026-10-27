package br.com.redekrill.agendamento.infrastructure.persistence.entity;

import br.com.redekrill.agendamento.domain.model.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String username;
    public String nome;
    @Column(name = "senha_hash")
    public String senhaHash;
    @Enumerated(EnumType.STRING)
    public Perfil perfil;
    public boolean ativo;
}
