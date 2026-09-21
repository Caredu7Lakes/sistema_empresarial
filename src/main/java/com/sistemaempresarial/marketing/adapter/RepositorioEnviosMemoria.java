package com.sistemaempresarial.marketing.adapter;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sistemaempresarial.marketing.port.RepositorioEnvios;

public class RepositorioEnviosMemoria implements RepositorioEnvios {
    private final Set<String> reservados = ConcurrentHashMap.newKeySet();

    private String k(String chave, LocalDate dia) { return chave + "#" + dia; }

    @Override public boolean reservar(String chave, LocalDate dia) {
        return reservados.add(k(chave, dia));
    }
    @Override public void liberar(String chave, LocalDate dia) {
        reservados.remove(k(chave, dia));
    }
}