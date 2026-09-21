package com.sistemaempresarial.entrega.lambda;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sistemaempresarial.bootstrap.ContextoAplicacao;
import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;

public class DispararCampanhaHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ContextoAplicacao CTX = new ContextoAplicacao();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent req, Context ctx) {
        try {
            JsonNode body = JSON.readTree(req.getBody());
            Mensagem msg = new Mensagem(
                    body.path("destinatario").asText(null),
                    body.path("conteudo").asText(null));

            List<ResultadoEnvio> resultados = CTX.disparador().disparar(msg);

            List<Map<String, String>> corpo = resultados.stream()
                    .map(r -> Map.of("canal", r.canal(), "status", r.status().name()))
                    .collect(Collectors.toList());

            return resposta(200, JSON.writeValueAsString(corpo));

        } catch (IllegalArgumentException e) {
            return resposta(400, "{\"erro\":\"" + safe(e.getMessage()) + "\"}");
        } catch (Exception e) {
            ctx.getLogger().log("Erro no disparo: " + e);
            return resposta(500, "{\"erro\":\"falha interna\"}");
        }
    }

    private APIGatewayProxyResponseEvent resposta(int status, String corpo) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(corpo);
    }

    private static String safe(String s) {
        return s == null ? "requisicao invalida" : s.replace("\"", "'");
    }
}