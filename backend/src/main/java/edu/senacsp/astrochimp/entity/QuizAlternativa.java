package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

@Entity
public class QuizAlternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String letra;

    @Column(nullable = false)
    private String texto;

    private boolean correta;

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

    public QuizPergunta getPergunta() {
        return pergunta;
    }

    public void setPergunta(QuizPergunta pergunta) {
        this.pergunta = pergunta;
    }
}
