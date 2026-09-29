package edu.senacsp.astrochimp.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
// Representa uma dúvida enviada por um aluno dentro de uma sala.
// A dúvida pode ser fixada, votada e acompanha o estado do atendimento.
public class Duvida {

    // Identificador único da dúvida.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Texto da dúvida, com limite de 280 caracteres.
    @Column(nullable = false, length = 280)
    private String texto;

    // Define se a dúvida foi enviada de forma anônima.
    @Column(nullable = false)
    private boolean anonima;

    // Estado atual da dúvida, como aberta, respondida ou resolvida.
    @Column(nullable = false)
    private String estado;

    // Indica se a dúvida foi fixada para destaque na sala.
    private boolean fixada;

    // Data e hora em que a dúvida foi criada.
    private LocalDateTime criadaEm;

    // Sala em que a dúvida foi registrada.
    @ManyToOne
    private Sala sala;

    // Aluno que enviou a dúvida.
    @ManyToOne
    private Aluno autor;

    // Lista de alunos que votaram na dúvida.
    @ManyToMany
    @JoinTable(
        name = "duvida_voto",
        joinColumns = @JoinColumn(name = "duvida_id"),
        inverseJoinColumns = @JoinColumn(name = "aluno_id"))
    private List<Aluno> votos;

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

    public List<Aluno> getVotos() {
        return votos;
    }

    public void setVotos(List<Aluno> votos) {
        this.votos = votos;
    }
}