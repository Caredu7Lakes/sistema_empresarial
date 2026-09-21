package com.sistemaempresarial.marketing.port;

import com.sistemaempresarial.marketing.model.ResultadoEnvio;

import java.time.LocalDate;

/**
 * Porta: registro de envios. Garante idempotência diária e serve de log/auditoria.
 *
 * Produção (AWS): DynamoDB com chave = "chaveIdempotencia#dia",
 * gravação condicional (attribute_not_exists) para atomicidade e
 * TTL no fim do dia para expiração automática da janela de deduplicação.
 * Alternativa relacional: índice UNIQUE (destinatario, hash, dia) no RDS.
 */
public interface RepositorioEnvios {
    /** true se a mesma mensagem já foi enviada a este destinatário no dia. */
    boolean jaEnviadoHoje(String chaveIdempotencia, LocalDate dia);

    /** Persiste o resultado (para auditoria e para bloquear duplicidade). */
    void registrar(String chaveIdempotencia, LocalDate dia, ResultadoEnvio resultado);
}
