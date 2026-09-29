package edu.senacsp.astrochimp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
// Registra a resposta de um aluno a uma alternativa de quiz.
// Armazena o momento da resposta e o contexto da sessão do aluno.
public class QuizResposta {

    // Identificador único da resposta.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Data e hora em que a resposta foi registrada.
    private LocalDateTime respondidoEm;

    // Identificador da sessão em que o aluno respondeu.
    private String sessaoId;

    // Aluno que respondeu ao quiz.
    @ManyToOne
    private Aluno aluno;

    // Alternativa escolhida pelo aluno.
    @ManyToOne
    private QuizAlternativa alternativa;

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
