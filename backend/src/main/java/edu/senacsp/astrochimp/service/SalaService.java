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
import java.util.UUID;

@Service
public class SalaService {
    private final SalaRepository salaRepository;
    private final AlunoRepository alunoRepository;
    private final AcessoService acessoService;

    public SalaService(SalaRepository salaRepository, AlunoRepository alunoRepository, AcessoService acessoService) {
        this.salaRepository = salaRepository;
        this.alunoRepository = alunoRepository;
        this.acessoService = acessoService;
    }

    @Transactional
    public SalaDto criar(CriarSalaDto pedido) {
        if (pedido == null || pedido.getProfessorId() == null || pedido.getNomeAula() == null || pedido.getNomeAula().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Professor e nome da aula são obrigatórios");
        }
        Professor professor = acessoService.professor(pedido.getProfessorId());
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (salaRepository.findByCodigo(codigo).isPresent());
        Sala sala = new Sala();
        sala.setCodigo(codigo);
        sala.setNomeAula(pedido.getNomeAula().trim());
        sala.setStatus("ativa");
        sala.setCriadaEm(LocalDateTime.now());
        sala.setExpiraEm(sala.getCriadaEm().plusHours(4));
        sala.setProfessor(professor);
        return resposta(salaRepository.save(sala));
    }

    @Transactional(readOnly = true)
    public SalaDto buscar(Long salaId, String tokenAluno) {
        Sala sala = acessoService.sala(salaId);
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            acessoService.alunoDaSala(salaId, tokenAluno);
        } else {
            acessoService.salaDoProfessor(salaId);
        }
        return resposta(sala);
    }

    @Transactional
    public EntradaSalaDto entrar(EntrarSalaDto pedido) {
        if (pedido == null || pedido.getCodigo() == null || pedido.getNomeAluno() == null || pedido.getNomeAluno().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código e nome do aluno são obrigatórios");
        }
        Sala sala = salaRepository.findByCodigo(pedido.getCodigo().trim().toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de sala não encontrado"));
        acessoService.exigirSalaAtiva(sala);
        Aluno aluno = new Aluno();
        aluno.setNome(pedido.getNomeAluno().trim());
        aluno.setToken(UUID.randomUUID().toString());
        alunoRepository.save(aluno);
        sala.getAlunos().add(aluno);
        salaRepository.save(sala);
        return new EntradaSalaDto(resposta(sala), aluno.getId(), aluno.getToken());
    }

    @Transactional
    public SalaDto encerrar(Long salaId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        sala.setStatus("encerrada");
        sala.setCronometroIniciadoEm(null);
        sala.setCronometroSegundos(null);
        sala.setQuizEmAndamento(null);
        sala.setQuizPerguntaAtual(null);
        sala.setQuizPerguntaIniciadaEm(null);
        sala.setQuizSessaoId(null);
        return resposta(salaRepository.save(sala));
    }

    @Transactional
    public CronometroDto iniciarCronometro(Long salaId, IniciarCronometroDto pedido) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        if (pedido == null || pedido.getSegundos() == null || pedido.getSegundos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duração deve ser maior que zero");
        }
        sala.setCronometroSegundos(pedido.getSegundos());
        sala.setCronometroIniciadoEm(LocalDateTime.now());
        salaRepository.save(sala);
        return cronometro(sala);
    }

    @Transactional(readOnly = true)
    public CronometroDto consultarCronometro(Long salaId, String tokenAluno) {
        Sala sala = acessoService.sala(salaId);
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            acessoService.alunoDaSala(salaId, tokenAluno);
        } else {
            acessoService.salaDoProfessor(salaId);
        }
        return cronometro(sala);
    }

    @Transactional
    public CronometroDto pararCronometro(Long salaId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        sala.setCronometroIniciadoEm(null);
        sala.setCronometroSegundos(null);
        salaRepository.save(sala);
        return cronometro(sala);
    }

    private CronometroDto cronometro(Sala sala) {
        if (sala.getCronometroIniciadoEm() == null || sala.getCronometroSegundos() == null) {
            return new CronometroDto(null, 0, false);
        }
        long segundosDecorridos = Duration.between(sala.getCronometroIniciadoEm(), LocalDateTime.now()).getSeconds();
        int segundosRestantes = (int) Math.max(0, sala.getCronometroSegundos() - segundosDecorridos);
        return new CronometroDto(sala.getCronometroSegundos(), segundosRestantes, segundosRestantes > 0);
    }

    private SalaDto resposta(Sala sala) {
        return new SalaDto(sala.getId(), sala.getCodigo(), sala.getNomeAula(), sala.getStatus(),
            sala.getProfessor().getNome(), sala.getCriadaEm(), sala.getExpiraEm());
    }
}
