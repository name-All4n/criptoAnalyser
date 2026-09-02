import ccxt

EXCHANGES = ["binance", "kraken", "okx", "bybit", "kucoin"]
SIMBOLOS  = ["BTC/USDT", "ETH/USDT", "SOL/USDT"]

VOLUME_USDT = 1000   # quanto você realmente pretende movimentar
TAXA_PADRAO = 0.001

# taxa de saque na rede, em unidades da própria moeda (fallback)
TAXAS_SAQUE = {"BTC": 0.0002, "ETH": 0.003, "SOL": 0.01}


def conectar(ids):
    ativas = {}
    for id_ in ids:
        try:
            ex = getattr(ccxt, id_)({"enableRateLimit": True})
            ex.load_markets()
            ativas[id_] = ex
        except Exception:
            print(f"[falha] {id_}")
    return ativas


def preco_efetivo(niveis, qtd_alvo):
    """Percorre a fila de ofertas e devolve o preço MÉDIO real
    pra negociar qtd_alvo. None = não tem oferta suficiente."""
    restante, gasto = qtd_alvo, 0.0
    for nivel in niveis:
        preco, disponivel = nivel[0], nivel[1]   # ignora extras (timestamp etc.)
        if not preco or not disponivel:
            continue
        usado = min(restante, disponivel)
        gasto += usado * preco
        restante -= usado
        if restante <= 0:
            return gasto / qtd_alvo
    return None


def taxa_saque(ex, moeda):
    info = ex.currencies.get(moeda) or {}
    return info.get("fee") or TAXAS_SAQUE.get(moeda, 0.0)


def livros(exchanges, simbolo):
    resultado = {}
    for id_, ex in exchanges.items():
        if simbolo not in ex.markets:
            continue
        try:
            livro = ex.fetch_order_book(simbolo, limit=50)
        except Exception:
            continue
        if livro["asks"] and livro["bids"]:
            resultado[id_] = livro
    return resultado


def analisar(exchanges, simbolo):
    base = simbolo.split("/")[0]
    books = livros(exchanges, simbolo)
    if len(books) < 2:
        print(f"\n{simbolo}: dados insuficientes")
        return

    qtd = VOLUME_USDT / books[list(books)[0]]["asks"][0][0]
    rotas = []

    for compra_em, livro_c in books.items():
        for vende_em, livro_v in books.items():
            if compra_em == vende_em:
                continue

            ex_c, ex_v = exchanges[compra_em], exchanges[vende_em]
            preco_compra = preco_efetivo(livro_c["asks"], qtd)
            if preco_compra is None:
                continue

            taxa_c = ex_c.markets[simbolo].get("taker") or TAXA_PADRAO
            taxa_v = ex_v.markets[simbolo].get("taker") or TAXA_PADRAO

            custo = preco_compra * qtd * (1 + taxa_c)

            # o saque na rede come um pedaço da moeda
            qtd_chegou = qtd - taxa_saque(ex_c, base)
            if qtd_chegou <= 0:
                continue

            preco_venda = preco_efetivo(livro_v["bids"], qtd_chegou)
            if preco_venda is None:
                continue

            receita = preco_venda * qtd_chegou * (1 - taxa_v)
            rotas.append({
                "rota":    f"{compra_em} -> {vende_em}",
                "lucro":   (receita - custo) / custo,
                "reais":   receita - custo,
            })

    print(f"\n=== {simbolo} | movimentando {VOLUME_USDT} USDT ({qtd:.6f} {base}) ===")
    for r in sorted(rotas, key=lambda r: -r["lucro"])[:3]:
        print(f"  {r['rota']:<22} {r['lucro'] * 100:+.3f}%   ({r['reais']:+.2f} USDT)")


if __name__ == "__main__":
    conexoes = conectar(EXCHANGES)
    for volume in [1_000, 10_000, 100_000]:
        VOLUME_USDT = volume
        print(f"\n########## {volume:,} USDT ##########")
        for s in SIMBOLOS:
            analisar(conexoes, s)