package edu.senacsp.astrochimp.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoPerguntaDto {

    // Envia ao professor id e enunciado da pergunta atual, total de respondentes e contagem por alternativa na rodada
    // em andamento da sala, identificada por quizSessaoId; não devolve as respostas individuais dos alunos.
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

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
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
