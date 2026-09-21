package com.sistemaempresarial.marketing.service;

import com.sistemaempresarial.marketing.port.*;
import com.sistemaempresarial.marketing.adapter.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testa a reação às confirmações assíncronas (bounce/reclamação/entrega). */
class ProcessadorEntregaTest {

    @Test @DisplayName("BOUNCE (e-mail inexistente) suprime o destinatário")
    void bounceSuprime() {
        ListaSupressao s = new ListaSupressaoMemoria();
        new ProcessadorEntrega(s).processar("x@mail.com", ProcessadorEntrega.Evento.BOUNCE, "no such user");
        assertTrue(s.suprimido("x@mail.com"));
    }

    @Test @DisplayName("RECLAMAÇÃO de spam suprime o destinatário")
    void reclamacaoSuprime() {
        ListaSupressao s = new ListaSupressaoMemoria();
        new ProcessadorEntrega(s).processar("x@mail.com", ProcessadorEntrega.Evento.RECLAMACAO, "spam");
        assertTrue(s.suprimido("x@mail.com"));
    }

    @Test @DisplayName("ENTREGUE não suprime")
    void entregueNaoSuprime() {
        ListaSupressao s = new ListaSupressaoMemoria();
        new ProcessadorEntrega(s).processar("x@mail.com", ProcessadorEntrega.Evento.ENTREGUE, "ok");
        assertFalse(s.suprimido("x@mail.com"));
    }
}
