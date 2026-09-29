package edu.senacsp.astrochimp.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
// Representa um aluno participante das salas e atividades do sistema.
// O aluno pode entrar em salas, responder quizzes e interagir com dúvidas.
public class Aluno {

    // Identificador único do aluno no banco de dados.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome do aluno exibido no sistema.
    @Column(nullable = false)
    private String nome;

    // URL da foto de perfil do aluno.
    private String fotoUrl;

    // Token único usado para identificar ou autenticar o aluno.
    @Column(unique = true)
    private String token;

    // Salas em que o aluno está cadastrado.
    @ManyToMany(mappedBy = "alunos")
    private List<Sala> salas;

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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<Sala> getSalas() {
        return salas;
    }

    public void setSalas(List<Sala> salas) {
        this.salas = salas;
    }
}
