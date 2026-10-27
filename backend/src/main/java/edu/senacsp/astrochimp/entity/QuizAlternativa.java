package edu.senacsp.astrochimp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

// Guarda cada alternativa de uma pergunta no banco.
@Entity
public class QuizAlternativa {

    // O id identifica esta alternativa no banco.
    @Id
    // O banco gera o id quando esta alternativa é salva pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Devolve o id da alternativa, mas ignora um id enviado no pedido.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    // Exige que a letra da alternativa tenha um valor no banco.
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String letra;

    @Column(nullable = false)
    private String texto;

    private boolean correta;

    // Cada alternativa pertence a uma pergunta, que pode ter várias alternativas.
    @ManyToOne
    // Deixa a pergunta fora do JSON da alternativa para não repetir uma dentro da outra sem fim.
    @JsonIgnore
    private QuizPergunta pergunta;

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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
