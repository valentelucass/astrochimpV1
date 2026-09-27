package edu.senacsp.astrochimp.dto;

public class CriarDuvidaDto {

    private String texto;
    private boolean anonima;

    public CriarDuvidaDto() {
    }

    public CriarDuvidaDto(String texto, boolean anonima) {
        this.texto = texto;
        this.anonima = anonima;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isAnonima() {
        return anonima;
    }

    public void setAnonima(boolean anonima) {
        this.anonima = anonima;
    }
}
