from banco import conectar_banco

conn = conectar_banco()
with conn.cursor() as cur:
    cur.execute("SELECT count(*) FROM precos")
    print("conectou, linhas em precos:", cur.fetchone()[0])
conn.close()