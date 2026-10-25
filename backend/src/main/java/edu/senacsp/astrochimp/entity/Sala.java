package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    private String nomeAula;

    @Column(nullable = false)
    private String status;

    private LocalDateTime criadaEm;

    private LocalDateTime expiraEm;

    private Integer cronometroSegundos;

    private LocalDateTime cronometroIniciadoEm;

    @ManyToOne
    private Professor professor;

    @ManyToOne
    private Quiz quizEmAndamento;

    @ManyToOne
    private QuizPergunta quizPerguntaAtual;

    private LocalDateTime quizPerguntaIniciadaEm;

    private String quizSessaoId;

    @OneToMany(mappedBy = "sala")
    private List<Duvida> duvidas = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "sala_aluno",
        joinColumns = @JoinColumn(name = "sala_id"),
        inverseJoinColumns = @JoinColumn(name = "aluno_id"))
    private List<Aluno> alunos = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "sala_material",
        joinColumns = @JoinColumn(name = "sala_id"),
        inverseJoinColumns = @JoinColumn(name = "material_id"))
    private List<Material> materiais = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "sala_material_fixado",
        joinColumns = @JoinColumn(name = "sala_id"),
        inverseJoinColumns = @JoinColumn(name = "material_id"))
    private List<Material> materiaisFixados = new ArrayList<>();

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

    public Integer getCronometroSegundos() {
        return cronometroSegundos;
    }

    public void setCronometroSegundos(Integer cronometroSegundos) {
        this.cronometroSegundos = cronometroSegundos;
    }

    public LocalDateTime getCronometroIniciadoEm() {
        return cronometroIniciadoEm;
    }

    public void setCronometroIniciadoEm(LocalDateTime cronometroIniciadoEm) {
        this.cronometroIniciadoEm = cronometroIniciadoEm;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public Quiz getQuizEmAndamento() {
        return quizEmAndamento;
    }

    public void setQuizEmAndamento(Quiz quizEmAndamento) {
        this.quizEmAndamento = quizEmAndamento;
    }

    public QuizPergunta getQuizPerguntaAtual() {
        return quizPerguntaAtual;
    }

    public void setQuizPerguntaAtual(QuizPergunta quizPerguntaAtual) {
        this.quizPerguntaAtual = quizPerguntaAtual;
    }

    public LocalDateTime getQuizPerguntaIniciadaEm() {
        return quizPerguntaIniciadaEm;
    }

    public void setQuizPerguntaIniciadaEm(LocalDateTime quizPerguntaIniciadaEm) {
        this.quizPerguntaIniciadaEm = quizPerguntaIniciadaEm;
    }

    public String getQuizSessaoId() {
        return quizSessaoId;
    }

    public void setQuizSessaoId(String quizSessaoId) {
        this.quizSessaoId = quizSessaoId;
    }

    public List<Duvida> getDuvidas() {
        return duvidas;
    }

    public void setDuvidas(List<Duvida> duvidas) {
        this.duvidas = duvidas;
    }

    public List<Aluno> getAlunos() {
        return alunos;
    }

    public void setAlunos(List<Aluno> alunos) {
        this.alunos = alunos;
    }

    public List<Material> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<Material> materiais) {
        this.materiais = materiais;
    }

    public List<Material> getMateriaisFixados() {
        return materiaisFixados;
    }

    public void setMateriaisFixados(List<Material> materiaisFixados) {
        this.materiaisFixados = materiaisFixados;
    }
}
