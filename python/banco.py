import os

import psycopg2
from psycopg2.extras import execute_values

CONEXAO = dict(
    host=os.getenv("DB_HOST", "localhost"),
    port=int(os.getenv("DB_PORT", "5432")),
    dbname="cripto",
    user=os.getenv("DB_USER", "postgres"),
    password=os.environ["DB_PASSWORD"],  
)


def conectar_banco():
    return psycopg2.connect(**CONEXAO)


def salvar_precos(conn, coleta_id, simbolo, cotacoes):
    """cotacoes = [{exchange, compra_em, vende_em, taxa}, ...]"""
    if not cotacoes:
        return
    linhas = [
        (str(coleta_id), c["exchange"], simbolo,
         c["compra_em"], c["vende_em"], c["taxa"])
        for c in cotacoes
    ]
    with conn.cursor() as cur:
        execute_values(cur, """
            INSERT INTO precos
                (coleta_id, exchange, simbolo, melhor_compra, melhor_venda, taxa_taker)
            VALUES %s
        """, linhas)
    conn.commit()


def salvar_spreads(conn, coleta_id, simbolo, volume, rotas):
    """rotas = [{compra_em, vende_em, lucro}, ...]"""
    if not rotas:
        return
    linhas = [
        (str(coleta_id), simbolo, volume,
         r["compra_em"], r["vende_em"], r["lucro"] * 100)
        for r in rotas
    ]
    with conn.cursor() as cur:
        execute_values(cur, """
            INSERT INTO spreads
                (coleta_id, simbolo, volume_usdt, exchange_compra, exchange_venda, lucro_pct)
            VALUES %s
        """, linhas)
    conn.commit()