import { useEffect, useState } from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import Pagina from '../components/Pagina';
import Cabecalho from '../components/Cabecalho';
import Cartao from '../components/Cartao';
import Botao from '../components/Botao';
import Mensagem from '../components/Mensagem';
import Marca from '../components/Marca';
import api from '../services/api';
import { abrirMaterial } from '../services/arquivos';
import { formatarData, iniciais } from '../services/formatacao';
import cores from '../styles/cores';
import estilos from '../styles/estilos';

import type { PerfilPublicoProfessor, Material } from '../types/modelos';

// O App informa o endereço do professor que será consultado e a ação de voltar.
type Props = { slug: string; onVoltar: () => void };

// Apresenta aos visitantes somente o perfil e os materiais liberados para acesso público.
export default function PerfilPublico({ slug, onVoltar }: Props) {
  // useState guarda o perfil recebido e atualiza a tela; null significa que ainda não há perfil.
  const [perfil, setPerfil] = useState<PerfilPublicoProfessor | null>(null);
  // Separa a busca do perfil da abertura de materiais e dos avisos de cada ação.
  const [carregando, setCarregando] = useState(true);
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  // Troca a foto pelas iniciais se o endereço da imagem não puder ser carregado.
  const [fotoFalhou, setFotoFalhou] = useState(false);

  async function carregar() {
    // Limpa o perfil anterior para não misturar professores quando o slug mudar.
    setCarregando(true);
    setErro('');
    setSucesso('');
    setPerfil(null);
    setFotoFalhou(false);
    try {
      // Sem endereço não há consulta; a rota pública dispensa as credenciais do professor.
      if (!slug) throw new Error('Este perfil ainda não tem um endereço público.');
      setPerfil(await api.buscarPerfilPublico(slug));
    } catch (e) {
      // Mostra falhas de conexão ou um perfil que o servidor não conseguiu localizar.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Encerra a espera e permite tentar consultar novamente pelo botão de atualização.
      setCarregando(false);
    }
  }
  // useEffect consulta ao abrir a tela e repete a busca quando o endereço recebido mudar.
  useEffect(() => {
    carregar();
  }, [slug]);

  async function abrir(material: Material) {
    // Bloqueia outra abertura até terminar a tentativa atual e limpa os avisos anteriores.
    if (ocupado) return;
    setOcupado(true);
    setErro('');
    setSucesso('');
    try {
      // O acesso null permite usar o download público; o serviço também abre links e copia códigos.
      await abrirMaterial(material, null);
      // Confirma a ação feita de acordo com o tipo do material escolhido.
      setSucesso(
        material.tipo === 'codigo'
          ? 'Código copiado.'
          : 'Material aberto ou enviado para download.',
      );
    } catch (e) {
      // Avisa quando o material não está disponível ou o aparelho não consegue abri-lo.
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir o pedido.');
    } finally {
      // Libera a escolha de outro material após a tentativa.
      setOcupado(false);
    }
  }

  return (
    <Pagina>
      {/* O visitante pode voltar e acompanhar os avisos da consulta ou da abertura de material. */}
      <Cabecalho titulo="Perfil público" onVoltar={onVoltar} />
      <View style={{ marginTop: 24 }}>
        <Mensagem texto={erro} />
        <Mensagem texto={sucesso} tipo="sucesso" />
      </View>
      {/* Só apresenta os dados do professor depois que a consulta pública foi concluída. */}
      {carregando ? (
        <Text style={estilos.vazio}>Carregando perfil…</Text>
      ) : perfil ? (
        <>
          {/* Mostra nome, descrição e foto pública, usando iniciais quando a imagem falta. */}
          <Cartao style={s.perfil} corDaBorda={cores.azulEscuro}>
            {perfil.fotoUrl && !fotoFalhou ? (
              <Image
                source={{ uri: perfil.fotoUrl }}
                onError={() => setFotoFalhou(true)}
                style={s.foto}
              />
            ) : (
              <View style={s.foto}>
                <Text style={estilos.titulo}>{iniciais(perfil.nome)}</Text>
              </View>
            )}
            <View style={{ flex: 1 }}>
              <Text style={[estilos.titulo, { color: cores.branco }]}>{perfil.nome}</Text>
              {perfil.descricao ? (
                <Text style={[estilos.texto, { color: cores.branco }]}>{perfil.descricao}</Text>
              ) : null}
            </View>
          </Cartao>
          {/* map cria um cartão por material recebido; uma lista vazia recebe um aviso próprio. */}
          <Text style={[estilos.subtitulo, estilos.espaco]}>Materiais públicos</Text>
          {!perfil.materiais || perfil.materiais.length === 0 ? (
            <Cartao>
              <Text style={estilos.vazio}>Este professor ainda não publicou materiais.</Text>
            </Cartao>
          ) : (
            perfil.materiais.map((material) => (
              <Cartao key={material.id}>
                <Text style={estilos.subtitulo}>{material.titulo}</Text>
                <Text style={[estilos.legenda, estilos.espaco]}>
                  {material.tipo} · {formatarData(material.criadoEm)}
                </Text>
                {/* O tipo define se o visitante copia código, abre um link ou baixa um arquivo. */}
                <Botao
                  titulo={
                    material.tipo === 'codigo'
                      ? 'COPIAR CÓDIGO'
                      : material.tipo === 'link'
                        ? 'ABRIR LINK'
                        : 'BAIXAR ARQUIVO'
                  }
                  cor="claro"
                  pequeno
                  desabilitado={ocupado}
                  onPress={() => abrir(material)}
                />
              </Cartao>
            ))
          )}
        </>
      ) : null}
      {/* A atualização manual busca novamente os dados e materiais públicos deste endereço. */}
      <Botao
        titulo="ATUALIZAR PERFIL"
        pequeno
        cor="claro"
        carregando={carregando}
        desabilitado={ocupado}
        onPress={carregar}
      />
      <Marca legenda="APRENDER E COMPARTILHAR" />
    </Pagina>
  );
}

// StyleSheet guarda a apresentação local: cartão azul com dados ao lado de um avatar circular.
const s = StyleSheet.create({
  perfil: { backgroundColor: cores.azul, flexDirection: 'row', alignItems: 'center', gap: 16 },
  // Mantém o mesmo espaço para a foto e para as iniciais, sem deslocar o nome.
  foto: {
    width: 75,
    height: 75,
    borderRadius: 38,
    backgroundColor: cores.azulClaro,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
