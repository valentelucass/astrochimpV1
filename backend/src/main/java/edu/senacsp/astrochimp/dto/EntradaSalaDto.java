package edu.senacsp.astrochimp.dto;

public class EntradaSalaDto {

    private SalaDto sala;
    private Long alunoId;
    private String tokenAluno;

    public EntradaSalaDto() {
    }

    public EntradaSalaDto(SalaDto sala, Long alunoId, String tokenAluno) {
        this.sala = sala;
        this.alunoId = alunoId;
        this.tokenAluno = tokenAluno;
    }

    public SalaDto getSala() {
        return sala;
    }

    public void setSala(SalaDto sala) {
        this.sala = sala;
    }

    public Long getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(Long alunoId) {
        this.alunoId = alunoId;
    }

    public String getTokenAluno() {
        return tokenAluno;
    }

    public void setTokenAluno(String tokenAluno) {
        this.tokenAluno = tokenAluno;
    }
}
