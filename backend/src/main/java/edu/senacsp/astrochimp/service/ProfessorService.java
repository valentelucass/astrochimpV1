package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.MaterialDto;
import edu.senacsp.astrochimp.dto.PerfilProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilPublicoProfessorDto;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.repository.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

// O Spring cria este service para cadastrar o professor e cuidar do seu perfil.
@Service
public class ProfessorService {
    private final ProfessorRepository professorRepository;
    private final MaterialService materialService;
    private final PasswordEncoder passwordEncoder;
    private final AcessoService acessoService;

    // Recebe as consultas do banco, a biblioteca de materiais, o codificador de senha e a conferência de acesso.
    public ProfessorService(
            ProfessorRepository professorRepository,
            MaterialService materialService,
            PasswordEncoder passwordEncoder,
            AcessoService acessoService) {
        this.professorRepository = professorRepository;
        this.materialService = materialService;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
    }

    // @Transactional confirma o cadastro do professor ao concluir o método.
    // Se a operação lançar um erro como ResponseStatusException, o Spring desfaz suas mudanças no banco.
    @Transactional
    public PerfilProfessorDto cadastrar(PerfilProfessorDto pedido) {
        if (pedido == null || vazio(pedido.getNome()) || vazio(pedido.getEmail()) || vazio(pedido.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, e-mail e senha são obrigatórios");
        }
        validarTamanho(pedido.getNome().trim(), "Nome");
        String email = pedido.getEmail().trim().toLowerCase(Locale.ROOT);
        validarTamanho(email, "E-mail");
        if (!email.matches("[^\\s@]+@[^\\s@]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail inválido");
        }
        // BCrypt aceita no máximo 72 bytes, que podem ser menos de 72 letras com acentos.
        if (pedido.getSenha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve ter no máximo 72 bytes em UTF-8");
        }
        if (professorRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        String slug = slugNovo(pedido.getSlugPerfilPublico());
        // O cadastro usa nome, e-mail, senha e endereço público; o id é gerado pelo banco.
        Professor professor = new Professor();
        professor.setNome(pedido.getNome().trim());
        professor.setEmail(email);
        // Guardamos a senha codificada, sem salvar o texto que a pessoa digitou.
        professor.setSenha(passwordEncoder.encode(pedido.getSenha()));
        professor.setSlugPerfilPublico(slug);
        return resposta(professorRepository.save(professor));
    }

    // AcessoService exige que o perfil consultado pertença ao professor conectado.
    public PerfilProfessorDto buscar(Long professorId) {
        return resposta(acessoService.professor(professorId));
    }

    // O login fornece o e-mail; encontramos seu id para usar a mesma consulta de perfil acima.
    public PerfilProfessorDto buscarPorEmail(String email) {
        Professor professor = professorRepository.findByEmail(email).orElse(null);
        if (professor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado");
        }
        return buscar(professor.getId());
    }

    @Transactional
    public PerfilProfessorDto atualizar(Long professorId, PerfilProfessorDto pedido) {
        Professor professor = acessoService.professor(professorId);
        if (pedido == null || vazio(pedido.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome obrigatório");
        }
        validarTamanho(pedido.getNome().trim(), "Nome");
        validarTamanho(pedido.getDescricao(), "Descrição");
        validarTamanho(pedido.getFotoUrl(), "Endereço da foto");
        // Atualizamos só estes campos. A senha e o e-mail não são alterados por esta rota.
        professor.setNome(pedido.getNome().trim());
        professor.setDescricao(pedido.getDescricao());
        professor.setFotoUrl(pedido.getFotoUrl());
        String slugInformado = pedido.getSlugPerfilPublico();
        if (!vazio(slugInformado)) {
            String slugNormalizado = slugInformado.trim().toLowerCase(Locale.ROOT);
            if (!slugNormalizado.equals(professor.getSlugPerfilPublico())) {
                professor.setSlugPerfilPublico(slugNovo(slugNormalizado));
            }
        }
        return resposta(professorRepository.save(professor));
    }

    // readOnly indica que a consulta do perfil e dos materiais públicos apenas lê os dados.
    @Transactional(readOnly = true)
    public PerfilPublicoProfessorDto publico(String slug) {
        if (vazio(slug)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado");
        }
        slug = slug.trim().toLowerCase(Locale.ROOT);
        Professor professor = professorRepository.findBySlugPerfilPublico(slug).orElse(null);
        if (professor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado");
        }
        List<MaterialDto> materiais = materialService.listarPublicos(professor.getId());
        return new PerfilPublicoProfessorDto(professor.getId(), professor.getNome(), professor.getDescricao(),
            professor.getFotoUrl(), professor.getSlugPerfilPublico(), materiais);
    }

    // Monta a resposta do próprio professor com seus dados de apresentação e e-mail, sem copiar a senha.
    private PerfilProfessorDto resposta(Professor professor) {
        return new PerfilProfessorDto(professor.getId(), professor.getNome(), professor.getEmail(),
            professor.getDescricao(), professor.getFotoUrl(), professor.getSlugPerfilPublico());
    }

    // Slug é o nome do endereço público. Criamos um quando o cadastro não informa e recusamos nomes já usados.
    private String slugNovo(String slugInformado) {
        String slug;
        if (vazio(slugInformado)) {
            slug = "professor-" + UUID.randomUUID().toString().substring(0, 8);
        } else {
            slug = slugInformado.trim().toLowerCase(Locale.ROOT);
        }
        validarTamanho(slug, "Endereço do perfil");
        if (!slug.matches("[a-z0-9-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil público aceita letras, números e hífen");
        }
        if (professorRepository.findBySlugPerfilPublico(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Endereço do perfil já utilizado");
        }
        return slug;
    }

    // Considera ausente tanto null quanto um texto composto apenas por espaços.
    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private void validarTamanho(String texto, String campo) {
        // Esses campos de Professor usam colunas de até 255 caracteres no MySQL.
        if (texto != null && texto.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ter no máximo 255 caracteres");
        }
    }
}
