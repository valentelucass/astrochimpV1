package edu.senacsp.astrochimp.dto;

public class ResultadoAlternativaDto {

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
