package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class AlternativaDto {

    private Long id;
    private String letra;
    private String texto;
    private boolean correta;

    @JsonCreator
    public AlternativaDto() {
    }

    public AlternativaDto(Long id, String letra, String texto, boolean correta) {
        this.id = id;
        this.letra = letra;
        this.texto = texto;
        this.correta = correta;
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

    public boolean isCorreta() {
        return correta;
    }

    public void setCorreta(boolean correta) {
        this.correta = correta;
    }
}
