package com.sistemaempresarial.marketing.service;

import java.util.Optional;

/**
 * Decide, a partir do tipo de notificação do SES, se e como suprimir o destinatário.
 * Regra isolada aqui (sem JSON, sem AWS) para ser testável de forma exaustiva.
 *
 *   - Reclamação (spam)        -> RECLAMACAO (sempre suprime).
 *   - Bounce PERMANENTE        -> BOUNCE (endereço inexistente; suprime).
 *   - Bounce Transient/entrega -> vazio (temporário; NÃO suprime, pode reenviar).
 */
public final class PoliticaSupressaoSes {

    private PoliticaSupressaoSes() {}

    public static Optional<ProcessadorEntrega.Evento> decidir(String notificationType, String bounceType) {
        if ("Complaint".equals(notificationType)) {
            return Optional.of(ProcessadorEntrega.Evento.RECLAMACAO);
        }
        if ("Bounce".equals(notificationType) && "Permanent".equals(bounceType)) {
            return Optional.of(ProcessadorEntrega.Evento.BOUNCE);
        }
        return Optional.empty();
    }
}