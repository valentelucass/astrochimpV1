import { useState } from 'react';
import { Text, View, Image } from 'react-native';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Campo from '../components/Campo';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api from '../services/api';
import estilos from '../styles/estilos';

import type { Professor, AcessoProfessor } from '../types/modelos';

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
