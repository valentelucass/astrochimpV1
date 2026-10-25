package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class QuizPergunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String enunciado;

    private String dificuldade;

    private int ordem;

    private int tempoLimiteSegundos;

    @ManyToOne
    private Quiz quiz;

    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL)
    @OrderBy("letra ASC")
    private List<QuizAlternativa> alternativas = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(String dificuldade) {
        this.dificuldade = dificuldade;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public int getTempoLimiteSegundos() {
        return tempoLimiteSegundos;
    }

    public void setTempoLimiteSegundos(int tempoLimiteSegundos) {
        this.tempoLimiteSegundos = tempoLimiteSegundos;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public List<QuizAlternativa> getAlternativas() {
        return alternativas;
    }

    public void setAlternativas(List<QuizAlternativa> alternativas) {
        this.alternativas = alternativas;
    }
}
