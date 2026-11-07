package edu.senacsp.astrochimp;

import edu.senacsp.astrochimp.entity.Professor;
import edu.senacsp.astrochimp.repository.MaterialRepository;
import edu.senacsp.astrochimp.repository.SalaRepository;
import edu.senacsp.astrochimp.service.AcessoService;
import edu.senacsp.astrochimp.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Confere o limite no service sem iniciar servidor nem conectar ao banco.
class MaterialServiceTest {
    // O JUnit fornece uma pasta vazia exclusiva e a remove ao terminar o teste.
    @TempDir
    Path pasta;

    @Test
    void arquivoAcimaDe10MiBERecusadoAntesDeGravarMesmoSemServidorHttp() throws Exception {
        MaterialRepository materiais = mock(MaterialRepository.class);
        SalaRepository salas = mock(SalaRepository.class);
        AcessoService acesso = mock(AcessoService.class);
        when(acesso.professor(1L)).thenReturn(new Professor());
        MultipartFile arquivo = mock(MultipartFile.class);
        // O tamanho é simulado para conferir a regra sem reservar um arquivo grande na memória.
        when(arquivo.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        MaterialService servico = new MaterialService(materiais, salas, acesso, pasta.toString());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.enviarArquivo(1L, "Aula", arquivo));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, erro.getStatusCode());
        verifyNoInteractions(materiais, salas);
        verify(arquivo, never()).transferTo(any(Path.class));
        try (var arquivos = Files.list(pasta)) {
            assertEquals(0, arquivos.count());
        }
    }
}
