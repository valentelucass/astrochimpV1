package edu.senacsp.astrochimp.dto;

import java.util.List;

public class PerguntaProfessorDto {

    private Long id;
    private String enunciado;
    private String dificuldade;
    private int tempoLimiteSegundos;
    private int ordem;
    private List<AlternativaProfessorDto> alternativas;

    public PerguntaProfessorDto() {
    }

    public PerguntaProfessorDto(
            Long id,
            String enunciado,
            String dificuldade,
            int tempoLimiteSegundos,
            int ordem,
            List<AlternativaProfessorDto> alternativas) {
        this.id = id;
        this.enunciado = enunciado;
        this.dificuldade = dificuldade;
        this.tempoLimiteSegundos = tempoLimiteSegundos;
        this.ordem = ordem;
        this.alternativas = alternativas;
    }

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

    public int getTempoLimiteSegundos() {
        return tempoLimiteSegundos;
    }

    public void setTempoLimiteSegundos(int tempoLimiteSegundos) {
        this.tempoLimiteSegundos = tempoLimiteSegundos;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public List<AlternativaProfessorDto> getAlternativas() {
        return alternativas;
    }

    public void setAlternativas(List<AlternativaProfessorDto> alternativas) {
        this.alternativas = alternativas;
    }
}
