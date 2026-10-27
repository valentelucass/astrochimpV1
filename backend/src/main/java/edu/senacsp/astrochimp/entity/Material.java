package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Guarda os dados dos materiais das aulas no banco.
@Entity
public class Material {

    // O id identifica este material no banco.
    @Id
    // O banco gera o id quando este material é salvo pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exige que o título do material tenha um valor no banco.
    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String tipo;

    // Permite guardar conteúdo longo em conteudoOuUrl, como o código de um exemplo da aula.
    @Lob
    private String conteudoOuUrl;

    private String detalhe;

    private boolean publico;

    private LocalDateTime criadoEm;

    // Cada material tem um professor dono, que pode ter vários materiais.
    @ManyToOne
    private Professor professor;

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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

    public boolean isPublico() {
        return publico;
    }

    public void setPublico(boolean publico) {
        this.publico = publico;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

}
