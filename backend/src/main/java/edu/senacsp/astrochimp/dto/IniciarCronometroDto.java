package edu.senacsp.astrochimp.dto;

public class IniciarCronometroDto {

    private Integer segundos;

    public IniciarCronometroDto() {
    }

    public IniciarCronometroDto(Integer segundos) {
        this.segundos = segundos;
    }

    public Integer getSegundos() {
        return segundos;
    }

    public void setSegundos(Integer segundos) {
        this.segundos = segundos;
    }
}
