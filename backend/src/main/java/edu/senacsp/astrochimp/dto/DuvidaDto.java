package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;

public class DuvidaDto {

    private Long id;
    private String texto;
    private boolean anonima;
    private Long autorId;
    private String autorNome;
    private String estado;
    private boolean fixada;
    private int votos;
    private LocalDateTime criadaEm;

    @JsonCreator
    public DuvidaDto() {
    }

    public DuvidaDto(
            Long id,
            String texto,
            boolean anonima,
            Long autorId,
            String autorNome,
            String estado,
            boolean fixada,
            int votos,
            LocalDateTime criadaEm) {
        this.id = id;
        this.texto = texto;
        this.anonima = anonima;
        this.autorId = autorId;
        this.autorNome = autorNome;
        this.estado = estado;
        this.fixada = fixada;
        this.votos = votos;
        this.criadaEm = criadaEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isAnonima() {
        return anonima;
    }

    public void setAnonima(boolean anonima) {
        this.anonima = anonima;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public String getAutorNome() {
        return autorNome;
    }

    public void setAutorNome(String autorNome) {
        this.autorNome = autorNome;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isFixada() {
        return fixada;
    }

    public void setFixada(boolean fixada) {
        this.fixada = fixada;
    }

    public int getVotos() {
        return votos;
    }

    public void setVotos(int votos) {
        this.votos = votos;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }
}
