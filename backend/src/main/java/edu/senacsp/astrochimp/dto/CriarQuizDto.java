package edu.senacsp.astrochimp.dto;

import java.util.ArrayList;
import java.util.List;

public class CriarQuizDto {

    private String titulo;
    private boolean salvarNoPerfil;
    private List<PerguntaDto> perguntas = new ArrayList<>();

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

    public List<PerguntaDto> getPerguntas() {
        return perguntas;
    }

    public void setPerguntas(List<PerguntaDto> perguntas) {
        this.perguntas = perguntas;
    }
}
