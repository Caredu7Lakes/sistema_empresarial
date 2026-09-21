package com.sistemaempresarial.marketing.adapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.model.StatusEnvio;
import com.sistemaempresarial.marketing.port.ServicoMensagem;

/**
 * Adapter REAL de WhatsApp via Meta Cloud API (Graph API).
 *
 * Diferente de SES/SNS, a Meta nÃƒÂ£o tem SDK AWS Ã¢â‚¬â€ ÃƒÂ© REST HTTPS. Usamos o
 * HttpClient do JDK (sem dependÃƒÂªncia extra) e Jackson para o JSON.
 *
 * IMPORTANTE (regra da Meta): para INICIAR conversa (marketing/outbound) fora da
 * janela de 24h, sÃƒÂ³ ÃƒÂ© permitido enviar mensagem de TEMPLATE previamente aprovado.
 * Texto livre sÃƒÂ³ funciona dentro da janela de atendimento. Por isso enviamos um
 * template com UMA variÃƒÂ¡vel de corpo ({{1}}), preenchida com Mensagem.conteudo().
 *
 * DependÃƒÂªncias injetadas (composition root):
 *   - http:          HttpClient compartilhado.
 *   - baseUrl:       ex. https://graph.facebook.com/v21.0
 *   - phoneNumberId: ID do nÃƒÂºmero emissor (do WhatsApp Business).
 *   - accessToken:   token do system user (SEGREDO -> Secrets Manager).
 *   - template:      nome do template aprovado (com um {{1}} no corpo).
 *   - idioma:        cÃƒÂ³digo de idioma do template, ex. pt_BR.
 */
public class ServicoWhatsAppMeta implements ServicoMensagem {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final String url;          // baseUrl + "/" + phoneNumberId + "/messages"
    private final String accessToken;
    private final String template;
    private final String idioma;

    public ServicoWhatsAppMeta(HttpClient http, String baseUrl, String phoneNumberId,
                               String accessToken, String template, String idioma) {
        this.http = http;
        this.url = baseUrl + "/" + phoneNumberId + "/messages";
        this.accessToken = accessToken;
        this.template = template;
        this.idioma = idioma;
    }

    @Override
    public ResultadoEnvio enviar(Mensagem mensagem) {
        try {
            String corpoJson = montarPayload(mensagem);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoJson))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();

            if (status / 100 == 2) {
                // 2xx: aceite sÃƒÂ­ncrono. Extrai o wamid (id da mensagem na Meta).
                JsonNode raiz = JSON.readTree(resp.body());
                String id = raiz.path("messages").path(0).path("id").asText(null);
                return ResultadoEnvio.aceito(canal(), id);
            }

            if (status >= 400 && status < 500) {
                // 4xx: erro definitivo (nÃƒÂºmero invÃƒÂ¡lido, template errado, token). NÃƒÂ£o faz retry.
                return ResultadoEnvio.de(canal(), StatusEnvio.REJEITADO, resumoErro(resp.body(), status));
            }

            // 5xx: indisponibilidade da Meta -> transitÃƒÂ³rio -> retry (Disparador libera a reserva).
            throw new RuntimeException("Meta indisponivel (HTTP " + status + ")");

        } catch (java.io.IOException e) {
            // Falha de rede/timeout -> transitÃƒÂ³rio.
            throw new RuntimeException("Falha de rede no WhatsApp/Meta", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // boa prÃƒÂ¡tica: restaura o flag de interrupÃƒÂ§ÃƒÂ£o
            throw new RuntimeException("Envio WhatsApp interrompido", e);
        }
    }

    /** Monta o JSON de mensagem de template com uma variÃƒÂ¡vel de corpo. */
    private String montarPayload(Mensagem mensagem) {
        ObjectNode raiz = JSON.createObjectNode();
        raiz.put("messaging_product", "whatsapp");
        raiz.put("to", mensagem.destinatario());   // ex.: 5511999999999
        raiz.put("type", "template");

        ObjectNode tpl = raiz.putObject("template");
        tpl.put("name", template);
        tpl.putObject("language").put("code", idioma);

        // components -> body -> parameters: [ {type:text, text: <conteudo>} ]
        ArrayNode components = tpl.putArray("components");
        ObjectNode body = components.addObject();
        body.put("type", "body");
        ObjectNode param = body.putArray("parameters").addObject();
        param.put("type", "text");
        param.put("text", mensagem.conteudo());

        return raiz.toString();
    }

    private String resumoErro(String corpo, int status) {
        try {
            JsonNode msg = JSON.readTree(corpo).path("error").path("message");
            return msg.isMissingNode() ? "HTTP " + status : msg.asText();
        } catch (Exception e) {
            return "HTTP " + status;
        }
    }

    @Override
    public String canal() { return "WhatsApp"; }
}