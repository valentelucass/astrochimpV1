package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Duvida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 280)
    private String texto;

    @Column(nullable = false)
    private boolean anonima;

    @Column(nullable = false)
    private String estado;

    private boolean fixada;

    private LocalDateTime criadaEm;

    @ManyToOne
    private Sala sala;

    @ManyToOne
    private Aluno autor;

    @ManyToMany
    @JoinTable(
        name = "duvida_voto",
        joinColumns = @JoinColumn(name = "duvida_id"),
        inverseJoinColumns = @JoinColumn(name = "aluno_id"))
    private List<Aluno> alunosQueVotaram = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isAnonima() {
        return anonima;
    }

    public void setAnonima(boolean anonima) {
        this.anonima = anonima;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isFixada() {
        return fixada;
    }

    public void setFixada(boolean fixada) {
        this.fixada = fixada;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Aluno getAutor() {
        return autor;
    }

    public void setAutor(Aluno autor) {
        this.autor = autor;
    }

    public List<Aluno> getAlunosQueVotaram() {
        return alunosQueVotaram;
    }

    public void setAlunosQueVotaram(List<Aluno> alunosQueVotaram) {
        this.alunosQueVotaram = alunosQueVotaram;
    }
}
