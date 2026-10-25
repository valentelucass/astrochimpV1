package edu.senacsp.astrochimp.service;

import edu.senacsp.astrochimp.dto.MaterialDto;
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
    private final MaterialRepository materialRepository;
    private final SalaRepository salaRepository;
    private final AcessoService acessoService;
    private final Path pastaArquivos;

    public MaterialService(MaterialRepository materialRepository, SalaRepository salaRepository, AcessoService acessoService,
                           @Value("${astrochimp.upload-dir:uploads}") String pasta) {
        this.materialRepository = materialRepository;
        this.salaRepository = salaRepository;
        this.acessoService = acessoService;
        this.pastaArquivos = Path.of(pasta).toAbsolutePath().normalize();
    }

    @Transactional
    public MaterialDto criar(Long professorId, MaterialDto pedido) {
        Professor professor = acessoService.professor(professorId);
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
        return resposta(materialRepository.save(material), false);
    }

    @Transactional
    public MaterialDto enviarArquivo(Long professorId, String titulo, MultipartFile arquivo) {
        Professor professor = acessoService.professor(professorId);
        if (vazio(titulo) || arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título e arquivo são obrigatórios");
        }
        String original = arquivo.getOriginalFilename() == null ? "documento" : arquivo.getOriginalFilename();
        String nomeSeguro = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (nomeSeguro.length() > 80) {
            nomeSeguro = nomeSeguro.substring(nomeSeguro.length() - 80);
        }
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
        return resposta(materialRepository.save(material), false);
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listarProfessor(Long professorId) {
        acessoService.professor(professorId);
        List<MaterialDto> materiais = new ArrayList<>();
        for (Material material : materialRepository.findByProfessorId(professorId)) {
            materiais.add(resposta(material, false));
        }
        return materiais;
    }

    @Transactional
    public MaterialDto publicarNaSala(Long salaId, Long materialId) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        Material material = buscar(materialId);
        if (!material.getProfessor().getId().equals(sala.getProfessor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Material pertence a outro professor");
        }
        if (contemMaterial(sala.getMateriais(), materialId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material já publicado nesta sala");
        }
        sala.getMateriais().add(material);
        salaRepository.save(sala);
        return resposta(material, false);
    }

    @Transactional
    public MaterialDto enviarArquivoParaSala(Long salaId, String titulo, MultipartFile arquivo) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        MaterialDto materialDto = enviarArquivo(sala.getProfessor().getId(), titulo, arquivo);
        return publicarNaSala(salaId, materialDto.getId());
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listarSala(Long salaId, String tokenAluno) {
        Sala sala = acessoService.sala(salaId);
        if (tokenAluno != null && !tokenAluno.isBlank()) {
            acessoService.alunoDaSala(salaId, tokenAluno);
        } else {
            acessoService.salaDoProfessor(salaId);
        }
        List<MaterialDto> fixados = new ArrayList<>();
        List<MaterialDto> naoFixados = new ArrayList<>();
        for (Material material : sala.getMateriais()) {
            boolean fixado = contemMaterial(sala.getMateriaisFixados(), material.getId());
            if (fixado) {
                fixados.add(resposta(material, true));
            } else {
                naoFixados.add(resposta(material, false));
            }
        }
        fixados.addAll(naoFixados);
        return fixados;
    }

    @Transactional
    public MaterialDto fixar(Long salaId, Long materialId, MaterialDto pedido) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        if (pedido == null || !contemMaterial(sala.getMateriais(), materialId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material não publicado nesta sala");
        }
        Material material = buscar(materialId);
        if (pedido.isFixado()) {
            if (!contemMaterial(sala.getMateriaisFixados(), materialId)) {
                sala.getMateriaisFixados().add(material);
            }
        } else {
            sala.getMateriaisFixados().remove(material);
        }
        salaRepository.save(sala);
        return resposta(material, pedido.isFixado());
    }

    @Transactional
    public MaterialDto tornarPublico(
            Long professorId,
            Long materialId,
            MaterialDto pedido) {
        acessoService.professor(professorId);
        Material material = buscar(materialId);
        if (!material.getProfessor().getId().equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Material pertence a outro professor");
        }
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe publico");
        }
        material.setPublico(pedido.isPublico());
        return resposta(materialRepository.save(material), false);
    }

    @Transactional(readOnly = true)
    public ArquivoDownload baixar(Long materialId, Long salaId, String tokenAluno) {
        Material material = buscar(materialId);
        if (!"arquivo".equals(material.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material não é arquivo");
        }
        if (!material.isPublico()) {
            if (salaId == null) {
                acessoService.professor(material.getProfessor().getId());
            } else {
                Sala sala = acessoService.sala(salaId);
                if (tokenAluno != null && !tokenAluno.isBlank()) {
                    acessoService.alunoDaSala(salaId, tokenAluno);
                } else {
                    acessoService.salaDoProfessor(salaId);
                }
                if (!contemMaterial(sala.getMateriais(), materialId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo não está nesta sala");
                }
            }
        }
        Path caminho = Path.of(material.getConteudoOuUrl()).toAbsolutePath().normalize();
        if (!caminho.startsWith(pastaArquivos) || !Files.exists(caminho)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no servidor");
        }
        return new ArquivoDownload(new FileSystemResource(caminho), material.getDetalhe());
    }

    // Guarda o arquivo e o nome que o controller usa no cabeçalho do download.
    public static class ArquivoDownload {
        private final Resource arquivo;
        private final String nomeArquivo;

        public ArquivoDownload(Resource arquivo, String nomeArquivo) {
            this.arquivo = arquivo;
            this.nomeArquivo = nomeArquivo;
        }

        public Resource getArquivo() {
            return arquivo;
        }

        public String getNomeArquivo() {
            return nomeArquivo;
        }
    }

    private Material buscar(Long materialId) {
        return materialRepository.findById(materialId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));
    }

    private boolean contemMaterial(List<Material> materiais, Long materialId) {
        for (Material material : materiais) {
            if (material.getId().equals(materialId)) {
                return true;
            }
        }
        return false;
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
