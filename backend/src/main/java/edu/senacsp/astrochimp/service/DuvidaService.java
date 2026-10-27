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

// O Spring cria este service para cuidar das dúvidas, dos votos e de quem pode ver o autor.
@Service
public class DuvidaService {
    private final DuvidaRepository duvidaRepository;
    private final AcessoService acessoService;

    // Recebe o acesso às dúvidas salvas e a conferência de quem pode participar da sala.
    public DuvidaService(DuvidaRepository duvidaRepository, AcessoService acessoService) {
        this.duvidaRepository = duvidaRepository;
        this.acessoService = acessoService;
    }

    // @Transactional confirma o cadastro da dúvida ao concluir o método.
    // Se a operação lançar um erro como ResponseStatusException, o Spring desfaz suas mudanças no banco.
    @Transactional
    public DuvidaDto criar(Long salaId, String tokenAluno, DuvidaDto pedido) {
        Sala sala = acessoService.sala(salaId);
        acessoService.exigirSalaAtiva(sala);
        Aluno aluno = acessoService.alunoDaSala(salaId, tokenAluno);
        if (pedido == null || pedido.getTexto() == null || pedido.getTexto().isBlank() || pedido.getTexto().length() > 280) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dúvida deve ter entre 1 e 280 caracteres");
        }
        // O aluno manda o texto e escolhe o anonimato. Autor, votos e estado inicial ficam por nossa conta.
        Duvida duvida = new Duvida();
        duvida.setSala(sala);
        duvida.setAutor(aluno);
        duvida.setTexto(pedido.getTexto().trim());
        duvida.setAnonima(pedido.isAnonima());
        duvida.setEstado("pendente");
        duvida.setCriadaEm(LocalDateTime.now());
        return resposta(duvidaRepository.save(duvida), false);
    }

    // readOnly indica que a consulta das dúvidas e dos autores apenas lê os dados.
    @Transactional(readOnly = true)
    public List<DuvidaDto> listar(Long salaId, String tokenAluno, boolean paraProfessor) {
        if (paraProfessor) {
            acessoService.salaDoProfessor(salaId);
        } else {
            acessoService.alunoDaSala(salaId, tokenAluno);
        }
        // ArrayList começa vazia; o for acrescenta uma resposta por dúvida, respeitando quem vai ler os autores.
        List<DuvidaDto> duvidas = new ArrayList<>();
        for (Duvida duvida : duvidaRepository.listarPorSala(salaId)) {
            duvidas.add(resposta(duvida, paraProfessor));
        }
        return duvidas;
    }

    @Transactional
    public DuvidaDto votar(Long duvidaId, String tokenAluno) {
        if (duvidaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dúvida obrigatória");
        }
        // Duas requisições esperam sua vez para conferir e salvar votos nesta dúvida.
        Duvida duvida = duvidaRepository.buscarParaAtualizar(duvidaId).orElse(null);
        if (duvida == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dúvida não encontrada");
        }
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
    // O dono da sala pode reabrir, marcar como respondida ou arquivar, usando apenas os três estados aceitos.
    public DuvidaDto mudarEstado(Long duvidaId, DuvidaDto pedido) {
        Duvida duvida = buscar(duvidaId);
        acessoService.salaDoProfessor(duvida.getSala().getId());
        acessoService.exigirSalaAtiva(duvida.getSala());
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        String estado = pedido.getEstado();
        boolean estadoValido = "pendente".equals(estado) || "respondida".equals(estado) || "arquivada".equals(estado);
        if (!estadoValido) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        duvida.setEstado(estado);
        return resposta(duvidaRepository.save(duvida), true);
    }

    @Transactional
    // Destaca ou retira o destaque da dúvida; o estado de respondida/pendente continua separado dessa opção.
    public DuvidaDto fixar(Long duvidaId, DuvidaDto pedido) {
        Duvida duvida = buscar(duvidaId);
        acessoService.salaDoProfessor(duvida.getSala().getId());
        acessoService.exigirSalaAtiva(duvida.getSala());
        if (pedido == null || pedido.isFixada() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe fixada");
        }
        duvida.setFixada(pedido.isFixada());
        return resposta(duvidaRepository.save(duvida), true);
    }

    // Compartilha a busca usada pelas alterações e devolve 404 se a pergunta solicitada não existir.
    private Duvida buscar(Long duvidaId) {
        if (duvidaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dúvida obrigatória");
        }
        Duvida duvida = duvidaRepository.findById(duvidaId).orElse(null);
        if (duvida == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dúvida não encontrada");
        }
        return duvida;
    }

    private DuvidaDto resposta(Duvida duvida, boolean paraProfessor) {
        // O professor pode ver o autor. Os alunos só veem o nome quando a dúvida não é anônima.
        Long autorId = null;
        String autorNome = null;
        if (paraProfessor || !duvida.isAnonima()) {
            autorId = duvida.getAutor().getId();
            autorNome = duvida.getAutor().getNome();
        }
        return new DuvidaDto(duvida.getId(), duvida.getTexto(), duvida.isAnonima(),
            autorId, autorNome,
            duvida.getEstado(), duvida.isFixada(),
            duvida.getAlunosQueVotaram().size(), duvida.getCriadaEm());
    }
}
