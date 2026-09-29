package edu.senacsp.astrochimp.entity;

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
// Representa uma pergunta dentro de um quiz.
// Cada pergunta possui um enunciado, ordem e alternativas associadas.
public class QuizPergunta {

    // Identificador único da pergunta.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Enunciado da pergunta exibido ao aluno.
    @Column(nullable = false)
    private String enunciado;

    // Nível de dificuldade da pergunta.
    private String dificuldade;

    // Ordem da pergunta no quiz.
    private int ordem;

    // Tempo limite para responder a pergunta em segundos.
    private int tempoLimiteSegundos;

    // Quiz ao qual a pergunta pertence.
    @ManyToOne
    private Quiz quiz;

    // Alternativas disponíveis para esta pergunta.
    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL)
    private List<QuizAlternativa> alternativas;

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