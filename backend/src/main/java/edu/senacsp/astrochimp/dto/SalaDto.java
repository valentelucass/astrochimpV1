package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class SalaDto {

    // Recebe do professor professorId e nomeAula para abrir a sala. Devolve id, código, aula, estado, nome do professor
    // e datas aos participantes, sem tokens dos alunos, e-mail ou senha do professor, nem o gabarito do quiz.
    // Devolve o id da sala no JSON, mas ignora um id enviado; o mesmo vale para código, estado, nome do dono e datas.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String codigo;
    private String nomeAula;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String status;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nomeProfessor;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime criadaEm;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime expiraEm;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int totalAlunos;
    // Recebe o id do professor na criação, sem acrescentar professorId à resposta da sala.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long professorId;

    // Cria uma SalaDto vazia ao ler o JSON de abertura da sala, preenchendo os campos de entrada pelos setters.
    @JsonCreator
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

    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
    public Long getId() {
        return id;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
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

    public int getTotalAlunos() {
        return totalAlunos;
    }

    public void setTotalAlunos(int totalAlunos) {
        this.totalAlunos = totalAlunos;
    }
}
