package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.CriarMaterialDto;
import edu.senacsp.astrochimp.dto.FixarMaterialDto;
import edu.senacsp.astrochimp.dto.MaterialDto;
import edu.senacsp.astrochimp.dto.PublicarMaterialDto;
import edu.senacsp.astrochimp.service.MaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


// basicamente oq eu já expliquei no DuvidaController, 
// aqui é a mesma coisa, só que para materiais
@RestController
public class MaterialController {
    private final MaterialService materiais;

    public MaterialController(MaterialService materiais) {
        this.materiais = materiais;
    }


    // observa-se que a maioria dos metodos estao nos retornando um ResponseEntity,
    // isso é pq precisamos montar a resposta HTTP completa, com status, cabeçalhos e 
    // corpo da resposta
    // perceba a semelhança de todos com status, dentro do corpo recebendo um 
    // HTPStatus.CREATED, e chamando o serviço de materiais para criar o material


    @PostMapping("/api/professores/{professorId}/materiais")
    public ResponseEntity<MaterialDto> criar(
            @PathVariable Long professorId,
            @RequestBody CriarMaterialDto pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materiais.criar(professorId, pedido));
    }

    @PostMapping(value = "/api/professores/{professorId}/materiais/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialDto> enviarArquivo(
            @PathVariable Long professorId,
            @RequestParam String titulo,
            @RequestParam MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materiais.enviarArquivo(professorId, titulo, arquivo));
    }

    @GetMapping("/api/professores/{professorId}/materiais")
    public List<MaterialDto> listarProfessor(@PathVariable Long professorId) {
        return materiais.listarProfessor(professorId);
    }

    @PostMapping("/api/salas/{salaId}/materiais/{materialId}")
    public MaterialDto publicar(@PathVariable Long salaId, @PathVariable Long materialId) {
        return materiais.publicarNaSala(salaId, materialId);
    }


    // Dentro do parametro tivemos que adicionar essa sugestao do VSCODE para rodar corretamente
    @PostMapping(value = "/api/salas/{salaId}/materiais/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialDto> enviarParaSala(
            @PathVariable Long salaId,
            @RequestParam String titulo,
            @RequestParam MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materiais.enviarArquivoParaSala(salaId, titulo, arquivo));
    }

    @GetMapping("/api/salas/{salaId}/materiais")
    public List<MaterialDto> listarSala(
            @PathVariable Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String token) {
        return materiais.listarSala(salaId, token);
    }

    @PatchMapping("/api/salas/{salaId}/materiais/{materialId}/fixacao")
    public MaterialDto fixar(
            @PathVariable Long salaId,
            @PathVariable Long materialId,
            @RequestBody FixarMaterialDto pedido) {
        return materiais.fixar(salaId, materialId, pedido);
    }

    @PatchMapping("/api/professores/{professorId}/materiais/{materialId}/publicacao")
    public MaterialDto tornarPublico(
            @PathVariable Long professorId,
            @PathVariable Long materialId,
            @RequestBody PublicarMaterialDto pedido) {
        return materiais.tornarPublico(professorId, materialId, pedido);
    }


    // ResponseEntity padrao como todos os outros, mas nesse aqui temos algumas coisas diferentes
    // usamos MaterialService.ArquivoResponse para pegar o nome do arquivo e o arquivo em si,
    // e usamos o HttpHeaders.CONTENT_DISPOSITION para definir o nome do arquivo que será baixado
    // além de definir o tipo de conteúdo como MediaType.APPLICATION_OCTET_STREAM, que é o tipo de conteúdo para arquivos binários
    // e por fim usamos o .body(resultado.arquivo()) para colocar o arquivo no corpo da resposta

    // Confesso que tivemos esse método como uma indicação, já mostra-se pela dificuldade desse método
    @GetMapping("/api/materiais/{materialId}/arquivo")
    public ResponseEntity<Resource> baixar(
            @PathVariable Long materialId,
            @RequestParam(required = false) Long salaId,
            @RequestHeader(value = "X-Aluno-Token", required = false) String token) {
        MaterialService.ArquivoResponse resultado = materiais.baixar(materialId, salaId, token);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultado.nome() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resultado.arquivo());
    }
}
