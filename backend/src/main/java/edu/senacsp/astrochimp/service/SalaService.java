package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.CriarSalaDto;
import edu.senacsp.astrochimp.dto.CronometroDto;
import edu.senacsp.astrochimp.dto.EntradaSalaDto;
import edu.senacsp.astrochimp.dto.EntrarSalaDto;
import edu.senacsp.astrochimp.dto.IniciarCronometroDto;
import edu.senacsp.astrochimp.dto.SalaDto;
import edu.senacsp.astrochimp.entity.Aluno;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.AlunoRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class SalaService {
    private final SalaRepository salas;
    private final AlunoRepository alunos;
    private final AcessoService acesso;

    public SalaService(SalaRepository salas, AlunoRepository alunos, AcessoService acesso) {
        this.salas = salas;
        this.alunos = alunos;
        this.acesso = acesso;
    }

    @Transactional
    public SalaDto criar(CriarSalaDto pedido) {
        if (pedido == null || pedido.getProfessorId() == null || pedido.getNomeAula() == null || pedido.getNomeAula().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Professor e nome da aula são obrigatórios");
        }
        Professor professor = acesso.professor(pedido.getProfessorId());
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (salas.findByCodigo(codigo).isPresent());
        Sala sala = new Sala();
        sala.setCodigo(codigo);
        sala.setNomeAula(pedido.getNomeAula().trim());
        sala.setStatus("ativa");
        sala.setCriadaEm(LocalDateTime.now());
        sala.setExpiraEm(sala.getCriadaEm().plusHours(4));
        sala.setProfessor(professor);
        return resposta(salas.save(sala));
    }

    @Transactional(readOnly = true)
    public SalaDto buscar(Long id, String tokenAluno) {
        Sala sala = acesso.sala(id);
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            acesso.alunoDaSala(id, tokenAluno);
        } else {
            acesso.salaDoProfessor(id);
        }
        return resposta(sala);
    }

    @Transactional
    public EntradaSalaDto entrar(EntrarSalaDto pedido) {
        if (pedido == null || pedido.getCodigo() == null || pedido.getNomeAluno() == null || pedido.getNomeAluno().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código e nome do aluno são obrigatórios");
        }
        Sala sala = salas.findByCodigo(pedido.getCodigo().trim().toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de sala não encontrado"));
        acesso.exigirSalaAtiva(sala);
        Aluno aluno = new Aluno();
        aluno.setNome(pedido.getNomeAluno().trim());
        aluno.setToken(UUID.randomUUID().toString());
        alunos.save(aluno);
        if (sala.getAlunos() == null) sala.setAlunos(new ArrayList<>());
        sala.getAlunos().add(aluno);
        salas.save(sala);
        return new EntradaSalaDto(resposta(sala), aluno.getId(), aluno.getToken());
    }

    @Transactional
    public SalaDto encerrar(Long id) {
        Sala sala = acesso.salaDoProfessor(id);
        sala.setStatus("encerrada");
        sala.setCronometroIniciadoEm(null);
        sala.setQuizEmAndamento(null);
        sala.setQuizPerguntaAtual(null);
        sala.setQuizPerguntaIniciadaEm(null);
        sala.setQuizSessaoId(null);
        return resposta(salas.save(sala));
    }

    @Transactional
    public CronometroDto iniciarCronometro(Long id, IniciarCronometroDto pedido) {
        Sala sala = acesso.salaDoProfessor(id);
        acesso.exigirSalaAtiva(sala);
        if (pedido == null || pedido.getSegundos() == null || pedido.getSegundos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duração deve ser maior que zero");
        }
        sala.setCronometroSegundos(pedido.getSegundos());
        sala.setCronometroIniciadoEm(LocalDateTime.now());
        salas.save(sala);
        return cronometro(sala);
    }

    @Transactional(readOnly = true)
    public CronometroDto consultarCronometro(Long id, String tokenAluno) {
        Sala sala = acesso.sala(id);
        if (tokenAluno != null && !tokenAluno.isBlank()) acesso.alunoDaSala(id, tokenAluno);
        else acesso.salaDoProfessor(id);
        return cronometro(sala);
    }

    @Transactional
    public CronometroDto pararCronometro(Long id) {
        Sala sala = acesso.salaDoProfessor(id);
        sala.setCronometroIniciadoEm(null);
        sala.setCronometroSegundos(null);
        salas.save(sala);
        return cronometro(sala);
    }

    private CronometroDto cronometro(Sala sala) {
        if (sala.getCronometroIniciadoEm() == null || sala.getCronometroSegundos() == null) {
            return new CronometroDto(null, 0, false);
        }
        long decorrido = Duration.between(sala.getCronometroIniciadoEm(), LocalDateTime.now()).getSeconds();
        int restante = (int) Math.max(0, sala.getCronometroSegundos() - decorrido);
        return new CronometroDto(sala.getCronometroSegundos(), restante, restante > 0);
    }

    private SalaDto resposta(Sala sala) {
        return new SalaDto(sala.getId(), sala.getCodigo(), sala.getNomeAula(), sala.getStatus(),
            sala.getProfessor().getNome(), sala.getCriadaEm(), sala.getExpiraEm());
    }
}
