package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PerfilProfessorDto {



    // Aceita a senha no cadastro e devolve o perfil do professor sem expor a senha no JSON.
    // Este DTO existe porque senha precisa entrar no cadastro e ficar fora da resposta do perfil.



    // Devolve o id do professor, mas ignora um id enviado no JSON.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String nome;
    private String email;
    // Aceita a senha no cadastro, mas nunca inclui senha no JSON de resposta, mesmo que o objeto tenha esse valor.






    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // WRITE_ONLY: aceita a senha no cadastro, mas nunca inclui senha no JSON de resposta.
    private String senha; // WRITE_ONLY. A senha entra no JSON do cadastro e fica fora da resposta.






    
    private String descricao;
    private String fotoUrl;
    private String slugPerfilPublico;

    // Usa este construtor vazio ao ler o JSON do perfil e depois preenche os campos pelos setters.
    @JsonCreator
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







    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
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
