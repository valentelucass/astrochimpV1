package edu.senacsp.astrochimp.dto;

import java.util.List;

public class PerguntaAlunoDto {

    private Long quizId;
    private Long perguntaId;
    private String enunciado;
    private String dificuldade;
    private int numeroPergunta;
    private int totalPerguntas;
    private int tempoRestanteSegundos;
    private List<AlternativaAlunoDto> alternativas;

    public PerguntaAlunoDto() {
    }

    public PerguntaAlunoDto(
            Long quizId,
            Long perguntaId,
            String enunciado,
            String dificuldade,
            int numeroPergunta,
            int totalPerguntas,
            int tempoRestanteSegundos,
            List<AlternativaAlunoDto> alternativas) {
        this.quizId = quizId;
        this.perguntaId = perguntaId;
        this.enunciado = enunciado;
        this.dificuldade = dificuldade;
        this.numeroPergunta = numeroPergunta;
        this.totalPerguntas = totalPerguntas;
        this.tempoRestanteSegundos = tempoRestanteSegundos;
        this.alternativas = alternativas;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Long getPerguntaId() {
        return perguntaId;
    }

    public void setPerguntaId(Long perguntaId) {
        this.perguntaId = perguntaId;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(String dificuldade) {
        this.dificuldade = dificuldade;
    }

    public int getNumeroPergunta() {
        return numeroPergunta;
    }

    public void setNumeroPergunta(int numeroPergunta) {
        this.numeroPergunta = numeroPergunta;
    }

    public int getTotalPerguntas() {
        return totalPerguntas;
    }

    public void setTotalPerguntas(int totalPerguntas) {
        this.totalPerguntas = totalPerguntas;
    }

    public int getTempoRestanteSegundos() {
        return tempoRestanteSegundos;
    }

    public void setTempoRestanteSegundos(int tempoRestanteSegundos) {
        this.tempoRestanteSegundos = tempoRestanteSegundos;
    }

    public List<AlternativaAlunoDto> getAlternativas() {
        return alternativas;
    }

    public void setAlternativas(List<AlternativaAlunoDto> alternativas) {
        this.alternativas = alternativas;
    }
}
