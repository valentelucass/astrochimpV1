package edu.senacsp.astrochimp.dto;

public class CadastrarProfessorDto {

    private String nome;
    private String email;
    private String senha;
    private String slugPerfilPublico;

    public CadastrarProfessorDto() {
    }

    public CadastrarProfessorDto(String nome, String email, String senha, String slugPerfilPublico) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.slugPerfilPublico = slugPerfilPublico;
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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getSlugPerfilPublico() {
        return slugPerfilPublico;
    }

    public void setSlugPerfilPublico(String slugPerfilPublico) {
        this.slugPerfilPublico = slugPerfilPublico;
    }
}
