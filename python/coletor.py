import ccxt

EXCHANGES = ["binance", "kraken", "okx", "bybit", "kucoin"]
SIMBOLOS  = ["BTC/USDT", "ETH/USDT", "SOL/USDT"]

TAXA_PADRAO = 0.001  # 0,1% se a exchange não informar


def conectar(ids):
    """Abre conexão com cada exchange e carrega a lista de mercados."""
    ativas = {}
    for id_ in ids:
        try:
            ex = getattr(ccxt, id_)({"enableRateLimit": True})
            ex.load_markets()
            ativas[id_] = ex
            print(f"[ok]    {id_}")
        except Exception as e:
            print(f"[falha] {id_}: {type(e).__name__}")
    return ativas


def taxa_taker(ex, simbolo):
    """Comissão cobrada de quem compra/vende a mercado."""
    mercado = ex.markets.get(simbolo) or {}
    return mercado.get("taker") or TAXA_PADRAO


def coletar(exchanges, simbolos):
    """Retorna {simbolo: [{exchange, compra_em, vende_em, taxa}, ...]}."""
    dados = {s: [] for s in simbolos}

    for id_, ex in exchanges.items():
        disponiveis = [s for s in simbolos if s in ex.markets]
        if not disponiveis:
            continue
        try:
            tickers = ex.fetch_tickers(disponiveis)
        except Exception as e:
            print(f"[falha] cotação {id_}: {type(e).__name__}")
            continue

        for simbolo, t in tickers.items():
            if not t.get("ask") or not t.get("bid"):
                continue
            dados[simbolo].append({
                "exchange":  id_,
                "compra_em": t["ask"],   # preço pra VOCÊ comprar
                "vende_em":  t["bid"],   # preço pra VOCÊ vender
                "taxa":      taxa_taker(ex, simbolo),
            })
    return dados


def comparar(dados):
    for simbolo, cotacoes in dados.items():
        if len(cotacoes) < 2:
            print(f"\n{simbolo}: menos de 2 exchanges responderam, pulando")
            continue

        barata = min(cotacoes, key=lambda c: c["compra_em"])
        cara   = max(cotacoes, key=lambda c: c["vende_em"])

        bruto = (cara["vende_em"] - barata["compra_em"]) / barata["compra_em"]
        # o que sobra depois da comissão nas duas pontas
        custo    = barata["compra_em"] * (1 + barata["taxa"])
        recebido = cara["vende_em"]    * (1 - cara["taxa"])
        liquido  = (recebido - custo) / custo

        print(f"\n=== {simbolo} ===")
        for c in sorted(cotacoes, key=lambda c: c["compra_em"]):
            print(f"  {c['exchange']:<10} compra {c['compra_em']:>12,.2f} | vende {c['vende_em']:>12,.2f}")
        print(f"  -> mais barata: {barata['exchange']} ({barata['compra_em']:,.2f})")
        print(f"  -> mais cara:   {cara['exchange']} ({cara['vende_em']:,.2f})")
        print(f"  -> spread bruto:   {bruto * 100:+.3f}%")
        print(f"  -> spread líquido: {liquido * 100:+.3f}%  (já com comissões)")


if __name__ == "__main__":
    exchanges = conectar(EXCHANGES)
    comparar(coletar(exchanges, SIMBOLOS))