package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CronometroDto {

    // Recebe do professor a duração do cronômetro da sala em segundos. Devolve segundosTotais, como 120 segundos,
    // segundosRestantes, que é quanto falta desde o início, e ativo, que indica se a contagem continua.
    // Aceita o nome segundos usado no pedido de início, guardando o valor em segundosTotais sem mudar a resposta.
    @JsonAlias("segundos")
    private Integer segundosTotais;
    // Tempo restante e contagem ativa só saem no JSON; o cliente não pode definir esses valores calculados.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int segundosRestantes;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean ativo;

    // Cria uma CronometroDto vazia para ler a duração enviada pelo professor no JSON.
    @JsonCreator
    public CronometroDto() {
    }

    public CronometroDto(Integer segundosTotais, int segundosRestantes, boolean ativo) {
        this.segundosTotais = segundosTotais;
        this.segundosRestantes = segundosRestantes;
        this.ativo = ativo;
    }

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
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
