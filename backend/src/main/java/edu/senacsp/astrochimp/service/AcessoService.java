package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.entity.Aluno;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.AlunoRepository;
import edu.senacsp.astrochimp.repository.ProfessorRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AcessoService {
    private final ProfessorRepository professores;
    private final SalaRepository salas;
    private final AlunoRepository alunos;

    public AcessoService(ProfessorRepository professores, SalaRepository salas, AlunoRepository alunos) {
        this.professores = professores;
        this.salas = salas;
        this.alunos = alunos;
    }

    public Professor professor(Long id) {
        Professor professor = professores.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado"));
        Authentication login = SecurityContextHolder.getContext().getAuthentication();
        if (login == null || login instanceof AnonymousAuthenticationToken || !login.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login do professor obrigatório");
        }
        if (!professor.getEmail().equals(login.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso reservado ao professor dono");
        }
        return professor;
    }

    public Sala sala(Long id) {
        return salas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala não encontrada"));
    }

    public Sala salaDoProfessor(Long salaId) {
        Sala sala = sala(salaId);
        professor(sala.getProfessor().getId());
        return sala;
    }

    @Transactional(readOnly = true)
    public Aluno alunoDaSala(Long salaId, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno obrigatório");
        }
        Aluno aluno = alunos.findByToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno inválido"));
        Sala sala = sala(salaId);
        for (Aluno participante : sala.getAlunos()) {
            if (participante.getId().equals(aluno.getId())) {
                return aluno;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno não participa desta sala");
    }

    public void exigirSalaAtiva(Sala sala) {
        if (!"ativa".equals(sala.getStatus()) ||
            (sala.getExpiraEm() != null && LocalDateTime.now().isAfter(sala.getExpiraEm()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sala encerrada ou expirada");
        }
    }
}
