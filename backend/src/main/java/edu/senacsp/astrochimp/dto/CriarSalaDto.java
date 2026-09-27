package edu.senacsp.astrochimp.dto;

public class CriarSalaDto {

    private Long professorId;
    private String nomeAula;

    public CriarSalaDto() {
    }

    public CriarSalaDto(Long professorId, String nomeAula) {
        this.professorId = professorId;
        this.nomeAula = nomeAula;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public String getNomeAula() {
        return nomeAula;
    }

    public void setNomeAula(String nomeAula) {
        this.nomeAula = nomeAula;
    }
}
