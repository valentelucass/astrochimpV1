import http from 'k6/http'; // Envia consultas HTTP para a API de teste isolada.
import { check, sleep } from 'k6'; // Confere as respostas e respeita o intervalo de atualização.

// Este roteiro mede somente leituras da API. Não mede renderização ou desempenho do celular.
// As metas abaixo são propostas para discussão pelo grupo; não são resultados observados.
const base = __ENV.BASE_URL || 'http://127.0.0.1:18080';
const salaId = __ENV.SALA_ID || '';
const vus = Number(__ENV.VUS || 10);
const segundos = Number(__ENV.SEGUNDOS || 60);
const tokens = JSON.parse(__ENV.TOKENS_ALUNOS || '[]');

// A porta separada e a confirmação reduzem o risco de usar o servidor das aulas.
// O operador ainda precisa conferir o banco do processo; uma URL não comprova isolamento.
if (base !== 'http://127.0.0.1:18080' || __ENV.CONFIRMO_AMBIENTE_ISOLADO !== 'SIM') {
  throw new Error('Use API isolada em http://127.0.0.1:18080 e CONFIRMO_AMBIENTE_ISOLADO=SIM.');
}
if (!/^\d+$/.test(salaId)) throw new Error('Informe SALA_ID de uma sala sintética ativa.');
if (!Number.isInteger(vus) || vus < 1 || vus > 30) throw new Error('VUS deve ficar entre 1 e 30.');
if (!Number.isInteger(segundos) || segundos < 1 || segundos > 120) {
  throw new Error('SEGUNDOS deve ficar entre 1 e 120.');
}
if (!Array.isArray(tokens) || tokens.length < vus ||
    tokens.some((token) => typeof token !== 'string' || !token.trim()) ||
    new Set(tokens).size !== tokens.length) {
  throw new Error('TOKENS_ALUNOS deve ser um JSON com um token sintético diferente por usuário.');
}

export const options = {
  vus,
  duration: segundos + 's',
  maxRedirects: 0,
  thresholds: {
    // Critérios propostos, ainda não aprovados nem medidos no AstroChimp.
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate==1'],
  },
};

function parametros(nome, token) {
  return {
    headers: { 'X-Aluno-Token': token },
    tags: { name: nome },
    timeout: '5s',
  };
}

function conferir(resposta, nome) {
  return check(resposta, { [nome + ': HTTP 200']: (r) => r.status === 200 });
}

// Reproduz a ordem das consultas de SalaAluno: sala, dúvidas/materiais e cronômetro/quiz.
// O intervalo real começa a cada 3 s e evita sobreposição. Aqui o ciclo espera o
// restante de 3 s; se ficar mais lento, a carga efetiva será menor e deve ser registrada.
export default function () {
  const inicio = Date.now();
  const token = tokens[__VU - 1];
  const caminho = base + '/api/salas/' + salaId;
  const sala = http.get(caminho, parametros('sala', token));
  if (!conferir(sala, 'sala')) {
    sleep(3);
    return;
  }
  check(sala, {
    'sala: id confere': (r) => {
      try { return Number(r.json('id')) === Number(salaId); } catch { return false; }
    },
  });

  const listas = http.batch([
    ['GET', caminho + '/duvidas', null, parametros('duvidas', token)],
    ['GET', caminho + '/materiais', null, parametros('materiais', token)],
  ]);
  listas.forEach((resposta, indice) => {
    const nome = indice === 0 ? 'duvidas' : 'materiais';
    conferir(resposta, nome);
    check(resposta, {
      [nome + ': lista recebida']: (r) => {
        try { return Array.isArray(r.json()); } catch { return false; }
      },
    });
  });
  const controles = http.batch([
    ['GET', caminho + '/cronometro', null, parametros('cronometro', token)],
    ['GET', caminho + '/quiz/pergunta-atual', null, parametros('quiz', token)],
  ]);
  controles.forEach((resposta, indice) => conferir(resposta, indice === 0 ? 'cronometro' : 'quiz'));
  sleep(Math.max(0, 3 - (Date.now() - inicio) / 1000));
}
