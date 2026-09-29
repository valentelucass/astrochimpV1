package edu.senacsp.astrochimp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
// Representa uma alternativa possível de resposta de uma pergunta de quiz.
// A alternativa pode ser marcada como correta e acompanhar a estatística de uso.
public class QuizAlternativa {

    // Identificador único da alternativa.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Letra identificadora da alternativa, como A, B, C ou D.
    @Column(nullable = false)
    private String letra;

    // Texto exibido para o aluno.
    @Column(nullable = false)
    private String texto;

    // Indica se essa alternativa é a resposta correta.
    private boolean correta;

    // Quantidade total de vezes que essa alternativa foi escolhida.
    private int totalRespostas;

    // Pergunta à qual esta alternativa pertence.
    @ManyToOne
    private QuizPergunta pergunta;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLetra() {
        return letra;
    }

    public void setLetra(String letra) {
        this.letra = letra;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isCorreta() {
        return correta;
    }

    public void setCorreta(boolean correta) {
        this.correta = correta;
    }

    public int getTotalRespostas() {
        return totalRespostas;
    }

    public void setTotalRespostas(int totalRespostas) {
        this.totalRespostas = totalRespostas;
    }

    public QuizPergunta getPergunta() {
        return pergunta;
    }

    public void setPergunta(QuizPergunta pergunta) {
        this.pergunta = pergunta;
    }
}