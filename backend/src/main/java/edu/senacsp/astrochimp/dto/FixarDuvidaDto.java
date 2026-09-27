package edu.senacsp.astrochimp.dto;

public class FixarDuvidaDto {

    private boolean fixada;

    public FixarDuvidaDto() {
    }

    public FixarDuvidaDto(boolean fixada) {
        this.fixada = fixada;
    }

    public boolean isFixada() {
        return fixada;
    }

    public void setFixada(boolean fixada) {
        this.fixada = fixada;
    }
}
