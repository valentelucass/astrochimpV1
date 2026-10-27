package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Guarda a alternativa que cada aluno escolheu em uma rodada do quiz.
@Entity
public class QuizResposta {

    // O id identifica esta resposta do aluno no banco.
    @Id
    // O banco gera o id quando esta resposta do aluno é salva pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime respondidoEm;

    private String sessaoId;

    // Cada resposta tem um aluno, que pode responder outras perguntas.
    @ManyToOne
    private Aluno aluno;

    @ManyToOne
    private QuizAlternativa alternativa;

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRespondidoEm() {
        return respondidoEm;
    }

    public void setRespondidoEm(LocalDateTime respondidoEm) {
        this.respondidoEm = respondidoEm;
    }

    public String getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(String sessaoId) {
        this.sessaoId = sessaoId;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public void setAluno(Aluno aluno) {
        this.aluno = aluno;
    }

    public QuizAlternativa getAlternativa() {
        return alternativa;
    }

    public void setAlternativa(QuizAlternativa alternativa) {
        this.alternativa = alternativa;
    }
}
