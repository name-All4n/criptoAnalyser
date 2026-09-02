const API = "";
const POLL_MS = 60000;
const IDADE_MAX = 120;
const VOL_DEMO = 10000;

const CORES = { "BTC/USDT": "#4f4fa3", "ETH/USDT": "#b0604a", "SOL/USDT": "#2f7a5f" };

const state = {
  modo: "carregando",
  precos: [],
  spreads: [],
  hist: {},
  limiar: 0.30,
  timer: null,
  anterior: {},
  chart: null,
};

const fPct = n => (n >= 0 ? "+" : "−") + Math.abs(n).toFixed(3).replace(".", ",") + "%";
const fUsd = n => (n >= 0 ? "+" : "−") + Math.abs(n).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fPreco = n => Number(n).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fTaxa = n => (Number(n) * 100).toFixed(3).replace(".", ",") + "%";
const fIdade = s => s < 90 ? `há ${Math.round(s)}s` : s < 5400 ? `há ${Math.round(s / 60)}min` : `há ${Math.round(s / 3600)}h`;
const hora = iso => new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" });

const BASE = {
  "BTC/USDT": { binance: [114382.51, 114381.77, 0.0010], bybit: [114366.90, 114365.42, 0.0010], okx: [114390.18, 114388.55, 0.0008], kucoin: [114402.33, 114399.10, 0.0010] },
  "ETH/USDT": { binance: [4271.83, 4271.62, 0.0010], bybit: [4270.98, 4270.71, 0.0010], okx: [4272.44, 4272.15, 0.0008], kucoin: [4273.60, 4273.19, 0.0010] },
  "SOL/USDT": { binance: [214.671, 214.652, 0.0010], bybit: [214.620, 214.604, 0.0010], okx: [214.712, 214.690, 0.0008], kucoin: [214.795, 214.751, 0.0010] },
};

function demo(quando = new Date()) {
  const coletaId = crypto.randomUUID();
  const iso = quando.toISOString();
  const jitter = () => 1 + (Math.random() - 0.5) * 0.00009;
  const precos = [], spreads = [];

  for (const [simbolo, exchanges] of Object.entries(BASE)) {
    const j = jitter();
    const linhas = Object.entries(exchanges).map(([ex, [ask, bid, taxa]]) => ({
      exchange: ex, ask: ask * j * jitter(), bid: bid * j * jitter(), taxa,
    }));

    for (const l of linhas) {
      precos.push({
        coletaId, coletadoEm: iso, simbolo, exchange: l.exchange,
        melhorCompra: l.ask, melhorVenda: l.bid, taxaTaker: l.taxa,
        spreadInternoPct: ((l.bid - l.ask) / l.ask) * 100,
        idadeSegundos: 0,
      });
    }
    for (const c of linhas) for (const v of linhas) {
      if (c.exchange === v.exchange) continue;
      const custo = c.ask * (1 + c.taxa);
      const receita = v.bid * (1 - v.taxa);
      spreads.push({
        coletaId, coletadoEm: iso, simbolo,
        rota: `${c.exchange} -> ${v.exchange}`,
        exchangeCompra: c.exchange, exchangeVenda: v.exchange,
        volumeUsdt: VOL_DEMO,
        lucroPct: ((receita - custo) / custo) * 100,
        lucroUsdt: 0, status: "", idadeSegundos: 0, dadoVelho: false,
      });
    }
  }
  spreads.forEach(s => { s.lucroUsdt = s.volumeUsdt * s.lucroPct / 100; });
  spreads.sort((a, b) => b.lucroPct - a.lucroPct);
  return { precos, spreads };
}

function semearHistorico() {
  const agora = Date.now();
  for (let i = 17; i >= 1; i--) {
    const t = new Date(agora - i * POLL_MS);
    const d = demo(t);
    registrarHistorico(d.spreads, t);
  }
}

