package edu.senacsp.astrochimp.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
// Representa o professor responsável por criar salas, materiais e quizzes.
// Também mantém os dados públicos para perfil e os conteúdos vinculados ao mesmo.
public class Professor {

    // Identificador único do professor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome do professor.
    @Column(nullable = false)
    private String nome;

    // E-mail do professor, usado como identificador único no sistema.
    @Column(nullable = false, unique = true)
    private String email;

    // Senha do professor, armazenada de forma segura no fluxo de autenticação.
    @Column(nullable = false)
    private String senha;

    // Descrição exibida no perfil público do professor.
    private String descricao;

    // URL da foto de perfil do professor.
    private String fotoUrl;

    // Identificador público usado para criar o perfil do professor.
    @Column(unique = true)
    private String slugPerfilPublico;

    // Salas criadas por esse professor.
    @OneToMany(mappedBy = "professor")
    private List<Sala> salas;

    // Materiais publicados por esse professor.
    @OneToMany(mappedBy = "professor")
    private List<Material> materiais;

    // Quizzes criados pelo professor.
    @OneToMany(mappedBy = "professor")
    private List<Quiz> quizzes;

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