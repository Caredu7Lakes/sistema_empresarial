package com.sistemaempresarial.marketing.service;

import com.sistemaempresarial.marketing.model.*;
import com.sistemaempresarial.marketing.port.*;
import com.sistemaempresarial.marketing.adapter.*;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testa os nÃ³s sensÃ­veis do disparo: supressÃ£o, idempotÃªncia e isolamento de falha. */
class DisparadorMarketingTest {

    // Clock fixo -> a regra "mesmo dia" Ã© determinÃ­stica.
    private final Clock clockFixo = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC"));

    /** Canal-espiÃ£o: conta quantas vezes o provedor foi realmente chamado. */
    private static class CanalEspiao implements ServicoMensagem {
        final AtomicInteger chamadas = new AtomicInteger();
        final boolean falha;
        CanalEspiao(boolean falha) { this.falha = falha; }
        @Override public ResultadoEnvio enviar(Mensagem m) {
            chamadas.incrementAndGet();
            if (falha) throw new RuntimeException("timeout");
            return ResultadoEnvio.aceito(canal(), "id-1");
        }
        @Override public String canal() { return "ESPIAO"; }
    }

    private DisparadorMarketing novo(ListaSupressao s, ServicoMensagem c) {
        return new DisparadorMarketing(new RepositorioEnviosMemoria(), s, clockFixo).registrar(c);
    }

    @Test @DisplayName("Envio vÃ¡lido Ã© ACEITO e chama o provedor uma vez")
    void envioAceito() {
        CanalEspiao c = new CanalEspiao(false);
        var res = novo(new ListaSupressaoMemoria(), c).disparar(new Mensagem("ana@mail.com", "oi"));
        assertEquals(StatusEnvio.ACEITO, res.get(0).status());
        assertEquals(1, c.chamadas.get());
    }

    @Test @DisplayName("Mesma mensagem no mesmo dia -> DUPLICADO e NÃƒO reenvia")
    void idempotenciaDiaria() {
        CanalEspiao c = new CanalEspiao(false);
        DisparadorMarketing d = novo(new ListaSupressaoMemoria(), c);
        Mensagem m = new Mensagem("ana@mail.com", "promo");
        d.disparar(m);
        List<ResultadoEnvio> segundo = d.disparar(m);
        assertEquals(StatusEnvio.DUPLICADO, segundo.get(0).status());
        assertEquals(1, c.chamadas.get(), "provedor nÃ£o pode ser chamado 2x");
    }

    @Test @DisplayName("DestinatÃ¡rio suprimido -> SUPRIMIDO e provedor nÃ£o Ã© chamado")
    void destinatarioSuprimido() {
        CanalEspiao c = new CanalEspiao(false);
        ListaSupressao s = new ListaSupressaoMemoria();
        s.suprimir("ana@mail.com", "bounce");
        var res = novo(s, c).disparar(new Mensagem("ana@mail.com", "oi"));
        assertEquals(StatusEnvio.SUPRIMIDO, res.get(0).status());
        assertEquals(0, c.chamadas.get());
    }

    @Test @DisplayName("Falha em um canal nÃ£o impede os demais (isolamento)")
    void isolamentoDeFalha() {
        CanalEspiao ok = new CanalEspiao(false);
        CanalEspiao quebrado = new CanalEspiao(true);
        DisparadorMarketing d = new DisparadorMarketing(
                new RepositorioEnviosMemoria(), new ListaSupressaoMemoria(), clockFixo)
                .registrar(quebrado).registrar(ok);
        var res = d.disparar(new Mensagem("ana@mail.com", "oi"));
        assertEquals(StatusEnvio.FALHA_TEMPORARIA, res.get(0).status());
        assertEquals(StatusEnvio.ACEITO, res.get(1).status());
        assertEquals(1, ok.chamadas.get());
    }
    @Test @DisplayName("Falha temporaria libera a chave para reenvio no mesmo dia")
    void falhaLiberaReenvio() {
        ServicoMensagem instavel = new ServicoMensagem() {
            int n = 0;
            @Override public ResultadoEnvio enviar(Mensagem m) {
                if (n++ == 0) throw new RuntimeException("timeout");
                return ResultadoEnvio.aceito(canal(), "id");
            }
            @Override public String canal() { return "INSTAVEL"; }
        };
        DisparadorMarketing d = new DisparadorMarketing(
                new RepositorioEnviosMemoria(), new ListaSupressaoMemoria(), clockFixo)
                .registrar(instavel);
        Mensagem m = new Mensagem("ana@mail.com", "oi");
        assertEquals(StatusEnvio.FALHA_TEMPORARIA, d.disparar(m).get(0).status());
        assertEquals(StatusEnvio.ACEITO, d.disparar(m).get(0).status());
    }
}