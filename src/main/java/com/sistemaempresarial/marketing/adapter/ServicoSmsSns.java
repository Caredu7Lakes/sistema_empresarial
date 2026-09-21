package com.sistemaempresarial.marketing.adapter;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.model.StatusEnvio;
import com.sistemaempresarial.marketing.port.ServicoMensagem;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * Adapter REAL de SMS via Amazon SNS.
 *
 * Implementa a mesma porta ServicoMensagem — o DisparadorMarketing não muda.
 * SNS envia SMS direto para um número (E.164, ex.: +5511999999999) sem tópico.
 *
 * Dependência injetada:
 *   - SnsClient: criado uma vez na composition root.
 */
public class ServicoSmsSns implements ServicoMensagem {

    private final SnsClient sns;

    public ServicoSmsSns(SnsClient sns) {
        this.sns = sns;
    }

    @Override
    public ResultadoEnvio enviar(Mensagem mensagem) {
        try {
            PublishResponse resp = sns.publish(PublishRequest.builder()
                    .phoneNumber(mensagem.destinatario()) // formato E.164: +55DDDNUMERO
                    .message(mensagem.conteudo())
                    .build());

            // Aceite síncrono: o SNS aceitou e devolveu um messageId.
            return ResultadoEnvio.aceito(canal(), resp.messageId());

        } catch (InvalidParameterException e) {
            // Número/parâmetro inválido -> definitivo, sem retry.
            return ResultadoEnvio.de(canal(), StatusEnvio.REJEITADO, e.awsErrorDetails().errorMessage());

        } catch (SnsException e) {
            // Throttling/indisponibilidade -> transitório -> Disparador libera e marca FALHA_TEMPORARIA.
            throw new RuntimeException("Falha transitoria no SNS", e);
        }
    }

    @Override
    public String canal() { return "SMS"; }
}