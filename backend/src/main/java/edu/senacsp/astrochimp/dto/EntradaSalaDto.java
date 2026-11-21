package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntradaSalaDto {



    // Recebe código e nome para entrar na sala e devolve o token individual criado para aquele aluno.
    // Este DTO existe porque tokenAluno só sai na resposta de entrada. codigo e nomeAluno só entram no pedido.



    // Sala, alunoId e tokenAluno só saem no JSON; o servidor define esses dados depois de conferir a entrada.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private SalaDto sala;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long alunoId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // READ_ONLY. O servidor gera e devolve o token. O aluno não pode defini-lo no pedido.
    private String tokenAluno;
    // Código e nomeAluno são recebidos na entrada e não acrescentam campos ao JSON de resposta.


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String codigo;


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY. O aluno envia seu nome na entrada, mas o servidor não devolve esse campo no JSON de resposta.
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
