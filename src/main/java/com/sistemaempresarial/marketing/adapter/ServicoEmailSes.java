package com.sistemaempresarial.marketing.adapter;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.model.StatusEnvio;
import com.sistemaempresarial.marketing.port.ServicoMensagem;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * Adapter REAL de e-mail via Amazon SES v2.
 *
 * Implementa a mesma porta ServicoMensagem dos stubs — então o DisparadorMarketing
 * não sabe (nem precisa saber) que agora é SES de verdade. Trocar provedor = trocar
 * o adapter injetado na composition root; o núcleo não muda.
 *
 * Dependências injetadas:
 *   - SesV2Client: cliente da AWS (criado uma vez na composition root).
 *   - remetente:   e-mail "From" verificado no SES (ex.: no-reply@seudominio.com).
 *   - assunto:     assunto fixo por ora (Mensagem só carrega o corpo; assunto por
 *                  campanha exigiria estender Mensagem — decisão futura).
 */
public class ServicoEmailSes implements ServicoMensagem {

    private final SesV2Client ses;
    private final String remetente;
    private final String assunto;

    public ServicoEmailSes(SesV2Client ses, String remetente, String assunto) {
        this.ses = ses;
        this.remetente = remetente;
        this.assunto = assunto;
    }

    @Override
    public ResultadoEnvio enviar(Mensagem mensagem) {
        try {
            SendEmailResponse resp = ses.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(remetente)
                    .destination(Destination.builder()
                            .toAddresses(mensagem.destinatario())
                            .build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data(assunto).build())
                                    .body(Body.builder()
                                            .text(Content.builder().data(mensagem.conteudo()).build())
                                            .build())
                                    .build())
                            .build())
                    .build());

            // Aceite SÍNCRONO: o SES recebeu e devolveu um messageId. A entrega real
            // (ou o bounce) chega depois por SNS -> BounceHandler.
            return ResultadoEnvio.aceito(canal(), resp.messageId());

        } catch (MessageRejectedException e) {
            // Rejeição definitiva (conteúdo/endereço) — não adianta retry hoje.
            return ResultadoEnvio.de(canal(), StatusEnvio.REJEITADO, e.awsErrorDetails().errorMessage());

        } catch (SesV2Exception e) {
            // Erro transitório (throttling, indisponibilidade). Relança como runtime:
            // o Disparador captura, LIBERA a reserva e marca FALHA_TEMPORARIA (retry no dia).
            throw new RuntimeException("Falha transitoria no SES", e);
        }
    }

    @Override
    public String canal() { return "E-mail"; }
}