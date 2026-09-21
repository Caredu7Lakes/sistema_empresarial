package com.sistemaempresarial.entrega.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sistemaempresarial.bootstrap.ContextoAplicacao;
import com.sistemaempresarial.marketing.service.ProcessadorEntrega;
import com.sistemaempresarial.marketing.service.PoliticaSupressaoSes;

/**
 * Handler de BOUNCE / RECLAMAÇÃO.
 *
 * Caminho do evento:
 *   SES detecta bounce/reclamação -> publica no tópico SNS -> SNS invoca este Lambda.
 *
 * O corpo de cada registro SNS é um JSON de notificação do SES. Aqui a gente lê
 * o tipo e os destinatários afetados e delega ao ProcessadorEntrega, que grava
 * na ListaSupressao (DynamoDB). Assim, endereço problemático nunca mais é disparado.
 *
 * Formato resumido da notificação do SES (dentro de Sns.Message):
 *   { "notificationType": "Bounce",
 *     "bounce": { "bounceType": "Permanent",
 *                 "bouncedRecipients": [ { "emailAddress": "x@dominio.com" } ] } }
 *   { "notificationType": "Complaint",
 *     "complaint": { "complainedRecipients": [ { "emailAddress": "y@dominio.com" } ] } }
 */
public class BounceHandler implements RequestHandler<SNSEvent, Void> {

    // Criado UMA vez por container (fase INIT): reusa cliente DynamoDB entre invocações.
    private static final ContextoAplicacao CTX = new ContextoAplicacao();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Void handleRequest(SNSEvent evento, Context ctx) {
        // Um mesmo disparo do SNS pode trazer vários registros — processa todos.
        for (SNSEvent.SNSRecord registro : evento.getRecords()) {
            try {
                processarNotificacao(registro.getSNS().getMessage(), ctx);
            } catch (Exception e) {
                // Não relança: um registro ruim não deve derrubar os demais do lote.
                // (Em produção, um bounce não processado vai para DLQ via config do Lambda.)
                ctx.getLogger().log("Falha ao processar notificacao SES: " + e);
            }
        }
        return null;
    }

    /** Lê o JSON do SES e suprime os endereços conforme o tipo do evento. */
    private void processarNotificacao(String mensagemJson, Context ctx) throws Exception {
        JsonNode raiz = JSON.readTree(mensagemJson);
        String tipo = raiz.path("notificationType").asText("");
        ProcessadorEntrega processador = CTX.processadorEntrega();

        if ("Bounce".equals(tipo)) {
            JsonNode bounce = raiz.path("bounce");
            // A política decide se este bounce deve suprimir (só permanente).
            PoliticaSupressaoSes.decidir("Bounce", bounce.path("bounceType").asText(""))
                .ifPresent(evento -> {
                    for (JsonNode r : bounce.path("bouncedRecipients")) {
                        String email = r.path("emailAddress").asText(null);
                        if (email != null) processador.processar(email, evento, "SES bounce permanente");
                    }
                });

        } else if ("Complaint".equals(tipo)) {
            PoliticaSupressaoSes.decidir("Complaint", "")
                .ifPresent(evento -> {
                    for (JsonNode r : raiz.path("complaint").path("complainedRecipients")) {
                        String email = r.path("emailAddress").asText(null);
                        if (email != null) processador.processar(email, evento, "SES reclamacao");
                    }
                });

        } else {
            ctx.getLogger().log("Notificacao SES ignorada (tipo=" + tipo + ")");
        }
    }
}
