package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.AlterarEstadoDuvidaDto;
import edu.senacsp.astrochimp.dto.CriarDuvidaDto;
import edu.senacsp.astrochimp.dto.DuvidaDto;
import edu.senacsp.astrochimp.dto.FixarDuvidaDto;
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
import java.util.Comparator;
import java.util.List;

@Service
public class DuvidaService {
    private final DuvidaRepository duvidas;
    private final AcessoService acesso;

    public DuvidaService(DuvidaRepository duvidas, AcessoService acesso) {
        this.duvidas = duvidas;
        this.acesso = acesso;
    }

    @Transactional
    public DuvidaDto criar(Long salaId, String token, CriarDuvidaDto pedido) {
        Sala sala = acesso.sala(salaId);
        acesso.exigirSalaAtiva(sala);
        Aluno aluno = acesso.alunoDaSala(salaId, token);
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
        duvida.setVotos(new ArrayList<>());
        return resposta(duvidas.save(duvida), false);
    }

    @Transactional(readOnly = true)
    public List<DuvidaDto> listar(Long salaId, String token, boolean paraProfessor) {
        if (paraProfessor) acesso.salaDoProfessor(salaId);
        else acesso.alunoDaSala(salaId, token);
        List<Duvida> lista = new ArrayList<>(duvidas.findBySalaId(salaId));
        lista.sort(Comparator.comparing(Duvida::isFixada).reversed()
            .thenComparing(Comparator.comparingInt((Duvida d) -> d.getVotos().size()).reversed())
            .thenComparing(Duvida::getCriadaEm));
        List<DuvidaDto> respostas = new ArrayList<>();
        for (Duvida duvida : lista) respostas.add(resposta(duvida, paraProfessor));
        return respostas;
    }

    @Transactional
    public DuvidaDto votar(Long id, String token) {
        Duvida duvida = buscar(id);
        acesso.exigirSalaAtiva(duvida.getSala());
        Aluno aluno = acesso.alunoDaSala(duvida.getSala().getId(), token);
        if (duvida.getVotos() == null) duvida.setVotos(new ArrayList<>());
        for (Aluno votante : duvida.getVotos()) {
            if (votante.getId().equals(aluno.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Aluno já votou nesta dúvida");
            }
        }
        duvida.getVotos().add(aluno);
        return resposta(duvidas.save(duvida), false);
    }

    @Transactional
    public DuvidaDto mudarEstado(Long id, AlterarEstadoDuvidaDto pedido) {
        Duvida duvida = buscar(id);
        acesso.salaDoProfessor(duvida.getSala().getId());
        if (pedido == null || !("pendente".equals(pedido.getEstado()) || "respondida".equals(pedido.getEstado())
            || "arquivada".equals(pedido.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        duvida.setEstado(pedido.getEstado());
        return resposta(duvidas.save(duvida), true);
    }

    @Transactional
    public DuvidaDto fixar(Long id, FixarDuvidaDto pedido) {
        Duvida duvida = buscar(id);
        acesso.salaDoProfessor(duvida.getSala().getId());
        if (pedido == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe fixada");
        duvida.setFixada(pedido.isFixada());
        return resposta(duvidas.save(duvida), true);
    }

    private Duvida buscar(Long id) {
        return duvidas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dúvida não encontrada"));
    }

    private DuvidaDto resposta(Duvida duvida, boolean paraProfessor) {
        boolean mostrarAutor = paraProfessor || !duvida.isAnonima();
        return new DuvidaDto(duvida.getId(), duvida.getTexto(), duvida.isAnonima(),
            mostrarAutor ? duvida.getAutor().getId() : null,
            mostrarAutor ? duvida.getAutor().getNome() : null,
            duvida.getEstado(), duvida.isFixada(),
            duvida.getVotos() == null ? 0 : duvida.getVotos().size(), duvida.getCriadaEm());
    }
}
