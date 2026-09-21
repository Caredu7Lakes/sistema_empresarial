package com.sistemaempresarial.bootstrap;

/**
 * Entrypoint de PRODUÇÃO. Constrói o contexto (que valida as variáveis de
 * ambiente, abre o pool RDS e o cliente DynamoDB) e o fecha no fim.
 *
 * try-with-resources garante o close() do pool/cliente mesmo em falha.
 * A camada de ENTREGA (REST/API Gateway+Lambda) consome ctx.disparador(),
 * ctx.processadorEntrega() e ctx.caixa() — decisão do próximo passo.
 */
public final class Aplicacao {

    public static void main(String[] args) {
        try (ContextoAplicacao ctx = new ContextoAplicacao()) {
            System.out.println("Contexto de producao iniciado (RDS + DynamoDB).");
            // ctx.disparador() / ctx.caixa() / ctx.processadorEntrega() prontos.
        }
    }
}