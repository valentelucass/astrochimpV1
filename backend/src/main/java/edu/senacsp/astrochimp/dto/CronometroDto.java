package edu.senacsp.astrochimp.dto;

public class CronometroDto {

    private Integer segundosTotais;
    private int segundosRestantes;
    private boolean ativo;

    public CronometroDto() {
    }

    public CronometroDto(Integer segundosTotais, int segundosRestantes, boolean ativo) {
        this.segundosTotais = segundosTotais;
        this.segundosRestantes = segundosRestantes;
        this.ativo = ativo;
    }

    public Integer getSegundosTotais() {
        return segundosTotais;
    }

    public void setSegundosTotais(Integer segundosTotais) {
        this.segundosTotais = segundosTotais;
    }

    public int getSegundosRestantes() {
        return segundosRestantes;
    }

    public void setSegundosRestantes(int segundosRestantes) {
        this.segundosRestantes = segundosRestantes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
