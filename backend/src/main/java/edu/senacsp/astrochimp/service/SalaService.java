package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.CronometroDto;
import edu.senacsp.astrochimp.dto.EntradaSalaDto;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

// O Spring cria este service para abrir salas, receber os alunos e controlar o cronômetro.
@Service
public class SalaService {
    private final SalaRepository salaRepository;
    private final AlunoRepository alunoRepository;
    private final AcessoService acessoService;

    // O Spring entrega os repositórios da aula e de seus participantes, além das regras de acesso.
    public SalaService(SalaRepository salaRepository, AlunoRepository alunoRepository, AcessoService acessoService) {
        this.salaRepository = salaRepository;
        this.alunoRepository = alunoRepository;
        this.acessoService = acessoService;
    }

    // @Transactional confirma o cadastro da sala ao concluir o método.
    // Se a operação lançar um erro como ResponseStatusException, o Spring desfaz suas mudanças no banco.
    @Transactional
    public SalaDto criar(SalaDto pedido) {
        if (pedido == null || pedido.getProfessorId() == null || pedido.getNomeAula() == null || pedido.getNomeAula().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Professor e nome da aula são obrigatórios");
        }
        Professor professor = acessoService.professor(pedido.getProfessorId());
        if (pedido.getNomeAula().trim().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da aula deve ter no máximo 255 caracteres");
        }
        // Geramos outro código se já existir uma sala com o mesmo código.
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (salaRepository.findByCodigo(codigo).isPresent());
        // Do pedido usamos só professorId e nomeAula; código, estado e datas são definidos aqui.
        Sala sala = new Sala();
        sala.setCodigo(codigo);
        sala.setNomeAula(pedido.getNomeAula().trim());
        sala.setStatus("ativa");
        sala.setCriadaEm(LocalDateTime.now());
        // Este é o prazo da aula inteira: quatro horas. O cronômetro de uma atividade é configurado à parte.
        sala.setExpiraEm(sala.getCriadaEm().plusHours(4));
        sala.setProfessor(professor);
        return resposta(salaRepository.save(sala));
    }

    // readOnly indica que a consulta da sala apenas lê os dados.
    @Transactional(readOnly = true)
    public SalaDto buscar(Long salaId, String tokenAluno) {
        // Aceita o aluno participante pelo token ou o professor dono pelo login, antes de montar a resposta.
        Sala sala = acessoService.salaComAcesso(salaId, tokenAluno);
        return resposta(sala);
    }

    @Transactional(readOnly = true)
    // O painel recebe apenas as aulas do professor conectado, da mais recente para a mais antiga.
    public List<SalaDto> listarDoProfessor(Long professorId) {
        acessoService.professor(professorId);
        List<SalaDto> salas = new ArrayList<>();
        for (Sala sala : salaRepository.findByProfessorIdOrderByCriadaEmDesc(professorId)) {
            salas.add(resposta(sala));
        }
        return salas;
    }

    @Transactional
    public EntradaSalaDto entrar(EntradaSalaDto pedido) {
        if (pedido == null || pedido.getCodigo() == null || pedido.getNomeAluno() == null || pedido.getNomeAluno().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código e nome do aluno são obrigatórios");
        }
        if (pedido.getNomeAluno().trim().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do aluno deve ter no máximo 255 caracteres");
        }
        String codigo = pedido.getCodigo().trim().toUpperCase(Locale.ROOT);
        Sala sala = salaRepository.findByCodigo(codigo).orElse(null);
        if (sala == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de sala não encontrado");
        }
        acessoService.exigirSalaAtiva(sala);
        // Do pedido usamos só código da sala e nome do aluno; geramos um novo token aqui.
        // O token identifica este aluno nas próximas requisições.
        // O cadastro do aluno e sua participação na sala são salvos na mesma transação:
        // se a participação falhar, o cadastro também é desfeito no banco.
        Aluno aluno = new Aluno();
        aluno.setNome(pedido.getNomeAluno().trim());
        aluno.setToken(UUID.randomUUID().toString());
        alunoRepository.save(aluno);
        sala.getAlunos().add(aluno);
        salaRepository.save(sala);
        return new EntradaSalaDto(resposta(sala), aluno.getId(), aluno.getToken());
    }

    @Transactional
    // Finaliza a aula e limpa seus controles em andamento; dúvidas, materiais e respostas salvas permanecem no banco.
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
    public CronometroDto iniciarCronometro(Long salaId, CronometroDto pedido) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        if (pedido == null || pedido.getSegundosTotais() == null || pedido.getSegundosTotais() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duração deve ser maior que zero");
        }
        // Do pedido usamos só o tempo total escolhido pelo professor.
        // O início é definido agora; os segundos restantes serão calculados na consulta.
        sala.setCronometroSegundos(pedido.getSegundosTotais());
        sala.setCronometroIniciadoEm(LocalDateTime.now());
        salaRepository.save(sala);
        return cronometro(sala);
    }

    @Transactional(readOnly = true)
    public CronometroDto consultarCronometro(Long salaId, String tokenAluno) {
        Sala sala = acessoService.salaComAcesso(salaId, tokenAluno);
        return cronometro(sala);
    }

    @Transactional
    // Limpa início e duração para que todos os participantes recebam a contagem como parada.
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
        // A sala expirada não mantém uma contagem ativa, mesmo sem o professor apertar encerrar.
        if (!"ativa".equals(sala.getStatus()) ||
                (sala.getExpiraEm() != null && !LocalDateTime.now().isBefore(sala.getExpiraEm()))) {
            return new CronometroDto(sala.getCronometroSegundos(), 0, false);
        }
        // segundosTotais é o tempo escolhido pelo professor, como 120 segundos.
        // iniciadoEm é o instante em que o professor iniciou esse cronômetro na sala.
        int segundosTotais = sala.getCronometroSegundos();
        LocalDateTime iniciadoEm = sala.getCronometroIniciadoEm();
        long segundosDecorridos = Duration.between(iniciadoEm, LocalDateTime.now()).getSeconds();
        if (segundosDecorridos < 0) {
            segundosDecorridos = 0;
        }
        // Se passaram 30 dos 120 segundos escolhidos, faltam 90; depois do fim, retornamos zero.
        long segundosRestantes = segundosTotais - segundosDecorridos;
        if (segundosRestantes < 0) {
            segundosRestantes = 0;
        }
        return new CronometroDto(segundosTotais, (int) segundosRestantes, segundosRestantes > 0);
    }

    private SalaDto resposta(Sala sala) {
        SalaDto dados = new SalaDto(sala.getId(), sala.getCodigo(), sala.getNomeAula(), sala.getStatus(),
            sala.getProfessor().getNome(), sala.getCriadaEm(), sala.getExpiraEm());
        // A tela recebe a quantidade de participantes, nunca a lista com seus tokens.
        dados.setTotalAlunos(sala.getAlunos().size());
        return dados;
    }
}
