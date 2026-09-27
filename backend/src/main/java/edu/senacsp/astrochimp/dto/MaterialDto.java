package edu.senacsp.astrochimp.dto;

import java.time.LocalDateTime;

public class MaterialDto {

    private Long id;
    private String titulo;
    private String tipo;
    private String conteudoOuUrl;
    private String detalhe;
    private boolean publico;
    private boolean fixado;
    private LocalDateTime criadoEm;

    public MaterialDto() {
    }

    public MaterialDto(
            Long id,
            String titulo,
            String tipo,
            String conteudoOuUrl,
            String detalhe,
            boolean publico,
            boolean fixado,
            LocalDateTime criadoEm) {
        this.id = id;
        this.titulo = titulo;
        this.tipo = tipo;
        this.conteudoOuUrl = conteudoOuUrl;
        this.detalhe = detalhe;
        this.publico = publico;
        this.fixado = fixado;
        this.criadoEm = criadoEm;
    }

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
}
