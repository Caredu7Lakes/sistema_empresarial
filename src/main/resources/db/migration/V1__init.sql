-- Catálogo de produtos (a alíquota base por tipo vive no enum TipoProduto;
-- a tabela guarda a categoria + o ajuste estadual do produto).
CREATE TABLE produto (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_barras   VARCHAR(14)   NOT NULL UNIQUE,
    descricao       VARCHAR(200)  NOT NULL,
    tipo            VARCHAR(20)   NOT NULL
        CHECK (tipo IN ('ALIMENTACAO','SAUDE_BEM_ESTAR','VESTUARIO','CULTURA')),
    valor_venda     NUMERIC(12,2) NOT NULL CHECK (valor_venda >= 0),
    ajuste_estadual NUMERIC(6,4)  NOT NULL DEFAULT 0,  -- delta sobre a alíquota
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Cabeçalho da venda.
CREATE TABLE venda (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    criada_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    total_venda   NUMERIC(12,2) NOT NULL CHECK (total_venda   >= 0),
    total_imposto NUMERIC(12,2) NOT NULL CHECK (total_imposto >= 0)
);

-- Itens da venda com SNAPSHOT fiscal: congela valor, alíquota e imposto
-- no momento da venda (o catálogo pode mudar depois; o histórico fiscal, não).
CREATE TABLE venda_item (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venda_id         BIGINT        NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    produto_id       BIGINT        NOT NULL REFERENCES produto(id),
    codigo_barras    VARCHAR(14)   NOT NULL,
    descricao        VARCHAR(200)  NOT NULL,
    tipo             VARCHAR(20)   NOT NULL,
    valor_venda      NUMERIC(12,2) NOT NULL,
    aliquota_efetiva NUMERIC(6,4)  NOT NULL,
    imposto          NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_venda_item_venda ON venda_item(venda_id);sim