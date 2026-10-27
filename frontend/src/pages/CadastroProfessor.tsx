import { useState } from 'react';
import { Text } from 'react-native';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api from '../services/api';
import estilos from '../styles/estilos';

import type {
  Professor,
  AcessoProfessor,
  CadastroProfessor as DadosCadastro,
} from '../types/modelos';

// O App recebe a conta criada com seu acesso e fornece a ação de voltar.
type Props = {
  onEntrar: (perfil: Professor, acesso: AcessoProfessor) => void;
  onVoltar: () => void;
};

// Reúne os dados do novo professor e inicia a sessão após o cadastro confirmado.
export default function CadastroProfessor({ onEntrar, onVoltar }: Props) {
  // useState mantém o formulário na tela e atualiza o que aparece a cada mudança.
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [slug, setSlug] = useState('');
  // Guarda a falha de cadastro e impede outro envio enquanto o servidor responde.
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  async function cadastrar() {
    // Exige nome, e-mail válido e senha, sem reenviar um cadastro ainda em andamento.
    if (carregando) return;
    if (!nome.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) || !senha.trim()) {
      setErro('Preencha seu nome, um e-mail válido e uma senha.');
      return;
    }
    // O endereço é opcional, mas quando preenchido precisa servir como parte de uma URL.
    if (slug.trim() && !/^[a-z0-9-]+$/.test(slug.trim())) {
      setErro('No endereço público, use letras minúsculas, números e hífen.');
      return;
    }
    // Limpa a tentativa anterior e bloqueia os campos durante a criação da conta.
    setErro('');
    setCarregando(true);
    try {
      // Monta somente os campos aceitos pelo cadastro, mantendo a senha original.
      const dados: DadosCadastro = {
        nome: nome.trim(),
        email: email.trim().toLowerCase(),
        senha,
        slugPerfilPublico: slug.trim(),
      };
      const perfil = await api.cadastrarProfessor(dados);
      // A confirmação devolve o perfil; o App passa a guardar a sessão em memória.
      onEntrar(perfil, { email: dados.email, senha });
    } catch (e) {
      // Mantém o formulário preenchido para a pessoa corrigir o problema informado.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Encerra a espera mesmo quando o Spring recusa o cadastro.
      setCarregando(false);
    }
  }

  return (
    <Pagina>
      {/* Apresenta o cadastro e oferece retorno enquanto nenhum pedido está pendente. */}
      <Cabecalho titulo="Criar conta" onVoltar={carregando ? undefined : onVoltar} />
      <Text style={[estilos.titulo, { marginVertical: 24 }]}>Sua próxima aula começa aqui.</Text>
      {/* O cartão agrupa os dados pessoais, o acesso e a escolha do endereço público. */}
      <Cartao>
        <Mensagem texto={erro} />
        <Campo
          rotulo="Seu nome"
          valor={nome}
          onChangeText={setNome}
          maxLength={255}
          editable={!carregando}
        />
        <Campo
          rotulo="E-mail"
          valor={email}
          onChangeText={setEmail}
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
          maxLength={255}
          editable={!carregando}
        />
        <Campo
          rotulo="Senha"
          valor={senha}
          onChangeText={setSenha}
          secureTextEntry
          autoCapitalize="none"
          autoComplete="new-password"
          editable={!carregando}
        />
        <Campo
          rotulo="Endereço público (opcional)"
          valor={slug}
          onChangeText={setSlug}
          autoCapitalize="none"
          maxLength={255}
          editable={!carregando}
        />
        {/* A orientação ajuda a escolher o slug; o botão envia o formulário validado. */}
        <Text style={[estilos.legenda, estilos.espaco]}>
          Escolha um nome curto com letras minúsculas, números ou hífen. Você poderá alterá-lo no
          perfil.
        </Text>
        <Botao
          titulo="CRIAR CONTA"
          onPress={cadastrar}
          carregando={carregando}
          icone="person-add-outline"
        />
      </Cartao>
      {/* Os estilos compartilhados mantêm a mesma tipografia e os mesmos espaços do login. */}
      <Marca legenda="VERSÃO DOS PROFESSORES" />
    </Pagina>
  );
}
