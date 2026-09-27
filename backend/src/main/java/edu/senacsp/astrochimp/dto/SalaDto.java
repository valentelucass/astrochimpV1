package edu.senacsp.astrochimp.dto;

import java.time.LocalDateTime;

public class SalaDto {

    private Long id;
    private String codigo;
    private String nomeAula;
    private String status;
    private String nomeProfessor;
    private LocalDateTime criadaEm;
    private LocalDateTime expiraEm;

    public SalaDto() {
    }

    public SalaDto(
            Long id,
            String codigo,
            String nomeAula,
            String status,
            String nomeProfessor,
            LocalDateTime criadaEm,
            LocalDateTime expiraEm) {
        this.id = id;
        this.codigo = codigo;
        this.nomeAula = nomeAula;
        this.status = status;
        this.nomeProfessor = nomeProfessor;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNomeAula() {
        return nomeAula;
    }

    public void setNomeAula(String nomeAula) {
        this.nomeAula = nomeAula;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNomeProfessor() {
        return nomeProfessor;
    }

    public void setNomeProfessor(String nomeProfessor) {
        this.nomeProfessor = nomeProfessor;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(LocalDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }
}
