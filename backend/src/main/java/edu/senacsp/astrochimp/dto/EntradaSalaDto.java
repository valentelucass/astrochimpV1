package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntradaSalaDto {

    // Recebe do aluno código da sala e nomeAluno; devolve a sala, o id criado e tokenAluno para identificar esse aluno
    // nas próximas requisições. Não permite escolher id ou token, nem inclui tokens dos outros participantes.
    // Sala, alunoId e tokenAluno só saem no JSON; o servidor define esses dados depois de conferir a entrada.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private SalaDto sala;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long alunoId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String tokenAluno;
    // Código e nomeAluno são recebidos na entrada e não acrescentam campos ao JSON de resposta.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String codigo;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String nomeAluno;

    // Cria uma EntradaSalaDto vazia para ler código e nomeAluno enviados no JSON de entrada na sala.
    @JsonCreator
    public EntradaSalaDto() {
    }

    public EntradaSalaDto(SalaDto sala, Long alunoId, String tokenAluno) {
        this.sala = sala;
        this.alunoId = alunoId;
        this.tokenAluno = tokenAluno;
    }

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
    public SalaDto getSala() {
        return sala;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNomeAluno() {
        return nomeAluno;
    }

    public void setNomeAluno(String nomeAluno) {
        this.nomeAluno = nomeAluno;
    }

    public void setSala(SalaDto sala) {
        this.sala = sala;
    }

    public Long getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(Long alunoId) {
        this.alunoId = alunoId;
    }

    public String getTokenAluno() {
        return tokenAluno;
    }

    public void setTokenAluno(String tokenAluno) {
        this.tokenAluno = tokenAluno;
    }
}
