import time
import uuid
from datetime import datetime

import ccxt

from banco import conectar_banco, salvar_precos, salvar_spreads
with conectar_banco() as conn, conn.cursor() as cur:
    cur.execute("SELECT current_database(), current_user, current_schema()")
    print(cur.fetchone())
    

EXCHANGES    = ["binance", "kraken", "okx", "bybit", "kucoin"]
SIMBOLOS     = ["BTC/USDT", "ETH/USDT", "SOL/USDT"]
VOLUME_USDT  = 10_000
INTERVALO    = 60
PROFUNDIDADE = 50

TAXA_PADRAO = 0.001

# Estratégia atual: saldo já posicionado nas duas pontas, sem transferir
# durante a operação. Só ligue isto se for testar o modelo com saque.
MODO_TRANSFERENCIA = False
TAXAS_SAQUE = {"BTC": 0.0002, "ETH": 0.003, "SOL": 0.01}


def conectar(ids):
    ativas = {}
    for id_ in ids:
        try:
            ex = getattr(ccxt, id_)({"enableRateLimit": True})
            ex.load_markets()
            ativas[id_] = ex
        except Exception as e:
            print(f"[falha] conexão {id_}: {type(e).__name__}")
    return ativas


def preco_efetivo(niveis, qtd_alvo):
    """Preço médio real para negociar qtd_alvo. None = livro raso demais."""
    restante, total = qtd_alvo, 0.0
    for nivel in niveis:
        preco, disponivel = nivel[0], nivel[1]
        if not preco or not disponivel:
            continue
        usado = min(restante, disponivel)
        total += usado * preco
        restante -= usado
        if restante <= 0:
            return total / qtd_alvo
    return None


def taxa_taker(exchange, simbolo):
    mercado = exchange.markets.get(simbolo) or {}
    return mercado.get("taker") or TAXA_PADRAO


def taxa_saque(exchange, moeda):
    if not MODO_TRANSFERENCIA:
        return 0.0
    info = exchange.currencies.get(moeda) or {}
    return info.get("fee") or TAXAS_SAQUE.get(moeda, 0.0)


def coletar_livros(exchanges, simbolo):
    livros = {}
    for id_, exchange in exchanges.items():
        if simbolo not in exchange.markets:
            continue
        try:
            livro = exchange.fetch_order_book(simbolo, limit=PROFUNDIDADE)
        except Exception:
            continue
        if livro.get("asks") and livro.get("bids"):
            livros[id_] = livro
    return livros


def analisar(exchanges, simbolo, livros, volume):
    base = simbolo.split("/")[0]

    cotacoes = [
        {
            "exchange":  id_,
            "compra_em": livro["asks"][0][0],
            "vende_em":  livro["bids"][0][0],
            "taxa":      taxa_taker(exchanges[id_], simbolo),
        }
        for id_, livro in livros.items()
    ]

    if len(livros) < 2:
        return cotacoes, []

    rotas = []
    for id_compra, livro_compra in livros.items():
        # quantidade dimensionada no topo do livro DA PRÓPRIA exchange de compra
        topo_ask = livro_compra["asks"][0][0]
        if not topo_ask:
            continue
        qtd = volume / topo_ask

        preco_compra = preco_efetivo(livro_compra["asks"], qtd)
        if preco_compra is None:
            continue

        custo = preco_compra * qtd * (1 + taxa_taker(exchanges[id_compra], simbolo))

        qtd_liquida = qtd - taxa_saque(exchanges[id_compra], base)
        if qtd_liquida <= 0:
            continue

        for id_venda, livro_venda in livros.items():
            if id_venda == id_compra:
                continue

            preco_venda = preco_efetivo(livro_venda["bids"], qtd_liquida)
            if preco_venda is None:
                continue

            receita = preco_venda * qtd_liquida * (1 - taxa_taker(exchanges[id_venda], simbolo))
            rotas.append({
                "compra_em": id_compra,
                "vende_em":  id_venda,
                "lucro":     (receita - custo) / custo,
            })

    return cotacoes, sorted(rotas, key=lambda r: -r["lucro"])


def ciclo(conn, exchanges):
    coleta_id = uuid.uuid4()
    agora = datetime.now().strftime("%H:%M:%S")

    for simbolo in SIMBOLOS:
        livros = coletar_livros(exchanges, simbolo)
        if not livros:
            print(f"[{agora}] {simbolo}: sem dados")
            continue

        cotacoes, rotas = analisar(exchanges, simbolo, livros, VOLUME_USDT)

        salvar_precos(conn, coleta_id, simbolo, cotacoes)
        salvar_spreads(conn, coleta_id, simbolo, VOLUME_USDT, rotas)

        melhor = rotas[0] if rotas else None
        resumo = (f"{melhor['compra_em']} -> {melhor['vende_em']} "
                  f"{melhor['lucro'] * 100:+.3f}%") if melhor else "-"
        print(f"[{agora}] {simbolo:<10} {len(cotacoes)} exchanges | melhor: {resumo}")


if __name__ == "__main__":
    conn = conectar_banco()
    exchanges = conectar(EXCHANGES)
    modo = "com saque" if MODO_TRANSFERENCIA else "sem transferência"
    print(f"monitorando {len(exchanges)} exchanges ({modo}), ciclo de {INTERVALO}s. ctrl+c pra parar.\n")

    try:
        while True:
            try:
                ciclo(conn, exchanges)
            except Exception as e:
                print(f"[erro no ciclo] {type(e).__name__}: {e}")
                conn.rollback()
            time.sleep(INTERVALO)
    except KeyboardInterrupt:
        print("\nencerrando.")
    finally:
        conn.close()