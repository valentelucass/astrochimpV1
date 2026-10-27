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

// O Spring cria este service para conferir quem pode acessar o professor e a sala.
@Service
public class AcessoService {
    private final ProfessorRepository professorRepository;
    private final SalaRepository salaRepository;
    private final AlunoRepository alunoRepository;

    // O Spring entrega os repositórios usados para conferir o professor, a sala e o token do aluno.
    public AcessoService(ProfessorRepository professorRepository, SalaRepository salaRepository, AlunoRepository alunoRepository) {
        this.professorRepository = professorRepository;
        this.salaRepository = salaRepository;
        this.alunoRepository = alunoRepository;
    }

    // Procura o perfil pedido e compara seu e-mail com o login atual, impedindo usar o id de outro professor.
    public Professor professor(Long professorId) {
        if (professorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Professor obrigatório");
        }
        Professor professor = professorRepository.findById(professorId).orElse(null);
        if (professor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado");
        }
        Authentication login = SecurityContextHolder.getContext().getAuthentication();
        if (login == null || login instanceof AnonymousAuthenticationToken || !login.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login do professor obrigatório");
        }
        if (!professor.getEmail().equals(login.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso reservado ao professor dono");
        }
        return professor;
    }

    // Localiza a sala ou devolve o motivo da recusa; a permissão é conferida pelos métodos que a utilizam.
    public Sala sala(Long salaId) {
        if (salaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sala obrigatória");
        }
        Sala sala = salaRepository.findById(salaId).orElse(null);
        if (sala == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala não encontrada");
        }
        return sala;
    }

    // Combina as duas conferências anteriores para operações que só o dono da aula pode executar.
    public Sala salaDoProfessor(Long salaId) {
        Sala sala = sala(salaId);
        professor(sala.getProfessor().getId());
        return sala;
    }

    // @Transactional mantém a consulta à sala e aos participantes na mesma transação.
    // readOnly indica que esta operação apenas lê os dados.
    @Transactional(readOnly = true)
    public Sala salaComAcesso(Long salaId, String tokenAluno) {
        Sala sala = sala(salaId);
        // O aluno entra pelo token; o professor precisa estar logado e ser o dono.
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            alunoDaSala(salaId, tokenAluno);
        } else {
            professor(sala.getProfessor().getId());
        }
        return sala;
    }

    @Transactional(readOnly = true)
    // Confere o token e procura o id do aluno na lista de participantes da sala solicitada.
    public Aluno alunoDaSala(Long salaId, String tokenAluno) {
        if (tokenAluno == null || tokenAluno.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno obrigatório");
        }
        Aluno aluno = alunoRepository.findByToken(tokenAluno).orElse(null);
        // A comparação do MySQL pode ignorar maiúsculas; o token precisa ser exatamente o recebido na entrada.
        if (aluno == null || !tokenAluno.equals(aluno.getToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno inválido");
        }
        Sala sala = sala(salaId);
        // Ter um token válido não basta: o aluno também precisa fazer parte desta sala.
        for (Aluno participante : sala.getAlunos()) {
            if (participante.getId().equals(aluno.getId())) {
                return aluno;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aluno não participa desta sala");
    }

    // Uma sala salva ainda pode ter terminado: seu estado e seu horário limite precisam permitir a ação.
    public void exigirSalaAtiva(Sala sala) {
        boolean encerrada = !"ativa".equals(sala.getStatus());
        boolean expirada = sala.getExpiraEm() != null && !LocalDateTime.now().isBefore(sala.getExpiraEm());
        if (encerrada || expirada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sala encerrada ou expirada");
        }
    }
}
