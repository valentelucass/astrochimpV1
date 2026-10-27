package edu.senacsp.astrochimp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Guarda os dados e o andamento de cada sala no banco.
@Entity
public class Sala {

    // O id identifica esta sala no banco.
    @Id
    // O banco gera o id quando esta sala é salva pela primeira vez.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exige um código para a sala e impede duas salas com o mesmo código.
    @Column(nullable = false, unique = true)
    private String codigo;

    private String nomeAula;

    @Column(nullable = false)
    private String status;

    private LocalDateTime criadaEm;

    private LocalDateTime expiraEm;

    private Integer cronometroSegundos;

    private LocalDateTime cronometroIniciadoEm;

    // Cada sala tem um professor dono, que pode abrir várias salas.
    @ManyToOne
    private Professor professor;

    @ManyToOne
    private Quiz quizEmAndamento;

    @ManyToOne
    private QuizPergunta quizPerguntaAtual;

    private LocalDateTime quizPerguntaIniciadaEm;

    private String quizSessaoId;

    // Uma sala pode ter várias dúvidas; a ligação fica no campo sala de Duvida.
    @OneToMany(mappedBy = "sala")
    private List<Duvida> duvidas = new ArrayList<>();

    // Uma sala pode ter vários alunos, e cada aluno pode participar de várias salas.
    @ManyToMany
    // Guarda as participações na tabela sala_aluno, ligando salas e alunos.
    @JoinTable(
        name = "sala_aluno",
        // @JoinColumn: A coluna sala_id indica em qual sala o aluno está participando.
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

    // Os services usam estes get/is para ler os campos da entidade e set para preencher as mudanças antes de salvar.
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
