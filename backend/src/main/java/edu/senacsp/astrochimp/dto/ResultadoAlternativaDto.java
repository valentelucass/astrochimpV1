package edu.senacsp.astrochimp.dto;

public class ResultadoAlternativaDto {

    // Envia ao professor letra, texto e quantidade de respostas desta alternativa na rodada em andamento da sala,
    // identificada por quizSessaoId; não lista nomes, ids ou tokens dos alunos.
    private String letra;
    private String texto;
    private long totalRespostas;

    public ResultadoAlternativaDto() {
    }

    public ResultadoAlternativaDto(String letra, String texto, long totalRespostas) {
        this.letra = letra;
        this.texto = texto;
        this.totalRespostas = totalRespostas;
    }

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
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

    public long getTotalRespostas() {
        return totalRespostas;
    }

    public void setTotalRespostas(long totalRespostas) {
        this.totalRespostas = totalRespostas;
    }
}
