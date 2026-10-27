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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// O Spring cria este service para cadastrar, publicar e baixar os materiais das aulas.
@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final SalaRepository salaRepository;
    private final AcessoService acessoService;
    private final Path pastaArquivos;

    // Recebe os acessos ao banco e prepara o endereço completo da pasta em que os uploads serão guardados.
    public MaterialService(MaterialRepository materialRepository, SalaRepository salaRepository, AcessoService acessoService,
                           // Lê a pasta dos arquivos nas configurações; usa uploads se ela não foi informada.
                           @Value("${astrochimp.upload-dir:uploads}") String pasta) {
        this.materialRepository = materialRepository;
        this.salaRepository = salaRepository;
        this.acessoService = acessoService;
        this.pastaArquivos = Path.of(pasta).toAbsolutePath().normalize();
    }

    // @Transactional confirma o cadastro do material ao concluir o método.
    // Se a operação lançar um erro como ResponseStatusException, o Spring desfaz suas mudanças no banco.
    @Transactional
    public MaterialDto criar(Long professorId, MaterialDto pedido) {
        Professor professor = acessoService.professor(professorId);
        if (pedido == null || vazio(pedido.getTitulo()) || vazio(pedido.getConteudoOuUrl()) ||
            !("link".equals(pedido.getTipo()) || "codigo".equals(pedido.getTipo()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe título, tipo link/codigo e conteúdo");
        }
        validarTamanho(pedido.getTitulo().trim(), "Título");
        validarTamanho(pedido.getDetalhe(), "Detalhe");
        Material material = montarMaterial(professor, pedido.getTitulo(), pedido.getTipo(),
                pedido.getConteudoOuUrl(), pedido.getDetalhe());
        return resposta(materialRepository.save(material), false);
    }

    @Transactional
    public MaterialDto enviarArquivo(Long professorId, String titulo, MultipartFile arquivo) {
        Professor professor = acessoService.professor(professorId);
        if (vazio(titulo) || arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título e arquivo são obrigatórios");
        }
        validarTamanho(titulo.trim(), "Título");
        String nomeSeguro = nomeSeguroDoArquivo(arquivo.getOriginalFilename());
        Path destino = pastaArquivos.resolve(UUID.randomUUID() + "_" + nomeSeguro).normalize();
        // O banco não apaga arquivos ao desfazer uma transação. Registramos a limpeza antes da cópia,
        // para remover também um arquivo parcial ou um upload cuja publicação na sala falhou.
        apagarArquivoSeTransacaoFalhar(destino);
        try {
            Files.createDirectories(pastaArquivos);
            arquivo.transferTo(destino);
        } catch (IOException erro) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao guardar arquivo", erro);
        }
        Material material = montarMaterial(professor, titulo, "arquivo", destino.toString(), nomeSeguro);
        return resposta(materialRepository.save(material), false);
    }

    // readOnly indica que a consulta dos materiais do professor apenas lê os dados.
    @Transactional(readOnly = true)
    public List<MaterialDto> listarProfessor(Long professorId) {
        acessoService.professor(professorId);
        List<MaterialDto> materiais = new ArrayList<>();
        for (Material material : materialRepository.findByProfessorId(professorId)) {
            materiais.add(resposta(material, false));
        }
        return materiais;
    }

    @Transactional(readOnly = true)
    // A página pública usa esta busca para receber somente conteúdos que o professor publicou para visitantes.
    public List<MaterialDto> listarPublicos(Long professorId) {
        List<MaterialDto> materiais = new ArrayList<>();
        for (Material material : materialRepository.findByProfessorIdAndPublicoTrue(professorId)) {
            materiais.add(resposta(material, false));
        }
        return materiais;
    }

    @Transactional
    // Associa um material do professor a uma aula dele e recusa a mesma associação repetida.
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
    // Faz upload e publicação na mesma operação; se uma etapa falhar, a transação não mantém só metade do pedido.
    public MaterialDto enviarArquivoParaSala(Long salaId, String titulo, MultipartFile arquivo) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        MaterialDto materialDto = enviarArquivo(sala.getProfessor().getId(), titulo, arquivo);
        return publicarNaSala(salaId, materialDto.getId());
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listarSala(Long salaId, String tokenAluno) {
        Sala sala = acessoService.salaComAcesso(salaId, tokenAluno);
        // Montamos duas listas para mostrar os materiais fixados primeiro.
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
    // Só um conteúdo já enviado à aula pode entrar ou sair da lista de materiais fixados daquela sala.
    public MaterialDto fixar(Long salaId, Long materialId, MaterialDto pedido) {
        Sala sala = acessoService.salaDoProfessor(salaId);
        acessoService.exigirSalaAtiva(sala);
        if (pedido == null || pedido.isFixado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe fixado");
        }
        if (!contemMaterial(sala.getMateriais(), materialId)) {
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
    // Confere o dono antes de liberar ou retirar este conteúdo da página pública do professor.
    public MaterialDto tornarPublico(
            Long professorId,
            Long materialId,
            MaterialDto pedido) {
        acessoService.professor(professorId);
        Material material = buscar(materialId);
        if (!material.getProfessor().getId().equals(professorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Material pertence a outro professor");
        }
        if (pedido == null || pedido.isPublico() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe publico");
        }
        material.setPublico(pedido.isPublico());
        return resposta(materialRepository.save(material), false);
    }

    @Transactional(readOnly = true)
    // Arquivo público aceita visitantes; arquivo privado exige o dono ou um participante da sala que o recebeu.
    public ArquivoDownload baixar(Long materialId, Long salaId, String tokenAluno) {
        Material material = buscar(materialId);
        if (!"arquivo".equals(material.getTipo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material não é arquivo");
        }
        if (!material.isPublico()) {
            if (salaId == null) {
                acessoService.professor(material.getProfessor().getId());
            } else {
                Sala sala = acessoService.salaComAcesso(salaId, tokenAluno);
                if (!contemMaterial(sala.getMateriais(), materialId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo não está nesta sala");
                }
            }
        }
        Path caminho = caminhoDoArquivo(material);
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

    // Concentra a busca por id usada nas publicações e no download, com erro claro para material inexistente.
    private Material buscar(Long materialId) {
        if (materialId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material obrigatório");
        }
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado");
        }
        return material;
    }

    private Path caminhoDoArquivo(Material material) {
        if (vazio(material.getConteudoOuUrl())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no servidor");
        }
        try {
            // toRealPath resolve atalhos de pasta: o arquivo real também precisa estar dentro dos uploads.
            Path pastaReal = pastaArquivos.toRealPath();
            Path caminho = Path.of(material.getConteudoOuUrl()).toRealPath();
            if (!caminho.startsWith(pastaReal) || !Files.isRegularFile(caminho) || !Files.isReadable(caminho)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no servidor");
            }
            return caminho;
        } catch (IOException | InvalidPathException erro) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no servidor", erro);
        }
    }

    private void apagarArquivoSeTransacaoFalhar(Path destino) {
        // O Spring chama afterCompletion depois de confirmar ou desfazer a transação inteira,
        // inclusive quando enviarArquivo é usado para publicar o material em uma sala.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // @Override implementa o aviso do Spring de que a transação do upload terminou.
            @Override
            public void afterCompletion(int estado) {
                if (estado == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(destino);
                    } catch (IOException erro) {
                        System.getLogger(MaterialService.class.getName()).log(System.Logger.Level.ERROR,
                                "Não foi possível apagar o upload da transação desfeita", erro);
                    }
                }
            }
        });
    }

    // Prepara o registro com o professor conferido e a data atual; id e publicação não são escolhidos pelo pedido.
    private Material montarMaterial(Professor professor, String titulo, String tipo, String conteudo, String detalhe) {
        Material material = new Material();
        material.setTitulo(titulo.trim());
        material.setTipo(tipo);
        material.setConteudoOuUrl(conteudo);
        material.setDetalhe(detalhe);
        material.setProfessor(professor);
        material.setCriadoEm(LocalDateTime.now());
        return material;
    }

    private String nomeSeguroDoArquivo(String nomeOriginal) {
        if (nomeOriginal == null) {
            nomeOriginal = "documento";
        }
        // Substituímos barras e caracteres fora da lista por _ e mantemos no máximo 80 caracteres.
        String nomeSeguro = nomeOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (nomeSeguro.length() > 80) {
            nomeSeguro = nomeSeguro.substring(nomeSeguro.length() - 80);
        }
        return nomeSeguro;
    }

    // Percorre os ids da lista para saber se o conteúdo já foi enviado ou já está fixado na sala.
    private boolean contemMaterial(List<Material> materiais, Long materialId) {
        for (Material material : materiais) {
            if (material.getId().equals(materialId)) {
                return true;
            }
        }
        return false;
    }

    private MaterialDto resposta(Material material, boolean fixado) {
        // O caminho do arquivo fica no servidor. Na resposta mandamos só o endereço de download.
        String conteudo = material.getConteudoOuUrl();
        if ("arquivo".equals(material.getTipo())) {
            conteudo = "/api/materiais/" + material.getId() + "/arquivo";
        }
        return new MaterialDto(material.getId(), material.getTitulo(), material.getTipo(),
            conteudo, material.getDetalhe(), material.isPublico(), fixado, material.getCriadoEm());
    }

    // Trata null e texto só com espaços como falta de preenchimento nos campos obrigatórios.
    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private void validarTamanho(String texto, String campo) {
        // Título e detalhe são colunas de até 255 caracteres; o conteúdo do material pode ser longo.
        if (texto != null && texto.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ter no máximo 255 caracteres");
        }
    }
}
