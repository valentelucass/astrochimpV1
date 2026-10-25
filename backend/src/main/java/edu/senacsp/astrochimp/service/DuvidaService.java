package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.DuvidaDto;
import edu.senacsp.astrochimp.entity.Aluno;
import edu.senacsp.astrochimp.entity.Duvida;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.DuvidaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DuvidaService {
    private final DuvidaRepository duvidaRepository;
    private final AcessoService acessoService;

    public DuvidaService(DuvidaRepository duvidaRepository, AcessoService acessoService) {
        this.duvidaRepository = duvidaRepository;
        this.acessoService = acessoService;
    }

    @Transactional
    public DuvidaDto criar(Long salaId, String tokenAluno, DuvidaDto pedido) {
        Sala sala = acessoService.sala(salaId);
        acessoService.exigirSalaAtiva(sala);
        Aluno aluno = acessoService.alunoDaSala(salaId, tokenAluno);
        if (pedido == null || pedido.getTexto() == null || pedido.getTexto().isBlank() || pedido.getTexto().length() > 280) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dúvida deve ter entre 1 e 280 caracteres");
        }
        Duvida duvida = new Duvida();
        duvida.setSala(sala);
        duvida.setAutor(aluno);
        duvida.setTexto(pedido.getTexto().trim());
        duvida.setAnonima(pedido.isAnonima());
        duvida.setEstado("pendente");
        duvida.setCriadaEm(LocalDateTime.now());
        return resposta(duvidaRepository.save(duvida), false);
    }

    @Transactional(readOnly = true)
    public List<DuvidaDto> listar(Long salaId, String tokenAluno, boolean paraProfessor) {
        if (paraProfessor) {
            acessoService.salaDoProfessor(salaId);
        } else {
            acessoService.alunoDaSala(salaId, tokenAluno);
        }
        List<DuvidaDto> duvidas = new ArrayList<>();
        for (Duvida duvida : duvidaRepository.listarPorSala(salaId)) {
            duvidas.add(resposta(duvida, paraProfessor));
        }
        return duvidas;
    }

    @Transactional
    public DuvidaDto votar(Long duvidaId, String tokenAluno) {
        Duvida duvida = buscar(duvidaId);
        acessoService.exigirSalaAtiva(duvida.getSala());
        Aluno aluno = acessoService.alunoDaSala(duvida.getSala().getId(), tokenAluno);
        for (Aluno votante : duvida.getAlunosQueVotaram()) {
            if (votante.getId().equals(aluno.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Aluno já votou nesta dúvida");
            }
        }
        duvida.getAlunosQueVotaram().add(aluno);
        return resposta(duvidaRepository.save(duvida), false);
    }

    @Transactional
    public DuvidaDto mudarEstado(Long duvidaId, DuvidaDto pedido) {
        Duvida duvida = buscar(duvidaId);
        acessoService.salaDoProfessor(duvida.getSala().getId());
        if (pedido == null || !("pendente".equals(pedido.getEstado()) || "respondida".equals(pedido.getEstado())
            || "arquivada".equals(pedido.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        duvida.setEstado(pedido.getEstado());
        return resposta(duvidaRepository.save(duvida), true);
    }

    @Transactional
    public DuvidaDto fixar(Long duvidaId, DuvidaDto pedido) {
        Duvida duvida = buscar(duvidaId);
        acessoService.salaDoProfessor(duvida.getSala().getId());
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe fixada");
        }
        duvida.setFixada(pedido.isFixada());
        return resposta(duvidaRepository.save(duvida), true);
    }

    private Duvida buscar(Long duvidaId) {
        return duvidaRepository.findById(duvidaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dúvida não encontrada"));
    }

    private DuvidaDto resposta(Duvida duvida, boolean paraProfessor) {
        boolean mostrarAutor = paraProfessor || !duvida.isAnonima();
        return new DuvidaDto(duvida.getId(), duvida.getTexto(), duvida.isAnonima(),
            mostrarAutor ? duvida.getAutor().getId() : null,
            mostrarAutor ? duvida.getAutor().getNome() : null,
            duvida.getEstado(), duvida.isFixada(),
            duvida.getAlunosQueVotaram().size(), duvida.getCriadaEm());
    }
}
