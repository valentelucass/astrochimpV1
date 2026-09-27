package edu.senacsp.astrochimp.dto;

import java.util.List;

public class CriarPerguntaDto {

    private String enunciado;
    private String dificuldade;
    private int tempoLimiteSegundos;
    private List<CriarAlternativaDto> alternativas;

    public CriarPerguntaDto() {
    }

    public CriarPerguntaDto(
            String enunciado,
            String dificuldade,
            int tempoLimiteSegundos,
            List<CriarAlternativaDto> alternativas) {
        this.enunciado = enunciado;
        this.dificuldade = dificuldade;
        this.tempoLimiteSegundos = tempoLimiteSegundos;
        this.alternativas = alternativas;
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

    public List<CriarAlternativaDto> getAlternativas() {
        return alternativas;
    }

    public void setAlternativas(List<CriarAlternativaDto> alternativas) {
        this.alternativas = alternativas;
    }
}
