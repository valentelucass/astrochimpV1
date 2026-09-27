package edu.senacsp.astrochimp.dto;

public class AtualizarProfessorDto {

    private String nome;
    private String descricao;
    private String fotoUrl;
    private String slugPerfilPublico;

    public AtualizarProfessorDto() {
    }

    public AtualizarProfessorDto(String nome, String descricao, String fotoUrl, String slugPerfilPublico) {
        this.nome = nome;
        this.descricao = descricao;
        this.fotoUrl = fotoUrl;
        this.slugPerfilPublico = slugPerfilPublico;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getSlugPerfilPublico() {
        return slugPerfilPublico;
    }

    public void setSlugPerfilPublico(String slugPerfilPublico) {
        this.slugPerfilPublico = slugPerfilPublico;
    }
}
