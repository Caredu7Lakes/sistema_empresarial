package com.sistemaempresarial.marketing.service;

import com.sistemaempresarial.marketing.port.ListaSupressao;

/**
 * Trata as confirmações ASSÍNCRONAS de entrega vindas dos provedores
 * (ex.: SES -> SNS, WhatsApp -> status webhook).
 *
 * É a resposta a "e se o e-mail não existir?": o provedor devolve BOUNCE,
 * e aqui o endereço é colocado na lista de supressão para não ser reenviado.
 */
public class ProcessadorEntrega {

    /** Tipos de evento que um provedor pode notificar após o aceite. */
    public enum Evento { ENTREGUE, BOUNCE, RECLAMACAO }

    private final ListaSupressao supressao;

    public ProcessadorEntrega(ListaSupressao supressao) {
        this.supressao = supressao;
    }

    public void processar(String destinatario, Evento evento, String detalhe) {
        switch (evento) {
            case ENTREGUE ->
                // Sucesso confirmado; aqui poderia atualizar métricas/status.
                { /* no-op neste escopo */ }
            case BOUNCE ->
                // Endereço inexistente/permanente -> suprime para não reenviar.
                supressao.suprimir(destinatario, "bounce: " + detalhe);
            case RECLAMACAO ->
                // Marcado como spam -> suprime (proteção de reputação do remetente).
                supressao.suprimir(destinatario, "reclamação: " + detalhe);
        }
    }
}
