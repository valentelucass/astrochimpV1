package edu.senacsp.astrochimp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

// Guarda os dados de cada professor no banco.
@Entity
public class Professor {

    // O id identifica este professor no banco.
    @Id
    // O banco gera o id quando este professor é salvo pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exige que o nome do professor tenha um valor no banco.
    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    // A senha codificada serve à autenticação; ela nunca deve entrar no JSON de uma entidade Professor.
    @JsonIgnore
    private String senha;

    private String descricao;
    private String fotoUrl;

    @Column(unique = true)
    private String slugPerfilPublico;

    // Um professor pode ter várias salas; a ligação fica no campo professor de Sala.
    @OneToMany(mappedBy = "professor")
    private List<Sala> salas = new ArrayList<>();

    @OneToMany(mappedBy = "professor")
    private List<Material> materiais = new ArrayList<>();

    @OneToMany(mappedBy = "professor")
    private List<Quiz> quizzes = new ArrayList<>();

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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

    public List<Sala> getSalas() {
        return salas;
    }

    public void setSalas(List<Sala> salas) {
        this.salas = salas;
    }

    public List<Material> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<Material> materiais) {
        this.materiais = materiais;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }
}
