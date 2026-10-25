package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.MaterialDto;
import edu.senacsp.astrochimp.service.MaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/api/professores/{professorId}/materiais")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDto criar(
            @PathVariable Long professorId,
            @RequestBody MaterialDto pedido) {
        return materialService.criar(professorId, pedido);
    }

    @PostMapping(value = "/api/professores/{professorId}/materiais/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDto enviarArquivo(
            @PathVariable Long professorId,
            @RequestParam String titulo,
            @RequestParam MultipartFile arquivo) {
        return materialService.enviarArquivo(professorId, titulo, arquivo);
    }

    @GetMapping("/api/professores/{professorId}/materiais")
    public List<MaterialDto> listarProfessor(@PathVariable Long professorId) {
        return materialService.listarProfessor(professorId);
    }

    @PostMapping("/api/salas/{salaId}/materiais/{materialId}")
    public MaterialDto publicar(@PathVariable Long salaId, @PathVariable Long materialId) {
        return materialService.publicarNaSala(salaId, materialId);
    }

    @PostMapping(value = "/api/salas/{salaId}/materiais/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDto enviarParaSala(
            @PathVariable Long salaId,
            @RequestParam String titulo,
            @RequestParam MultipartFile arquivo) {
        return materialService.enviarArquivoParaSala(salaId, titulo, arquivo);
    }

    @GetMapping("/api/salas/{salaId}/materiais")
    public List<MaterialDto> listarSala(
            @PathVariable Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return materialService.listarSala(salaId, tokenAluno);
    }

    @PatchMapping("/api/salas/{salaId}/materiais/{materialId}/fixacao")
    public MaterialDto fixar(
            @PathVariable Long salaId,
            @PathVariable Long materialId,
            @RequestBody MaterialDto pedido) {
        return materialService.fixar(salaId, materialId, pedido);
    }

    @PatchMapping("/api/professores/{professorId}/materiais/{materialId}/publicacao")
    public MaterialDto tornarPublico(
            @PathVariable Long professorId,
            @PathVariable Long materialId,
            @RequestBody MaterialDto pedido) {
        return materialService.tornarPublico(professorId, materialId, pedido);
    }

    @GetMapping("/api/materiais/{materialId}/arquivo")
    public ResponseEntity<Resource> baixar(
            @PathVariable Long materialId,
            @RequestParam(required = false) Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        MaterialService.ArquivoDownload resultado = materialService.baixar(materialId, salaId, tokenAluno);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultado.getNomeArquivo() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resultado.getArquivo());
    }
}
