package edu.senacsp.astrochimp.dto;

public class AlternativaAlunoDto {

    private Long id;
    private String letra;
    private String texto;

    public AlternativaAlunoDto() {
    }

    public AlternativaAlunoDto(Long id, String letra, String texto) {
        this.id = id;
        this.letra = letra;
        this.texto = texto;
    }

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
}
