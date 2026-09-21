package com.sistemaempresarial.marketing.port;

/**
 * Porta: destinatários bloqueados por bounce (endereço inexistente) ou
 * reclamação de spam. Produção: tabela DynamoDB/RDS.
 */
public interface ListaSupressao {
    boolean suprimido(String destinatario);
    void suprimir(String destinatario, String motivo);
}
