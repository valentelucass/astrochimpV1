package edu.senacsp.astrochimp.dto;

public class CriarAlternativaDto {

    private String texto;
    private boolean correta;

    public CriarAlternativaDto() {
    }

    public CriarAlternativaDto(String texto, boolean correta) {
        this.texto = texto;
        this.correta = correta;
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
