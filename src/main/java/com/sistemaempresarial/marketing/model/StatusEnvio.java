package com.sistemaempresarial.marketing.model;

/** Resultado SÍNCRONO da tentativa de envio (o provedor aceitou ou não). */
public enum StatusEnvio {
    ACEITO,            // provedor aceitou; entrega confirmada depois por webhook
    DUPLICADO,         // já enviado hoje para este destinatário -> não reenviado
    SUPRIMIDO,         // destinatário em lista de supressão (bounce/reclamação)
    REJEITADO,         // provedor recusou (ex.: formato inválido)
    FALHA_TEMPORARIA   // erro transitório (rede/timeout) -> elegível a retry
}
