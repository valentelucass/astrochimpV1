package edu.senacsp.astrochimp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AlternativaAlunoDto {



    // Recebe o ID escolhido pelo aluno e devolve as opções do quiz sem o campo correta, que entregaria o gabarito.
    // Este DTO existe porque id identifica a opção escolhida, e o atributo correta da entidade precisa ficar fora da resposta ao aluno.



    // Aceita alternativaId no pedido de resposta e mantém id como nome desse campo no JSON enviado ao aluno.
    @JsonAlias("alternativaId")
    private Long id; // O aluno envia alternativaId para escolher a opção. O gabarito fica no servidor.
    // Letra e texto só saem no JSON; a escolha do aluno usa somente o id da alternativa salva no banco.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String letra;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String texto;



    
    // Cria uma AlternativaAlunoDto vazia para ler o id escolhido no JSON de resposta do aluno.
    @JsonCreator
    public AlternativaAlunoDto() {
    }






    public AlternativaAlunoDto(Long id, String letra, String texto) {
        this.id = id;
        this.letra = letra;
        this.texto = texto;
    }







    // Neste DTO, get/is leem os campos da resposta e set preenche os valores do objeto Java.
    // As anotações dos campos acima continuam definindo o que pode entrar e sair no JSON.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLetra() {
        return letra;
    }

    public void setLetra(String letra) {
        this.letra = letra;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
