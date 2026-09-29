package edu.senacsp.astrochimp.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
// Representa uma sala de aula virtual vinculada a um professor.
// Nela acontecem interações como dúvidas, materiais, alunos e quizzes em andamento.
public class Sala {

    // Identificador único da sala.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código único usado para acessar a sala.
    @Column(nullable = false, unique = true)
    private String codigo;

    // Nome da aula ou tema da sala.
    private String nomeAula;

    // Status atual da sala, como ativa, encerrada ou pausada.
    @Column(nullable = false)
    private String status;

    // Data de criação da sala.
    private LocalDateTime criadaEm;

    // Data de expiração da sala, quando aplicável.
    private LocalDateTime expiraEm;

    // Tempo do cronômetro da sala em segundos.
    private Integer cronometroSegundos;

    // Momento em que o cronômetro foi iniciado.
    private LocalDateTime cronometroIniciadoEm;

    // Professor responsável pela sala.
    @ManyToOne
    private Professor professor;

    // Quiz que está sendo executado no momento na sala.
    @ManyToOne
    private Quiz quizEmAndamento;

    // Pergunta atual do quiz em andamento.
    @ManyToOne
    private QuizPergunta quizPerguntaAtual;

    // Momento em que a pergunta atual iniciou.
    private LocalDateTime quizPerguntaIniciadaEm;

    // Identificador da sessão do quiz em execução.
    private String quizSessaoId;

    // Dúvidas registradas na sala.
    @OneToMany(mappedBy = "sala")
    private List<Duvida> duvidas;

    // Alunos vinculados à sala.
    @ManyToMany
    @JoinTable(
        name = "sala_aluno",
        joinColumns = @JoinColumn(name = "sala_id"),
        inverseJoinColumns = @JoinColumn(name = "aluno_id"))
    private List<Aluno> alunos;

    // Materiais disponíveis na sala.
    @ManyToMany
    @JoinTable(
        name = "sala_material",
        joinColumns = @JoinColumn(name = "sala_id"),
        inverseJoinColumns = @JoinColumn(name = "material_id"))
    private List<Material> materiais;

    // Materiais fixados em destaque para os alunos.
    @ManyToMany
    @JoinTable(
        name = "sala_material_fixado",
        joinColumns = @JoinColumn(name = "sala_id"),
        inverseJoinColumns = @JoinColumn(name = "material_id"))
    private List<Material> materiaisFixados;

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
