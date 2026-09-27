package edu.senacsp.astrochimp.dto;

public class PerfilProfessorDto {

    private Long id;
    private String nome;
    private String email;
    private String descricao;
    private String fotoUrl;
    private String slugPerfilPublico;

    public PerfilProfessorDto() {
    }

    public PerfilProfessorDto(
            Long id,
            String nome,
            String email,
            String descricao,
            String fotoUrl,
            String slugPerfilPublico) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.descricao = descricao;
        this.fotoUrl = fotoUrl;
        this.slugPerfilPublico = slugPerfilPublico;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
