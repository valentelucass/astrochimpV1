package edu.senacsp.astrochimp.dto;

import java.util.ArrayList;
import java.util.List;

public class QuizProfessorDto {

    private Long id;
    private String titulo;
    private boolean salvoNoPerfil;
    private List<PerguntaDto> perguntas = new ArrayList<>();

    public QuizProfessorDto() {
    }

    public QuizProfessorDto(
            Long id,
            String titulo,
            boolean salvoNoPerfil,
            List<PerguntaDto> perguntas) {
        this.id = id;
        this.titulo = titulo;
        this.salvoNoPerfil = salvoNoPerfil;
        this.perguntas = perguntas;
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

    public boolean isSalvoNoPerfil() {
        return salvoNoPerfil;
    }

    public void setSalvoNoPerfil(boolean salvoNoPerfil) {
        this.salvoNoPerfil = salvoNoPerfil;
    }

    public List<PerguntaDto> getPerguntas() {
        return perguntas;
    }

    public void setPerguntas(List<PerguntaDto> perguntas) {
        this.perguntas = perguntas;
    }
}
