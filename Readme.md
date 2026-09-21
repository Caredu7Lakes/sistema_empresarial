# projeto003 - Sistema Empresarial

Duas features em camadas (Clean/Hexagonal): marketing (envio multicanal) e vendas (codigo de barras + imposto).

## Estrutura
    src/main/java/com/sistemaempresarial/
      Main.java
      marketing/  -> model, port, service, adapter
      vendas/     -> model, port, service, adapter
    src/main/resources/db/migration/   (Flyway: V1__init.sql)
    src/test/java/com/sistemaempresarial/...

Regra de dependencia: adapter -> port -> service -> model.

## Persistencia
- RDS PostgreSQL: catalogo, produtos, vendas (schema versionado via Flyway).
- DynamoDB: log de envio, idempotencia e supressao (TTL diario). [proximo passo]
- Catalogo lido pela porta CatalogoProdutos (adapters: CatalogoProdutosMemoria, CatalogoProdutosJdbc).

## Rodar
    mvn test
    $env:FLYWAY_URL="jdbc:postgresql://<host>:5432/<db>"
    $env:FLYWAY_USER="<user>"
    $env:FLYWAY_PASSWORD="<senha>"
    mvn flyway:migrate

## Comportamentos garantidos
- Envio: confirmacao sincrona + assincrona (webhook), supressao por bounce, idempotencia diaria por canal, isolamento de falha.
- Imposto: aliquota por tipo + ajuste estadual, piso em zero.