package edu.senacsp.astrochimp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

// Guarda os dados de cada aluno no banco.
@Entity
public class Aluno {

    // O id identifica este aluno no banco.
    @Id
    // O banco gera o id quando este aluno é salvo pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exige que o nome do aluno tenha um valor no banco.
    @Column(nullable = false)
    private String nome;

    private String fotoUrl;

    @Column(unique = true)
    // O token identifica o aluno nas requisições; só EntradaSalaDto o entrega ao aluno que acabou de entrar.
    @JsonIgnore
    private String token;

    // Um aluno pode participar de várias salas, e cada sala pode ter vários alunos.
    @ManyToMany(mappedBy = "alunos")
    private List<Sala> salas = new ArrayList<>();

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
