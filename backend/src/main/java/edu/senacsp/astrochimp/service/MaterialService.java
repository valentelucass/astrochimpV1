package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.CriarMaterialDto;
import edu.senacsp.astrochimp.dto.FixarMaterialDto;
import edu.senacsp.astrochimp.dto.MaterialDto;
import edu.senacsp.astrochimp.dto.PublicarMaterialDto;
import edu.senacsp.astrochimp.entity.Material;
import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.entity.Sala;
import edu.senacsp.astrochimp.repository.MaterialRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialService {
    private final MaterialRepository materiais;
    private final SalaRepository salas;
    private final AcessoService acesso;
    private final Path pastaArquivos;

    public MaterialService(MaterialRepository materiais, SalaRepository salas, AcessoService acesso,
                           @Value("${astrochimp.upload-dir:uploads}") String pasta) {
        this.materiais = materiais;
        this.salas = salas;
        this.acesso = acesso;
        this.pastaArquivos = Path.of(pasta).toAbsolutePath().normalize();
    }

    @Transactional
    public MaterialDto criar(Long professorId, CriarMaterialDto pedido) {
        Professor professor = acesso.professor(professorId);
        if (pedido == null || vazio(pedido.getTitulo()) || vazio(pedido.getConteudoOuUrl()) ||
            !("link".equals(pedido.getTipo()) || "codigo".equals(pedido.getTipo()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe título, tipo link/codigo e conteúdo");
        }
        Material material = new Material();
        material.setTitulo(pedido.getTitulo().trim());
        material.setTipo(pedido.getTipo());
        material.setConteudoOuUrl(pedido.getConteudoOuUrl());
        material.setDetalhe(pedido.getDetalhe());
        material.setProfessor(professor);
        material.setCriadoEm(LocalDateTime.now());
        return resposta(materiais.save(material), false);
    }

    @Transactional
    public MaterialDto enviarArquivo(Long professorId, String titulo, MultipartFile arquivo) {
        Professor professor = acesso.professor(professorId);
        if (vazio(titulo) || arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título e arquivo são obrigatórios");
        }
        String original = arquivo.getOriginalFilename() == null ? "documento" : arquivo.getOriginalFilename();
        String nomeSeguro = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (nomeSeguro.length() > 80) nomeSeguro = nomeSeguro.substring(nomeSeguro.length() - 80);
        Path destino = pastaArquivos.resolve(UUID.randomUUID() + "_" + nomeSeguro).normalize();
        try {
            Files.createDirectories(pastaArquivos);
            arquivo.transferTo(destino);
        } catch (IOException erro) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao guardar arquivo", erro);
        }
        Material material = new Material();
        material.setTitulo(titulo.trim());
        material.setTipo("arquivo");
        material.setConteudoOuUrl(destino.toString());
        material.setDetalhe(nomeSeguro);
        material.setProfessor(professor);
        material.setCriadoEm(LocalDateTime.now());
        return resposta(materiais.save(material), false);
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listarProfessor(Long professorId) {
        acesso.professor(professorId);
        List<MaterialDto> lista = new ArrayList<>();
        for (Material material : materiais.findByProfessorId(professorId)) lista.add(resposta(material, false));
        return lista;
    }

    @Transactional
    public MaterialDto publicarNaSala(Long salaId, Long materialId) {
        Sala sala = acesso.salaDoProfessor(salaId);
        acesso.exigirSalaAtiva(sala);
        Material material = buscar(materialId);
        if (!material.getProfessor().getId().equals(sala.getProfessor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Material pertence a outro professor");
        }
        if (sala.getMateriais() == null) sala.setMateriais(new ArrayList<>());
        if (sala.getMateriais().stream().anyMatch(m -> m.getId().equals(materialId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material já publicado nesta sala");
        }
        sala.getMateriais().add(material);
        salas.save(sala);
        return resposta(material, false);
    }

    @Transactional
    public MaterialDto enviarArquivoParaSala(Long salaId, String titulo, MultipartFile arquivo) {
        Sala sala = acesso.salaDoProfessor(salaId);
        acesso.exigirSalaAtiva(sala);
        MaterialDto material = enviarArquivo(sala.getProfessor().getId(), titulo, arquivo);
        return publicarNaSala(salaId, material.getId());
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listarSala(Long salaId, String tokenAluno) {
        Sala sala = acesso.sala(salaId);
        if (tokenAluno != null && !tokenAluno.isBlank()) acesso.alunoDaSala(salaId, tokenAluno);
        else acesso.salaDoProfessor(salaId);
        List<MaterialDto> lista = new ArrayList<>();
        if (sala.getMateriais() != null) {
            for (Material material : sala.getMateriais()) {
                if (estaFixado(sala, material.getId())) lista.add(resposta(material, true));
            }
            for (Material material : sala.getMateriais()) {
                if (!estaFixado(sala, material.getId())) lista.add(resposta(material, false));
            }
        }
        return lista;
    }

    @Transactional
    public MaterialDto fixar(Long salaId, Long materialId, FixarMaterialDto pedido) {
        Sala sala = acesso.salaDoProfessor(salaId);
        if (pedido == null || sala.getMateriais() == null ||
            sala.getMateriais().stream().noneMatch(m -> m.getId().equals(materialId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material não publicado nesta sala");
        }
        if (sala.getMateriaisFixados() == null) sala.setMateriaisFixados(new ArrayList<>());
        if (pedido.isFixado() && !estaFixado(sala, materialId)) sala.getMateriaisFixados().add(buscar(materialId));
        if (!pedido.isFixado()) sala.getMateriaisFixados().removeIf(m -> m.getId().equals(materialId));
        salas.save(sala);
        return resposta(buscar(materialId), pedido.isFixado());
    }

    @Transactional
    public MaterialDto tornarPublico(
            Long professorId,
            Long materialId,
            PublicarMaterialDto pedido) {
        acesso.professor(professorId);
        Material material = buscar(materialId);
        if (!material.getProfessor().getId().equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Material pertence a outro professor");
        }
        if (pedido == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe publico");
        material.setPublico(pedido.isPublico());
        return resposta(materiais.save(material), false);
    }

    @Transactional(readOnly = true)
    public ArquivoResponse baixar(Long materialId, Long salaId, String tokenAluno) {
        Material material = buscar(materialId);
        if (!"arquivo".equals(material.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material não é arquivo");
        }
        if (!material.isPublico()) {
            if (salaId == null) acesso.professor(material.getProfessor().getId());
            else {
                Sala sala = acesso.sala(salaId);
                if (tokenAluno != null && !tokenAluno.isBlank()) acesso.alunoDaSala(salaId, tokenAluno);
                else acesso.salaDoProfessor(salaId);
                if (sala.getMateriais() == null || sala.getMateriais().stream().noneMatch(m -> m.getId().equals(materialId))) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo não está nesta sala");
                }
            }
        }
        Path caminho = Path.of(material.getConteudoOuUrl()).toAbsolutePath().normalize();
        if (!caminho.startsWith(pastaArquivos) || !Files.exists(caminho)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no servidor");
        }
        return new ArquivoResponse(new FileSystemResource(caminho), material.getDetalhe());
    }

    public record ArquivoResponse(Resource arquivo, String nome) {}

    private Material buscar(Long id) {
        return materiais.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));
    }

    private boolean estaFixado(Sala sala, Long materialId) {
        return sala.getMateriaisFixados() != null &&
            sala.getMateriaisFixados().stream().anyMatch(m -> m.getId().equals(materialId));
    }

    private MaterialDto resposta(Material material, boolean fixado) {
        String conteudo = "arquivo".equals(material.getTipo()) ?
            "/api/materiais/" + material.getId() + "/arquivo" : material.getConteudoOuUrl();
        return new MaterialDto(material.getId(), material.getTitulo(), material.getTipo(),
            conteudo, material.getDetalhe(), material.isPublico(), fixado, material.getCriadoEm());
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
