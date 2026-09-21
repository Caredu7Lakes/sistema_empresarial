package com.sistemaempresarial.marketing.port;

import java.time.LocalDate;

public interface RepositorioEnvios {

    boolean reservar(String chaveIdempotencia, LocalDate dia);

    void liberar(String chaveIdempotencia, LocalDate dia);
}