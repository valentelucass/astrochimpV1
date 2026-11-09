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
import java.util.Optional;

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
        // Optional representa uma busca que pode não encontrar registro; isEmpty confere essa ausência.
        Optional<Professor> resultado = professorRepository.findById(professorId);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado");
        }
        Professor professor = resultado.get();
        exigirProfessor(professor);
        return professor;
    }

    // Confere o dono já carregado, sem buscar novamente o professor pelo id.
    public void exigirProfessor(Professor professor) {
        Authentication login = SecurityContextHolder.getContext().getAuthentication();
        if (login == null || login instanceof AnonymousAuthenticationToken || !login.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login do professor obrigatório");
        }
        if (!professor.getEmail().equals(login.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso reservado ao professor dono");
        }
    }

    // Localiza a sala ou devolve o motivo da recusa; a permissão é conferida pelos métodos que a utilizam.
    public Sala sala(Long salaId) {
        if (salaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sala obrigatória");
        }
        Optional<Sala> resultado = salaRepository.findById(salaId);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala não encontrada");
        }
        return resultado.get();
    }

    // Combina as duas conferências anteriores para operações que só o dono da aula pode executar.
    public Sala salaDoProfessor(Long salaId) {
        Sala sala = sala(salaId);
        exigirProfessor(sala.getProfessor());
        return sala;
    }

    // @Transactional mantém a consulta à sala e aos participantes na mesma transação.
    // readOnly indica que esta operação apenas lê os dados.
    @Transactional(readOnly = true)
    public Sala salaComAcesso(Long salaId, String tokenAluno) {
        Sala sala = sala(salaId);
        // O aluno entra pelo token; o professor precisa estar logado e ser o dono.
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            alunoDaSala(sala, tokenAluno);
        } else {
            exigirProfessor(sala.getProfessor());
        }
        return sala;
    }

    @Transactional(readOnly = true)
    // Confere o token e procura o id do aluno na lista de participantes da sala solicitada.
    public Aluno alunoDaSala(Long salaId, String tokenAluno) {
        Aluno aluno = buscarAluno(tokenAluno);
        exigirParticipacao(sala(salaId), aluno);
        return aluno;
    }

    @Transactional(readOnly = true)
    // Usa a sala que a operação já consultou, sem repetir a busca por id.
    public Aluno alunoDaSala(Sala sala, String tokenAluno) {
        Aluno aluno = buscarAluno(tokenAluno);
        exigirParticipacao(sala, aluno);
        return aluno;
    }

    // Localiza o aluno e exige o token exatamente como foi gerado na entrada.
    private Aluno buscarAluno(String tokenAluno) {
        if (tokenAluno == null || tokenAluno.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno obrigatório");
        }
        Optional<Aluno> resultado = alunoRepository.findByToken(tokenAluno);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno inválido");
        }
        Aluno aluno = resultado.get();
        // A comparação do MySQL pode ignorar maiúsculas; o token precisa ser exatamente o recebido na entrada.
        if (!tokenAluno.equals(aluno.getToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do aluno inválido");
        }
        return aluno;
    }

    private void exigirParticipacao(Sala sala, Aluno aluno) {
        // Ter um token válido não basta: o aluno também precisa fazer parte desta sala.
        for (Aluno participante : sala.getAlunos()) {
            if (participante.getId().equals(aluno.getId())) {
                return;
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