async function buscar() {
  const btn = document.getElementById("btn-refresh");
  btn.disabled = true;
  try {
    const [rp, rs] = await Promise.all([
      fetch(`${API}/api/precos/atuais`),
      fetch(`${API}/api/spreads/atuais?limite=200`),
    ]);
    if (!rp.ok && rp.status !== 204) throw new Error("HTTP " + rp.status);

    const precos = rp.status === 204 ? [] : await rp.json();
    const spreads = rs.status === 204 ? [] : await rs.json();

    if (!precos.length) { state.modo = "vazio"; state.precos = []; state.spreads = []; }
    else { state.modo = "live"; state.precos = precos; state.spreads = spreads; }
  } catch (e) {
    const d = demo();
    state.modo = "demo";
    state.precos = d.precos;
    state.spreads = d.spreads;
  } finally {
    btn.disabled = false;
  }
  if (state.spreads.length) registrarHistorico(state.spreads);
  await buscarHistorico();  // <-- adiciona aqui
  render();
}

function registrarHistorico(spreads, quando = new Date()) {
  const porSimbolo = {};
  for (const s of spreads) {
    if (!(s.simbolo in porSimbolo) || s.lucroPct > porSimbolo[s.simbolo]) porSimbolo[s.simbolo] = s.lucroPct;
  }
  for (const [simbolo, pct] of Object.entries(porSimbolo)) {
    (state.hist[simbolo] ||= []).push({ t: quando, pct });
    if (state.hist[simbolo].length > 120) state.hist[simbolo].shift();
  }
}

function classificar(pct) {
  if (pct >= state.limiar) return { txt: "oportunidade", cor: "var(--moss)", fundo: "var(--moss-soft)" };
  if (pct > 0) return { txt: "observação", cor: "var(--indigo)", fundo: "var(--indigo-soft)" };
  return { txt: "negativa", cor: "var(--clay)", fundo: "var(--clay-soft)" };
}

function render() {
  renderStatus();
  if (state.modo === "vazio") { renderVazio(); return; }
  renderPares();
  renderLedger();
  renderChart();
  window.lucide?.createIcons();
}

function renderStatus() {
  const pill = document.getElementById("pill-modo");
  const rotulo = { live: "ao vivo", demo: "demonstração", erro: "sem api", vazio: "banco vazio", carregando: "carregando" }[state.modo];
  pill.className = "pill " + (state.modo === "live" ? "pill--live" : state.modo === "demo" ? "pill--demo" : "pill--erro");
  pill.lastElementChild.textContent = rotulo;

  const idade = state.precos.length ? Math.max(0, state.precos[0].idadeSegundos ?? 0) : null;
  document.getElementById("m-idade").textContent = state.precos.length
    ? `${fIdade(idade)} · ${hora(state.precos[0].coletadoEm)}` : "—";
  document.getElementById("m-volume").textContent = state.spreads.length
    ? Number(state.spreads[0].volumeUsdt).toLocaleString("pt-BR") + " USDT" : "—";
  document.getElementById("m-exchanges").textContent = state.precos.length
    ? new Set(state.precos.map(p => p.exchange)).size : "—";

  const notice = document.getElementById("notice");
  const txt = document.getElementById("notice-txt");
  const velho = state.precos.some(p => p.dadoVelho) || (idade !== null && idade > IDADE_MAX);

  if (state.modo === "demo") {
    notice.classList.add("on");
    notice.style.borderColor = "var(--indigo)";
    notice.style.background = "var(--indigo-soft)";
    txt.innerHTML = `<b>Dados de demonstração.</b> A API não respondeu. Suba o Spring Boot e o <code>monitor.py</code>.`;
  } else if (velho) {
    notice.classList.add("on");
    notice.style.borderColor = "var(--clay)";
    notice.style.background = "var(--clay-soft)";
    txt.innerHTML = `<b>Coleta velha (${fIdade(idade)}).</b> Alertas suspensos até novo ciclo.`;
  } else {
    notice.classList.remove("on");
  }
}

function renderVazio() {
  document.getElementById("pares").innerHTML = `<div class="empty"><h3>Banco vazio</h3><p>Ligue o <code>monitor.py</code></p></div>`;
  document.getElementById("ledger-body").innerHTML = "";
  window.lucide?.createIcons();
}

