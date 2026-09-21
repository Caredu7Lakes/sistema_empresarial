# projeto003 — Sistema Empresarial

Ferramenta de **marketing multicanal** + **PDV com apuração de imposto** para pequenas empresas.
Java 21, arquitetura hexagonal (ports & adapters), rodando em AWS serverless.

O objetivo do desenho é um só: **trocar infraestrutura sem tocar em regra de negócio.**
Memória hoje, RDS/DynamoDB amanhã, outro provedor depois — o núcleo nem fica sabendo.

---

## Como o projeto pensa (arquitetura)

Cada feature é fatiada em 4 camadas. A dependência aponta **sempre para dentro**:

    adapter  ->  port  ->  service  ->  model

- **model** — entidades e regras puras (imposto, mensagem). Não depende de nada.
- **service** — orquestra o caso de uso. Depende só de *interfaces* (ports).
- **port** — o contrato ("busque um produto", "reserve um envio"). O núcleo só conhece isto.
- **adapter** — o mundo real: PostgreSQL, DynamoDB, SES. Plugável, descartável, testável.

Regra de ouro: **o núcleo não importa nada de infra.** Quem conhece AWS é só o `adapter` e o
`bootstrap`. Isso é o que permite testar 100% do miolo em memória, sem subir banco.

### Mapa das features

    marketing/                         vendas/
      model/   Mensagem, Resultado       model/   Produto, TipoProduto
      port/    ServicoMensagem            port/    CatalogoProdutos
               RepositorioEnvios          service/ Caixa, LeitorCodigoBarras
               ListaSupressao             adapter/ CatalogoProdutosJdbc  (RDS)
      service/ DisparadorMarketing                 CatalogoProdutosMemoria
               ProcessadorEntrega                  ProdutoMapper
      adapter/ ServicoSms/Email/...
               RepositorioEnviosDynamo   bootstrap/
               ListaSupressaoDynamo        ContextoAplicacao  (composition root)
               *Memoria                    Aplicacao          (entrypoint prod)

    entrega/lambda/  DispararCampanhaHandler  (API Gateway -> Lambda)

---

## Feature: marketing

Envia a **mesma mensagem** para N canais (SMS, E-mail, Redes Sociais, WhatsApp) e garante:

- **Confirmação em 2 níveis** — síncrona (o provedor *aceitou*) + assíncrona (entregue/bounce
  chega depois por webhook, tratada pelo `ProcessadorEntrega`).
- **Supressão** — e-mail que deu bounce (não existe) ou reclamação de spam entra na
  `ListaSupressao` e **nunca mais** é disparado.
- **Idempotência diária atômica** — a mesma mensagem, pra mesma pessoa, no mesmo canal, no mesmo
  dia, não sai duas vezes. E é *atômica de verdade*: **reserva antes de enviar** (ver decisões).
- **Isolamento de falha** — um canal cair não derruba os outros.

## Feature: vendas

Leitura por **código de barras** -> `Produto` -> imposto.

- Alíquota por categoria: Alimentação 1% · Saúde 1,5% · Vestuário 2,5% · Cultura 4%.
- **Ajuste estadual** (acréscimo/desconto) somado à alíquota, com **piso em zero** (desconto
  grande nunca vira imposto negativo).
- Dinheiro é `BigDecimal` com `HALF_UP` — nada de `double` em cálculo fiscal.

---

## Infra e as escolhas (custo x benefício)

| Necessidade | Serviço | Por que este |
|---|---|---|
| Catálogo, produtos, vendas | **RDS PostgreSQL** | ACID e relacional — obrigação fiscal exige histórico confiável |
| Idempotência + supressão | **DynamoDB** (on-demand) | `conditional put` atômico + **TTL** que limpa a janela do dia sozinho |
| Schema | **Flyway** | migrations versionadas, sem DDL na mão |
| E-mail / SMS | **SES / SNS** | menor custo; bounce/reclamação nativos via SNS |
| Entrega HTTP | **API Gateway + Lambda** | serverless: barato em pico, casa nativo com SNS (bounce) |
| Credenciais | **Secrets Manager -> env** | zero segredo no código; runtime injeta na env var |
| Pool de conexão | **HikariCP** | pool rápido e enxuto, reusado entre invocações do Lambda |

**Por que serverless (Lambda) e não container?** Carga de marketing é em picos (campanha dispara,
depois silêncio). Pagar container ocioso não faz sentido; Lambda cobra por invocação e escala
sozinho. O trade-off (cold start) é aceitável pro caso.

---

## Decisões que valem explicar

- **Idempotência = reservar-antes-de-enviar.** A versão ingênua (checa -> envia -> grava) tem
  janela de corrida: dois processos checam "não enviado" ao mesmo tempo e enviam os dois. A
  correção real é **reservar primeiro** (`PutItem` condicional): quem não reservou, não envia.
  Em falha transitória a reserva é **liberada** pra permitir retry no mesmo dia.
- **Snapshot fiscal em `venda_item`.** O item congela valor, alíquota e imposto no momento da
  venda. O catálogo pode mudar amanhã; o histórico fiscal, não.
- **TTL do DynamoDB é limpeza, não regra.** Quem garante a janela diária é o `#dia` na chave; o
  TTL só varre o lixo depois. Não dá pra confiar no TTL pra correção (ele apaga em ~48h, não na hora).
- **Arquivos em UTF-8 sem BOM.** `javac` recusa BOM. No VS Code: salvar como "UTF-8", não
  "UTF-8 with BOM".

---

## Rodar

    mvn test          # 18 testes (nós sensíveis: imposto, idempotência, supressão, mapper)
    mvn -q compile    # compila tudo (baixa deps AWS/Hikari/Jackson na 1a vez)
    mvn -q package    # gera o fat jar do Lambda em target/

Aplicar o schema no Postgres:

    $env:FLYWAY_URL="jdbc:postgresql://<host>:5432/<db>"
    $env:FLYWAY_USER="<user>"
    $env:FLYWAY_PASSWORD="<senha>"
    mvn flyway:migrate

Handler do Lambda (disparo de campanha):

    com.sistemaempresarial.entrega.lambda.DispararCampanhaHandler::handleRequest

## Variáveis de ambiente (produção via Secrets Manager)

    DB_URL, DB_USER, DB_PASSWORD          # Postgres/RDS
    DYNAMO_REGION                         # ex: us-east-1
    DYNAMO_TABELA_ENVIOS                  # envio_idempotencia
    DYNAMO_TABELA_SUPRESSAO               # supressao
    APP_ZONE                              # opcional, default America/Sao_Paulo

---

## Estado atual

Feito: núcleo + testes · schema Flyway · adapter JDBC do catálogo · idempotência atômica +
adapters DynamoDB · composition root · entrypoint · handler de disparo (API Gateway).

Próximo: handler de **bounce/reclamação** (SES -> SNS -> Lambda -> supressão) e os adapters
reais de canal (SES/SNS/Meta) no lugar dos stubs.