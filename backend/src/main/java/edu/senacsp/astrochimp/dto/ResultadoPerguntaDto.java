package edu.senacsp.astrochimp.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoPerguntaDto {

    private Long perguntaId;
    private String enunciado;
    private long totalRespondentes;
    private List<ResultadoAlternativaDto> resultados = new ArrayList<>();

    public ResultadoPerguntaDto() {
    }

    public ResultadoPerguntaDto(
            Long perguntaId,
            String enunciado,
            long totalRespondentes,
            List<ResultadoAlternativaDto> resultados) {
        this.perguntaId = perguntaId;
        this.enunciado = enunciado;
        this.totalRespondentes = totalRespondentes;
        this.resultados = resultados;
    }

    public Long getPerguntaId() {
        return perguntaId;
    }

    public void setPerguntaId(Long perguntaId) {
        this.perguntaId = perguntaId;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public long getTotalRespondentes() {
        return totalRespondentes;
    }

    public void setTotalRespondentes(long totalRespondentes) {
        this.totalRespondentes = totalRespondentes;
    }

    public List<ResultadoAlternativaDto> getResultados() {
        return resultados;
    }

    public void setResultados(List<ResultadoAlternativaDto> resultados) {
        this.resultados = resultados;
    }
}
