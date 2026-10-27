package edu.senacsp.astrochimp.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Guarda cada quiz como um registro no banco.
@Entity
public class Quiz {

    // O id identifica este quiz no banco.
    @Id
    // O banco gera o id quando este quiz é salvo pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Devolve o id do quiz na resposta, mas ignora um id enviado no pedido.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    // Exige que o título do quiz tenha um valor no banco.
    @Column(nullable = false)
    private String titulo;

    // Aceita salvarNoPerfil no pedido, mesmo com o campo chamado salvoNoPerfil.
    @JsonAlias("salvarNoPerfil")
    private boolean salvoNoPerfil;

    // Ignora criadoEm na entrada e na saída do JSON; o service define a data de criação do quiz.
    @JsonIgnore
    private LocalDateTime criadoEm;

    // Também ignora professor no JSON: e-mail, senha e listas de salas, materiais e quizzes não entram na resposta.
    @JsonIgnore
    // Cada quiz tem um professor dono, que pode criar vários quizzes.
    @ManyToOne
    private Professor professor;

    // Um quiz tem várias perguntas; ao salvar o quiz, também salvamos as perguntas.
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    // Busca as perguntas na ordem definida pelo campo ordem.
    @OrderBy("ordem ASC")
    private List<QuizPergunta> perguntas = new ArrayList<>();

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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
