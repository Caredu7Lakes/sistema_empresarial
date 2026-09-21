package com.sistemaempresarial.marketing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Nó sensível: quando um evento do SES deve (ou não) suprimir o destinatário. */
class PoliticaSupressaoSesTest {

    @Test @DisplayName("Reclamação sempre suprime")
    void reclamacao() {
        assertEquals(Optional.of(ProcessadorEntrega.Evento.RECLAMACAO),
                PoliticaSupressaoSes.decidir("Complaint", ""));
    }

    @Test @DisplayName("Bounce permanente suprime")
    void bouncePermanente() {
        assertEquals(Optional.of(ProcessadorEntrega.Evento.BOUNCE),
                PoliticaSupressaoSes.decidir("Bounce", "Permanent"));
    }

    @Test @DisplayName("Bounce transitório NÃO suprime")
    void bounceTransient() {
        assertTrue(PoliticaSupressaoSes.decidir("Bounce", "Transient").isEmpty());
    }

    @Test @DisplayName("Entrega/tipo desconhecido não suprime")
    void outros() {
        assertTrue(PoliticaSupressaoSes.decidir("Delivery", "").isEmpty());
        assertTrue(PoliticaSupressaoSes.decidir("Qualquer", "Permanent").isEmpty());
    }
}