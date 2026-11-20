package edu.senacsp.astrochimp.dto;

public class ResultadoAlternativaDto {



    // Mostra ao professor quantos alunos marcaram cada opção sem expor quem votou nela.
    // Este DTO existe porque totalRespostas mostra quantos alunos escolheram a opção sem identificar quem respondeu.



    private String letra;
    private String texto;
    private long totalRespostas; // Devolve só a contagem desta opção na rodada, sem nomes ou IDs de alunos.

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
