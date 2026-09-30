package edu.senacsp.astrochimp.controller;

import edu.senacsp.astrochimp.dto.CriarSalaDto;
import edu.senacsp.astrochimp.dto.CronometroDto;
import edu.senacsp.astrochimp.dto.EntradaSalaDto;
import edu.senacsp.astrochimp.dto.EntrarSalaDto;
import edu.senacsp.astrochimp.dto.IniciarCronometroDto;
import edu.senacsp.astrochimp.dto.SalaDto;
import edu.senacsp.astrochimp.service.SalaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// Essa classe não tem muitas explicações, pois é basicamente a mesma coisa que as outras, só que para salas
@RestController
@RequestMapping("/api/salas")   // essa anotação define o caminho base para todos os 
                                // métodos da classe, ou seja, todos os métodos terão /api/salas no início do caminho
public class SalaController {
    private final SalaService salas;

    public SalaController(SalaService salas) {
        this.salas = salas;
    }

    @PostMapping
    public ResponseEntity<SalaDto> criar(@RequestBody CriarSalaDto pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salas.criar(pedido));
    }

    @GetMapping("/{id}")
    public SalaDto buscar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Aluno-Token", required = false) String token) {
        return salas.buscar(id, token);
    }

    @PostMapping("/entrar")
    public EntradaSalaDto entrar(@RequestBody EntrarSalaDto pedido) {
        return salas.entrar(pedido);
    }

    @PatchMapping("/{id}/encerrar")
    public SalaDto encerrar(@PathVariable Long id) {
        return salas.encerrar(id);
    }

    @PostMapping("/{id}/cronometro")
    public CronometroDto iniciarCronometro(
            @PathVariable Long id,
            @RequestBody IniciarCronometroDto pedido) {
        return salas.iniciarCronometro(id, pedido);
    }

    @GetMapping("/{id}/cronometro")
    public CronometroDto consultarCronometro(
            @PathVariable Long id,
            @RequestHeader(value = "X-Aluno-Token", required = false) String token) {
        return salas.consultarCronometro(id, token);
    }

    @DeleteMapping("/{id}/cronometro")
    public CronometroDto pararCronometro(@PathVariable Long id) {
        return salas.pararCronometro(id);
    }
}
