package edu.senacsp.astrochimp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

// Guarda cada pergunta de um quiz no banco.
@Entity
public class QuizPergunta {

    // O id identifica esta pergunta no banco.
    @Id
    // O banco gera o id quando esta pergunta é salva pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Devolve o id da pergunta, mas ignora um id enviado no pedido.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    // Exige que o enunciado da pergunta tenha um valor no banco.
    @Column(nullable = false)
    private String enunciado;

    private String dificuldade;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int ordem;

    private int tempoLimiteSegundos;

    // Cada pergunta pertence a um quiz, que pode ter várias perguntas.
    @ManyToOne
    // Deixa o quiz fora do JSON da pergunta para não repetir quiz e perguntas sem fim.
    @JsonIgnore
    private Quiz quiz;

    // Uma pergunta tem várias alternativas; elas são salvas junto com a pergunta.
    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL)
    // Busca as alternativas pela letra: A, B, C e assim por diante.
    @OrderBy("letra ASC")
    private List<QuizAlternativa> alternativas = new ArrayList<>();

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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
