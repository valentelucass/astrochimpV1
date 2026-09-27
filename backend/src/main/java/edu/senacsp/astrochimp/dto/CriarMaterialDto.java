package edu.senacsp.astrochimp.dto;

public class CriarMaterialDto {

    private String titulo;
    private String tipo;
    private String conteudoOuUrl;
    private String detalhe;

    public CriarMaterialDto() {
    }

    public CriarMaterialDto(String titulo, String tipo, String conteudoOuUrl, String detalhe) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.conteudoOuUrl = conteudoOuUrl;
        this.detalhe = detalhe;
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
}
