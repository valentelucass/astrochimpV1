package edu.senacsp.astrochimp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

@Entity
// Representa um material didático publicado por um professor.
// Pode ser um texto, arquivo, vídeo, imagem ou link compartilhado em uma sala.
public class Material {

    // Identificador único do material no banco de dados.
    // Gerado automaticamente pelo banco para facilitar a persistência.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Título do material, obrigatório para identificação visual no sistema.
    @Column(nullable = false)
    private String titulo;

    // Tipo do conteúdo, como PDF, vídeo, texto, imagem ou link.
    // Ajuda a classificar e tratar o material de forma adequada.
    @Column(nullable = false)
    private String tipo;

    // Conteúdo do material ou URL externa, dependendo do tipo.
    // O uso de @Lob permite armazenar textos maiores, como descrições extensas.
    @Lob
    private String conteudoOuUrl;

    // Informações extras sobre o material, como detalhes de apresentação ou observações.
    private String detalhe;

    // Indica se o material deve ficar visível publicamente para outros usuários.
    private boolean publico;

    // Flag para marcar material como fixado em destaque.
    // Pode ser usado para priorizar conteúdos importantes na interface.
    private boolean fixado;

    // Data e hora em que o material foi criado.
    // Útil para ordenação e histórico de publicação.
    private LocalDateTime criadoEm;

    // Professor responsável pelo material.
    // Relacionamento muitos-para-um: vários materiais pertencem a um professor.
    @ManyToOne
    private Professor professor;

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

    public boolean isFixado() {
        return fixado;
    }

    public void setFixado(boolean fixado) {
        this.fixado = fixado;
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
