package edu.senacsp.astrochimp.dto;

public class PublicarMaterialDto {

    private boolean publico;

    public PublicarMaterialDto() {
    }

    public PublicarMaterialDto(boolean publico) {
        this.publico = publico;
    }

    public boolean isPublico() {
        return publico;
    }

    public void setPublico(boolean publico) {
        this.publico = publico;
    }
}
