package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.CadastrarProfessorDto;
import edu.senacsp.astrochimp.dto.MaterialDto;
import edu.senacsp.astrochimp.dto.PerfilProfessorDto;
import edu.senacsp.astrochimp.dto.PerfilPublicoProfessorDto;
import edu.senacsp.astrochimp.entity.Material;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.repository.MaterialRepository;
import edu.senacsp.astrochimp.repository.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProfessorService {
    private final ProfessorRepository professorRepository;
    private final MaterialRepository materialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcessoService acessoService;

    public ProfessorService(
            ProfessorRepository professorRepository,
            MaterialRepository materialRepository,
            PasswordEncoder passwordEncoder,
            AcessoService acessoService) {
        this.professorRepository = professorRepository;
        this.materialRepository = materialRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
    }

    @Transactional
    public PerfilProfessorDto cadastrar(CadastrarProfessorDto pedido) {
        if (pedido == null || vazio(pedido.getNome()) || vazio(pedido.getEmail()) || vazio(pedido.getSenha())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, e-mail e senha são obrigatórios");
        }
        String email = pedido.getEmail().trim().toLowerCase();
        if (professorRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        String slug = slugNovo(pedido.getSlugPerfilPublico());
        Professor professor = new Professor();
        professor.setNome(pedido.getNome().trim());
        professor.setEmail(email);
        professor.setSenha(passwordEncoder.encode(pedido.getSenha()));
        professor.setSlugPerfilPublico(slug);
        return resposta(professorRepository.save(professor));
    }

    public PerfilProfessorDto buscar(Long professorId) {
        return resposta(acessoService.professor(professorId));
    }

    @Transactional
    public PerfilProfessorDto atualizar(Long professorId, PerfilProfessorDto pedido) {
        Professor professor = acessoService.professor(professorId);
        if (pedido == null || vazio(pedido.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome obrigatório");
        }
        professor.setNome(pedido.getNome().trim());
        professor.setDescricao(pedido.getDescricao());
        professor.setFotoUrl(pedido.getFotoUrl());
        if (!vazio(pedido.getSlugPerfilPublico()) && !pedido.getSlugPerfilPublico().equals(professor.getSlugPerfilPublico())) {
            professor.setSlugPerfilPublico(slugNovo(pedido.getSlugPerfilPublico()));
        }
        return resposta(professorRepository.save(professor));
    }

    @Transactional(readOnly = true)
    public PerfilPublicoProfessorDto publico(String slug) {
        Professor professor = professorRepository.findBySlugPerfilPublico(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));
        List<MaterialDto> materiais = new ArrayList<>();
        for (Material material : materialRepository.findByProfessorIdAndPublicoTrue(professor.getId())) {
            String conteudo = "arquivo".equals(material.getTipo()) ?
                "/api/materiais/" + material.getId() + "/arquivo" : material.getConteudoOuUrl();
            materiais.add(new MaterialDto(material.getId(), material.getTitulo(), material.getTipo(),
                conteudo, material.getDetalhe(), true, false, material.getCriadoEm()));
        }
        return new PerfilPublicoProfessorDto(professor.getId(), professor.getNome(), professor.getDescricao(),
            professor.getFotoUrl(), professor.getSlugPerfilPublico(), materiais);
    }

    private PerfilProfessorDto resposta(Professor professor) {
        return new PerfilProfessorDto(professor.getId(), professor.getNome(), professor.getEmail(),
            professor.getDescricao(), professor.getFotoUrl(), professor.getSlugPerfilPublico());
    }

    private String slugNovo(String slugInformado) {
        String slug = vazio(slugInformado) ? "professor-" + UUID.randomUUID().toString().substring(0, 8) : slugInformado.trim().toLowerCase();
        if (!slug.matches("[a-z0-9-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil público aceita letras, números e hífen");
        }
        if (professorRepository.findBySlugPerfilPublico(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Endereço do perfil já utilizado");
        }
        return slug;
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
