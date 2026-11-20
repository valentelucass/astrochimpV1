package edu.senacsp.astrochimp.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoPerguntaDto {



    // Devolve ao professor o placar da pergunta atual sem incluir as respostas individuais dos alunos.
    // Este DTO existe porque resultados reúne a contagem por alternativa sem enviar as respostas individuais dos alunos.



    private Long perguntaId;
    private String enunciado;
    private long totalRespondentes;
    private List<ResultadoAlternativaDto> resultados = new ArrayList<>(); // Cada item traz uma opção e sua contagem, sem identificar quem respondeu.

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
