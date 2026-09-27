package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private boolean salvoNoPerfil;

    private LocalDateTime criadoEm;

    @ManyToOne
    private Professor professor;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<QuizPergunta> perguntas;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public boolean isSalvoNoPerfil() {
        return salvoNoPerfil;
    }

    public void setSalvoNoPerfil(boolean salvoNoPerfil) {
        this.salvoNoPerfil = salvoNoPerfil;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public List<QuizPergunta> getPerguntas() {
        return perguntas;
    }

    public void setPerguntas(List<QuizPergunta> perguntas) {
        this.perguntas = perguntas;
    }
}