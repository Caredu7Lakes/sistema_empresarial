package com.sistemaempresarial.marketing.port;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;

/**
 * Porta de saída: contrato único que todo canal implementa.
 * O núcleo depende desta interface, nunca de SES/SNS/Meta diretamente.
 */
public interface ServicoMensagem {
    /** Envia e devolve o resultado síncrono (aceite do provedor). */
    ResultadoEnvio enviar(Mensagem mensagem);

    /** Nome do canal, para log e relatórios. */
    String canal();
}
