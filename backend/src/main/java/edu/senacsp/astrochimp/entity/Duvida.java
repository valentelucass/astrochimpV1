package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Guarda cada dúvida como um registro no banco.
@Entity
public class Duvida {

    // O id identifica esta dúvida no banco.
    @Id
    // O banco gera o id quando esta dúvida é salva pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exige um texto com até 280 caracteres para a dúvida.
    @Column(nullable = false, length = 280)
    private String texto;

    @Column(nullable = false)
    private boolean anonima;

    @Column(nullable = false)
    private String estado;

    private boolean fixada;

    private LocalDateTime criadaEm;

    // Cada dúvida pertence a uma sala, que pode ter várias dúvidas.
    @ManyToOne
    private Sala sala;

    @ManyToOne
    private Aluno autor;

    // Guarda quais alunos votaram na dúvida para não contar o mesmo aluno duas vezes.
    @ManyToMany
    // Guarda os votos na tabela duvida_voto, ligando cada dúvida aos alunos que votaram.
    @JoinTable(
        name = "duvida_voto",
        // @JoinColumn: A coluna duvida_id indica qual dúvida recebeu o voto.
        joinColumns = @JoinColumn(name = "duvida_id"),
        inverseJoinColumns = @JoinColumn(name = "aluno_id"))
    private List<Aluno> alunosQueVotaram = new ArrayList<>();

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Aluno getAutor() {
        return autor;
    }

    public void setAutor(Aluno autor) {
        this.autor = autor;
    }

    public List<Aluno> getAlunosQueVotaram() {
        return alunosQueVotaram;
    }

    public void setAlunosQueVotaram(List<Aluno> alunosQueVotaram) {
        this.alunosQueVotaram = alunosQueVotaram;
    }
}