function renderPares() {
  const simbolos = [...new Set(state.spreads.map(s => s.simbolo))];
  const alvo = document.getElementById("pares");
  alvo.innerHTML = "";

  simbolos.forEach((simbolo, i) => {
    const rotas = state.spreads.filter(s => s.simbolo === simbolo).sort((a, b) => b.lucroPct - a.lucroPct);
    const melhor = rotas[0];
    const precos = state.precos.filter(p => p.simbolo === simbolo);
    const pc = precos.find(p => p.exchange === melhor.exchangeCompra);
    const pv = precos.find(p => p.exchange === melhor.exchangeVenda);

    const bruto = pc && pv ? ((pv.melhorVenda - pc.melhorCompra) / pc.melhorCompra) * 100 : null;
    const taxaC = pc ? Number(pc.taxaTaker) * 100 : null;
    const taxaV = pv ? Number(pv.taxaTaker) * 100 : null;
    const cls = classificar(melhor.lucroPct);
    const mudou = state.anterior[simbolo] !== undefined && state.anterior[simbolo].toFixed(3) !== melhor.lucroPct.toFixed(3);
    state.anterior[simbolo] = melhor.lucroPct;

    const [base, quote] = simbolo.split("/");
    const sinal = melhor.lucroPct >= 0 ? "pos" : "neg";

    const el = document.createElement("article");
    el.className = "par";
    el.style.setProperty("--i", i);
    el.innerHTML = `
      <div class="par-id">
        <span class="base">${base}<span class="quote">/${quote}</span></span>
        <div class="rota">comprar em <span class="ex">${melhor.exchangeCompra}</span> <span class="arrow">→</span> vender em <span class="ex">${melhor.exchangeVenda}</span></div>
        <span class="status-tag" style="color:${cls.cor};border-color:${cls.cor};background:${cls.fundo}">${cls.txt}</span>
      </div>
      <div class="par-liquido">
        <span class="big num ${sinal} ${mudou ? "flick" : ""}">${fPct(melhor.lucroPct)}</span>
        <span class="sub">líquido, ${rotas.length} rotas</span>
        <div class="bruto">bruto <b class="num">${bruto === null ? "—" : fPct(bruto)}</b></div>
      </div>
      <dl class="par-custos kv">
        <dt>taxa compra</dt><dd class="num">${taxaC === null ? "—" : "−" + taxaC.toFixed(3).replace(".", ",") + "%"}</dd>
        <dt>taxa venda</dt><dd class="num">${taxaV === null ? "—" : "−" + taxaV.toFixed(3).replace(".", ",") + "%"}</dd>
        <dt>taxa saque</dt><dd class="num">não</dd>
        <div class="sep"></div>
        <dt>resultado</dt>
        <dd class="num total ${sinal}">${fUsd(melhor.lucroUsdt ?? melhor.volumeUsdt * melhor.lucroPct / 100)}</dd>
      </dl>
      <div class="ladder">${ladder(rotas)}</div>`;
    alvo.appendChild(el);
  });
}

function ladder(rotas) {
  const vals = rotas.map(r => r.lucroPct);
  const min = Math.min(...vals);
  const max = Math.max(0.02, Math.max(...vals));
  const dom = max - min || 1;
  const pos = v => ((v - min) / dom) * 100;

  const ticks = rotas.map(r => `<span class="tick ${r === rotas[0] ? "best" : ""}" style="left:${pos(r.lucroPct).toFixed(2)}%" title="${r.exchangeCompra} → ${r.exchangeVenda}: ${fPct(r.lucroPct)}"></span>`).join("");

  return `<div class="ladder-track">${ticks}<span class="zero" style="left:${pos(0).toFixed(2)}%"></span></div><div class="ladder-legend"><span>pior ${fPct(min)}</span><span>rotas</span><span>melhor ${fPct(Math.max(...vals))}</span></div>`;
}

