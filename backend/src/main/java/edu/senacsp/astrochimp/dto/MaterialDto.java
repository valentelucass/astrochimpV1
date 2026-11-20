package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class MaterialDto {



    // Devolve o conteúdo que o app pode abrir sem expor o caminho do arquivo no servidor.
    // Este DTO existe porque conteudoOuUrl precisa devolver a rota de download no lugar do caminho interno quando o material é um arquivo.



    // Id e data de criação só saem no JSON; o servidor define esses dados ao salvar o material.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String titulo;
    private String tipo;
    private String conteudoOuUrl; // Se for arquivo, o service devolve a rota de download no lugar do caminho salvo.
    private String detalhe;
    // Boolean distingue campo ausente (null) de false: tornar o material privado ou desafixá-lo exige false explícito.
    private Boolean publico;
    private Boolean fixado;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime criadoEm;

    // Usa este construtor vazio ao ler o JSON de um material e depois preenche os campos pelos setters.
    @JsonCreator
    public MaterialDto() {
    }






    public MaterialDto(
            Long id,
            String titulo,
            String tipo,
            String conteudoOuUrl,
            String detalhe,
            boolean publico,
            boolean fixado,
            LocalDateTime criadoEm) {
        this.id = id;
        this.titulo = titulo;
        this.tipo = tipo;
        this.conteudoOuUrl = conteudoOuUrl;
        this.detalhe = detalhe;
        this.publico = publico;
        this.fixado = fixado;
        this.criadoEm = criadoEm;
    }







    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getConteudoOuUrl() {
        return conteudoOuUrl;
    }

    public void setConteudoOuUrl(String conteudoOuUrl) {
        this.conteudoOuUrl = conteudoOuUrl;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(String detalhe) {
        this.detalhe = detalhe;
    }

    public Boolean isPublico() {
        return publico;
    }

    public void setPublico(Boolean publico) {
        this.publico = publico;
    }

    public Boolean isFixado() {
        return fixado;
    }

    public void setFixado(Boolean fixado) {
        this.fixado = fixado;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
