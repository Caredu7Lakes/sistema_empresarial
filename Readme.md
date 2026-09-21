# Pequena Empresa

Duas features independentes — **marketing** (envio multicanal) e **vendas** (código de barras + imposto) — organizadas em camadas (Clean/Hexagonal).

## Camadas por feature
```
marketing/
  model/    Mensagem, ResultadoEnvio, StatusEnvio        # entidades e VOs
  port/     ServicoMensagem, RepositorioEnvios, ListaSupressao   # interfaces
  service/  DisparadorMarketing, ProcessadorEntrega      # regras de negócio
  adapter/  Servico{Sms,Email,RedesSociais,WhatsApp}, *Memoria   # implementações
vendas/
  model/    Produto, TipoProduto
  service/  Caixa, LeitorCodigoBarras
```

**Regra de dependência:** `adapter → port → service → model`. O `model` não depende de nada; o núcleo (service) não conhece AWS — só as portas.

## Rodar
```bash
mvn test          # testes JUnit 5 (nós sensíveis)
mvn -q compile exec:java -Dexec.mainClass=Main   # demo (ou rode Main pela IDE)
```

## Comportamentos garantidos
- Confirmação síncrona (`ResultadoEnvio`) + assíncrona via webhook (`ProcessadorEntrega`).
- Bounce/reclamação → `ListaSupressao` (não reenvia a endereço inválido).
- Idempotência por pessoa + mensagem + canal + dia (não duplica no dia).
- Imposto por categoria + ajuste estadual, com piso em zero.

## Produção (AWS)
- Catálogo/vendas/fiscal: **RDS PostgreSQL**.
- Log de envio + idempotência + supressão: **DynamoDB** (conditional put + TTL diário).
- E-mail **SES**, SMS **SNS**; WhatsApp **Meta Cloud API**.