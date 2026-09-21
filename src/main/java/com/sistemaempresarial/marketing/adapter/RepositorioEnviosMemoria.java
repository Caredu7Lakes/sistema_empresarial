package com.sistemaempresarial.marketing.adapter;

import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.model.StatusEnvio;
import com.sistemaempresarial.marketing.port.RepositorioEnvios;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Implementação em memória; simula o índice único (chave + dia). */
public class RepositorioEnviosMemoria implements RepositorioEnvios {
    private final Set<String> enviados = ConcurrentHashMap.newKeySet();

    private String k(String chave, LocalDate dia) { return chave + "#" + dia; }

    @Override public boolean jaEnviadoHoje(String chaveIdempotencia, LocalDate dia) {
        return enviados.contains(k(chaveIdempotencia, dia));
    }
    @Override public void registrar(String chave, LocalDate dia, ResultadoEnvio r) {
        // Só marca como "enviado" quando o provedor aceitou; falhas podem ser reprocessadas.
        if (r.status() == StatusEnvio.ACEITO) enviados.add(k(chave, dia));
    }
}
