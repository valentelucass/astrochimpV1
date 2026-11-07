import { useState } from 'react'; // Importa useState do React para guardar e-mail, senha, erro e espera do login.
import { Text, View, Image } from 'react-native'; // Importa os elementos do React Native usados para mostrar textos, imagem e caixas da tela.
import Pagina from '../components/Pagina'; // Importa Pagina para aplicar a estrutura e a rolagem padrão da tela.
import Cabecalho from '../components/Cabecalho'; // Importa Cabecalho para mostrar o título e a ação de voltar.
import Cartao from '../components/Cartao'; // Importa Cartao para agrupar os campos de acesso do professor.
import Campo from '../components/Campo'; // Importa Campo para receber o e-mail e a senha.
import Botao from '../components/Botao'; // Importa Botao para entrar, voltar ou abrir o cadastro.
import Mensagem from '../components/Mensagem'; // Importa Mensagem para informar dados inválidos ou falhas no acesso.
import Marca from '../components/Marca'; // Importa Marca para mostrar a identidade do AstroChimp no fim da tela.
import api from '../services/api'; // Importa o serviço da API para validar as credenciais no backend.
import estilos from '../styles/estilos'; // Importa os estilos compartilhados para manter os textos no padrão visual.

import type { Professor, AcessoProfessor } from '../types/modelos'; // Importa somente os tipos que descrevem o professor autenticado e suas credenciais.

// O App fornece as ações de entrar na sessão, abrir o cadastro e voltar à tela anterior.
type Props = {
  onEntrar: (perfil: Professor, acesso: AcessoProfessor) => void;
  onCadastrar: () => void;
  onVoltar: () => void;
};

// Confere as credenciais do professor antes de liberar o painel.
export default function LoginProfessor({ onEntrar, onCadastrar, onVoltar }: Props) {
  // useState guarda os valores digitados e redesenha a tela quando eles mudam.
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  // Controla a espera pelo servidor e a mensagem exibida quando o login falha.
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState('');

  // Envia as credenciais e entrega ao App o perfil confirmado pelo Spring.
  async function entrar() {
    // Evita repetir o pedido e recusa campos obrigatórios vazios antes da consulta.
    if (carregando) return;
    if (!email.trim() || !senha) {
      setErro('Informe seu e-mail e sua senha.');
      return;
    }
    // Bloqueia o formulário e remove o erro da tentativa anterior.
    setCarregando(true);
    setErro('');
    try {
      // Normaliza o e-mail; a senha permanece exatamente como foi digitada.
      const acesso = { email: email.trim().toLowerCase(), senha };
      const perfil = await api.buscarProfessorLogado(acesso);
      // O App recebe a sessão em memória depois que a API confirma o login.
      onEntrar(perfil, acesso);
    } catch (e) {
      // Exibe a recusa do servidor ou uma mensagem para um erro sem descrição.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera uma nova tentativa tanto no sucesso quanto na falha.
      setCarregando(false);
    }
  }

  return (
    <Pagina>
      {/* O cabeçalho permite voltar; o mascote apresenta a área de acesso do professor. */}
      <Cabecalho titulo="Área do professor" onVoltar={carregando ? undefined : onVoltar} />
      <View style={{ alignItems: 'center', paddingVertical: 24 }}>
        <Image
          source={require('../../assets/mascote.png')}
          style={{ width: 110, height: 110, resizeMode: 'contain' }}
        />
        <Text style={estilos.titulo}>Bom ter você de volta!</Text>
        <Text style={estilos.texto}>Entre para preparar sua próxima aula.</Text>
      </View>
      {/* Os campos usam os valores locais e ficam bloqueados enquanto o login é conferido. */}
      <Cartao>
        <Mensagem texto={erro} />
        <Campo
          rotulo="E-mail"
          valor={email}
          onChangeText={setEmail}
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
          editable={!carregando}
        />
        <Campo
          rotulo="Senha"
          valor={senha}
          onChangeText={setSenha}
          secureTextEntry
          autoCapitalize="none"
          autoComplete="current-password"
          editable={!carregando}
          onSubmitEditing={entrar}
        />
        <Botao titulo="ENTRAR" icone="log-in-outline" carregando={carregando} onPress={entrar} />
      </Cartao>
      {/* Quem ainda não tem conta abre o cadastro pelo callback recebido do App. */}
      <Botao
        titulo="CRIAR MINHA CONTA"
        cor="claro"
        onPress={onCadastrar}
        desabilitado={carregando}
      />
      {/* A marca identifica a versão; os textos e campos usam os estilos compartilhados. */}
      <Marca legenda="VERSÃO DOS PROFESSORES" />
    </Pagina>
  );
}
