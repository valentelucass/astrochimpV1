package edu.senacsp.astrochimp.dto;

public class FixarMaterialDto {

    private boolean fixado;

    public FixarMaterialDto() {
    }

    public FixarMaterialDto(boolean fixado) {
        this.fixado = fixado;
    }

    public boolean isFixado() {
        return fixado;
    }

    public void setFixado(boolean fixado) {
        this.fixado = fixado;
    }
}
