package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.MaterialDto;
import edu.senacsp.astrochimp.service.MaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// O Spring registra este controller para atender as rotas de materiais; os DTOs viram JSON e o download devolve o arquivo.
@RestController
public class MaterialController {
    private final MaterialService materialService;

    // O Spring passa o MaterialService criado por ele para este controller usar nas rotas.
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    // @PostMapping liga o POST /api/professores/{professorId}/materiais ao cadastro de material.
    @PostMapping("/api/professores/{professorId}/materiais")
    // Devolve 201 para avisar que o material foi criado.
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDto criar(
            // Pega professorId no endereço para saber de quem é o material.
            @PathVariable Long professorId,
            // Monta MaterialDto com os dados do material recebidos no JSON.
            @RequestBody MaterialDto pedido) {
        return materialService.criar(professorId, pedido);
    }

    @PostMapping(value = "/api/professores/{professorId}/materiais/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDto enviarArquivo(
            @PathVariable Long professorId,
            // @RequestParam lê titulo no formulário; o parâmetro arquivo recebe o arquivo enviado nesse mesmo formulário.
            @RequestParam String titulo,
            @RequestParam MultipartFile arquivo) {
        return materialService.enviarArquivo(professorId, titulo, arquivo);
    }

    // @GetMapping liga o GET /api/professores/{professorId}/materiais à consulta dos materiais do professor.
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
            // @RequestHeader lê X-Aluno-Token; sem esse cabeçalho, o service exige o login do professor dono da sala.
            @RequestHeader(value = "X-Aluno-Token", required = false) String tokenAluno) {
        return materialService.listarSala(salaId, tokenAluno);
    }

    // @PatchMapping liga esta rota à alteração do campo fixado do material na sala.
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
        // ContentDisposition monta o cabeçalho do download e escapa aspas no nome do arquivo.
        ContentDisposition cabecalhoDownload = ContentDisposition.attachment()
            .filename(resultado.getNomeArquivo()).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, cabecalhoDownload.toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resultado.getArquivo());
    }
}
