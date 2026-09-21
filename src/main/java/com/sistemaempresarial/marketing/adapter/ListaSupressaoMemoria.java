package com.sistemaempresarial.marketing.adapter;

import com.sistemaempresarial.marketing.port.ListaSupressao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Implementação em memória para testes e desenvolvimento. */
public class ListaSupressaoMemoria implements ListaSupressao {
    private final Map<String, String> bloqueados = new ConcurrentHashMap<>();

    @Override public boolean suprimido(String destinatario) {
        return bloqueados.containsKey(destinatario);
    }
    @Override public void suprimir(String destinatario, String motivo) {
        bloqueados.put(destinatario, motivo == null ? "" : motivo);
    }
}
