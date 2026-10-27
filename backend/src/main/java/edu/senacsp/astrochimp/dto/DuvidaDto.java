package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class DuvidaDto {

    // Recebe do aluno texto e anonimato e do professor estado ou fixação, conforme a operação; devolve a dúvida com a
    // quantidade de votos, sem listar votantes. O service oculta id e nome do autor anônimo nas respostas aos alunos,
    // mas permite que o professor veja o autor.
    // Id, autor, votos e data só saem no JSON; um pedido não pode escolher quem escreveu ou quantos votos recebeu.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String texto;
    private boolean anonima;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long autorId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String autorNome;
    private String estado;
    // Boolean fica null se fixada não vier no pedido; false significa que o professor pediu para desafixar a dúvida.
    private Boolean fixada;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int votos;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime criadaEm;

    // Usa este construtor vazio ao ler o JSON de uma dúvida e depois preenche os campos pelos setters.
    @JsonCreator
    public DuvidaDto() {
    }

    public DuvidaDto(
            Long id,
            String texto,
            boolean anonima,
            Long autorId,
            String autorNome,
            String estado,
            boolean fixada,
            int votos,
            LocalDateTime criadaEm) {
        this.id = id;
        this.texto = texto;
        this.anonima = anonima;
        this.autorId = autorId;
        this.autorNome = autorNome;
        this.estado = estado;
        this.fixada = fixada;
        this.votos = votos;
        this.criadaEm = criadaEm;
    }

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public String getAutorNome() {
        return autorNome;
    }

    public void setAutorNome(String autorNome) {
        this.autorNome = autorNome;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean isFixada() {
        return fixada;
    }

    public void setFixada(Boolean fixada) {
        this.fixada = fixada;
    }

    public int getVotos() {
        return votos;
    }

    public void setVotos(int votos) {
        this.votos = votos;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }
}
