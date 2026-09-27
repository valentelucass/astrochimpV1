package edu.senacsp.astrochimp.dto;

import java.util.List;

public class CriarQuizDto {

    private String titulo;
    private boolean salvarNoPerfil;
    private List<CriarPerguntaDto> perguntas;

    public CriarQuizDto() {
    }

    public CriarQuizDto(String titulo, boolean salvarNoPerfil, List<CriarPerguntaDto> perguntas) {
        this.titulo = titulo;
        this.salvarNoPerfil = salvarNoPerfil;
        this.perguntas = perguntas;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public boolean isSalvarNoPerfil() {
        return salvarNoPerfil;
    }

    public void setSalvarNoPerfil(boolean salvarNoPerfil) {
        this.salvarNoPerfil = salvarNoPerfil;
    }

    public List<CriarPerguntaDto> getPerguntas() {
        return perguntas;
    }

    public void setPerguntas(List<CriarPerguntaDto> perguntas) {
        this.perguntas = perguntas;
    }
}
