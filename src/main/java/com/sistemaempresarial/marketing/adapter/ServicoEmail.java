package com.sistemaempresarial.marketing.adapter;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.port.ServicoMensagem;

/**
 * Adaptador do canal E-mail.
 * Produção: Amazon SES (menor custo; bounce/reclamação via SNS -> ProcessadorEntrega).
 */
public class ServicoEmail implements ServicoMensagem {

    @Override
    public ResultadoEnvio enviar(Mensagem mensagem) {
        // TODO(produção): chamar o SDK do provedor e devolver o id real.
        // Erro de rede/timeout -> lançar RuntimeException; o Disparador
        // converte em FALHA_TEMPORARIA (elegível a retry).
        String idProvedor = "Email-" + Integer.toHexString(mensagem.chaveIdempotencia().hashCode());
        System.out.printf("[%s] -> %s | %s%n", canal(), mensagem.destinatario(), mensagem.conteudo());
        return ResultadoEnvio.aceito(canal(), idProvedor);
    }

    @Override public String canal() { return "E-mail"; }
}