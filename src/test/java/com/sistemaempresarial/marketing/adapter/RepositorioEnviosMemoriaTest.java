package com.sistemaempresarial.marketing.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Nó sensível: reserva atômica e liberação (base da idempotência diária). */
class RepositorioEnviosMemoriaTest {

    private final LocalDate hoje = LocalDate.of(2026, 1, 15);
    private final LocalDate amanha = hoje.plusDays(1);

    @Test @DisplayName("Primeira reserva vence; a segunda (mesma chave/dia) falha")
    void reservaUnica() {
        RepositorioEnviosMemoria repo = new RepositorioEnviosMemoria();
        assertTrue(repo.reservar("k", hoje));
        assertFalse(repo.reservar("k", hoje));
    }

    @Test @DisplayName("Liberar permite reservar de novo no mesmo dia")
    void liberarReabre() {
        RepositorioEnviosMemoria repo = new RepositorioEnviosMemoria();
        assertTrue(repo.reservar("k", hoje));
        repo.liberar("k", hoje);
        assertTrue(repo.reservar("k", hoje));
    }

    @Test @DisplayName("Mesma chave em dia diferente é uma nova reserva")
    void diaDiferente() {
        RepositorioEnviosMemoria repo = new RepositorioEnviosMemoria();
        assertTrue(repo.reservar("k", hoje));
        assertTrue(repo.reservar("k", amanha));
    }
}