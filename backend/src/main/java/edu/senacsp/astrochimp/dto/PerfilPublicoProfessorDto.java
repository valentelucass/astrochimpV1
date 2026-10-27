package edu.senacsp.astrochimp.dto;

import java.util.ArrayList;
import java.util.List;

public class PerfilPublicoProfessorDto {

    // Envia aos visitantes nome, descrição, foto, id e endereço público do professor junto dos materiais publicados;
    // não inclui e-mail, senha, salas ou materiais privados.
    private Long id;
    private String nome;
    private String descricao;
    private String fotoUrl;
    private String slugPerfilPublico;
    private List<MaterialDto> materiais = new ArrayList<>();

    public PerfilPublicoProfessorDto() {
    }

    public PerfilPublicoProfessorDto(
            Long id,
            String nome,
            String descricao,
            String fotoUrl,
            String slugPerfilPublico,
            List<MaterialDto> materiais) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.fotoUrl = fotoUrl;
        this.slugPerfilPublico = slugPerfilPublico;
        this.materiais = materiais;
    }

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
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

    public List<MaterialDto> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<MaterialDto> materiais) {
        this.materiais = materiais;
    }
}
