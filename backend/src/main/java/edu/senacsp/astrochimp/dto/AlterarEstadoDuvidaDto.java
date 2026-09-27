package edu.senacsp.astrochimp.dto;

public class AlterarEstadoDuvidaDto {

    private String estado;

    public AlterarEstadoDuvidaDto() {
    }

    public AlterarEstadoDuvidaDto(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