function renderLedger() {
  const corpo = document.getElementById("ledger-body");
  const simbolos = [...new Set(state.precos.map(p => p.simbolo))];
  corpo.innerHTML = simbolos.map(simbolo => {
    const linhas = state.precos.filter(p => p.simbolo === simbolo);
    const menorAsk = Math.min(...linhas.map(l => Number(l.melhorCompra)));
    const maiorBid = Math.max(...linhas.map(l => Number(l.melhorVenda)));

    const tr = linhas.sort((a, b) => a.melhorCompra - b.melhorCompra).map(l => {
      const barato = Number(l.melhorCompra) === menorAsk;
      const caro = Number(l.melhorVenda) === maiorBid;
      return `<tr><td>${l.exchange}${barato ? '<span class="flag">mais barata</span>' : ""}${caro ? '<span class="flag">mais cara</span>' : ""}</td><td class="${barato ? "cheap" : ""}">${fPreco(l.melhorCompra)}</td><td class="${caro ? "rich" : ""}">${fPreco(l.melhorVenda)}</td><td class="neg">${l.spreadInternoPct === null ? "—" : fPct(Number(l.spreadInternoPct))}</td><td>${fTaxa(l.taxaTaker)}</td></tr>`;
    }).join("");
    return `<tr class="group"><th colspan="5">${simbolo}</th></tr>${tr}`;
  }).join("");
}

function renderChart() {
  const ctx = document.getElementById("chart");
  const simbolos = Object.keys(state.hist);
  if (!simbolos.length) return;

  // Se tiver dados reais do histórico (mode live), usar eles
  // Senão usar o acumulado da sessão (demo)
  const labels = state.hist[simbolos[0]].map(p => hora(p.t).slice(0, 5));
  const series = simbolos.map(s => ({
    label: s,
    data: state.hist[s].map(p => p.pct),
    borderColor: CORES[s] || "#4f4fa3",
    backgroundColor: CORES[s] || "#4f4fa3",
    borderWidth: 1.5, pointRadius: 0, pointHoverRadius: 4, tension: 0.25,
  }));
  series.push({
    label: `limiar ${state.limiar.toFixed(2)}%`,
    data: labels.map(() => state.limiar),
    borderColor: "#8a8698", borderWidth: 1, borderDash: [3, 4],
    pointRadius: 0, tension: 0,
  });

  if (state.chart) {
    state.chart.data.labels = labels;
    state.chart.data.datasets = series;
    state.chart.update("none");
    return;
  }

  state.chart = new Chart(ctx, {
    type: "line",
    data: { labels, datasets: series },
    options: {
      responsive: true, maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: {
          position: "top", align: "end",
          labels: { boxWidth: 8, boxHeight: 8, font: { family: "IBM Plex Mono", size: 11 }, color: "#4a4657", padding: 14 },
        },
      },
      scales: {
        x: { grid: { display: false }, ticks: { font: { family: "IBM Plex Mono", size: 10 }, color: "#8a8698" } },
        y: { grid: { color: "#eceaf0" }, ticks: { font: { family: "IBM Plex Mono", size: 10 }, color: "#8a8698", callback: v => v.toFixed(2).replace(".", ",") + "%" } },
      },
    },
  });
}

async function buscarHistorico() {
  try {
    const res = await fetch(`${API}/api/spreads/historico?limite=1000`);
    if (!res.ok || res.status === 204) return;

    const dados = await res.json();
    const porSimbolo = {};

    for (const d of dados) {
      if (!(d.simbolo in porSimbolo)) porSimbolo[d.simbolo] = [];
      porSimbolo[d.simbolo].push({ t: new Date(d.momento), pct: Number(d.lucroPct) });
    }

    state.hist = porSimbolo;
  } catch (e) {
    // Falha silenciosa, continua com histórico de sessão
  }
}

document.getElementById("pares").innerHTML = Array.from({ length: 3 }, () => `<div class="skel"><div class="bar" style="width:22%"></div><div class="bar" style="width:64%"></div><div class="bar" style="width:40%"></div></div>`).join("");

document.getElementById("btn-refresh").addEventListener("click", buscar);

document.getElementById("limiar").addEventListener("input", e => {
  const v = parseFloat(e.target.value);
  if (Number.isNaN(v)) return;
  state.limiar = v;
  if (state.modo !== "vazio" && state.spreads.length) { renderPares(); renderChart(); }
});

document.getElementById("auto").addEventListener("change", e => {
  clearInterval(state.timer);
  if (e.target.checked) state.timer = setInterval(buscar, POLL_MS);
});

document.addEventListener("DOMContentLoaded", () => {
  semearHistorico();
  state.timer = setInterval(buscar, POLL_MS);
  buscar();
});