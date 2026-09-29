package edu.senacsp.astrochimp.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
// Representa um quiz criado por um professor e composto por várias perguntas.
// Pode ser utilizado em salas para atividades interativas.
public class Quiz {

    // Identificador único do quiz.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Título do quiz.
    @Column(nullable = false)
    private String titulo;

    // Indica se o quiz foi salvo no perfil do professor para reutilização.
    private boolean salvoNoPerfil;

    // Data e hora em que o quiz foi criado.
    private LocalDateTime criadoEm;

    // Professor responsável pela criação do quiz.
    @ManyToOne
    private Professor professor;

    // Perguntas associadas a esse quiz.
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