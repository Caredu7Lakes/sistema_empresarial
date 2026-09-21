package com.sistemaempresarial.marketing.service;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.model.StatusEnvio;
import com.sistemaempresarial.marketing.port.ServicoMensagem;
import com.sistemaempresarial.marketing.port.RepositorioEnvios;
import com.sistemaempresarial.marketing.port.ListaSupressao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestra o envio da MESMA mensagem para N canais, aplicando, por canal:
 *   1) supressão  -> destinatário com bounce/reclamação não recebe;
 *   2) idempotência -> não reenvia a mesma mensagem no mesmo dia;
 *   3) isolamento de falha -> erro em um canal não afeta os demais.
 *
 * Clock é injetado para tornar a regra de "mesmo dia" testável.
 */
public class DisparadorMarketing {

    private final List<ServicoMensagem> canais = new ArrayList<>();
    private final RepositorioEnvios repositorio;
    private final ListaSupressao supressao;
    private final Clock clock;

    public DisparadorMarketing(RepositorioEnvios repositorio,
                               ListaSupressao supressao,
                               Clock clock) {
        this.repositorio = repositorio;
        this.supressao = supressao;
        this.clock = clock;
    }

    public DisparadorMarketing registrar(ServicoMensagem canal) {
        canais.add(canal);
        return this;
    }

    /** Dispara para todos os canais e devolve o resultado de cada um. */
    public List<ResultadoEnvio> disparar(Mensagem mensagem) {
        LocalDate hoje = LocalDate.now(clock);
        String chave = mensagem.chaveIdempotencia();
        List<ResultadoEnvio> resultados = new ArrayList<>();

        for (ServicoMensagem canal : canais) {
            ResultadoEnvio r;
            // A dedup é POR CANAL: enviar a mesma promo por SMS e por WhatsApp é
            // multicanal legítimo; o que se bloqueia é repetir o MESMO canal no dia.
            String chaveCanal = chave + "#" + canal.canal();

            // 1) Supressão: endereço inválido/bounce nunca é disparado.
            if (supressao.suprimido(mensagem.destinatario())) {
                r = ResultadoEnvio.de(canal.canal(), StatusEnvio.SUPRIMIDO, "destinatário suprimido");

            // 2) Idempotência: mesma mensagem, mesmo destinatário, mesmo canal, mesmo dia.
            } else if (repositorio.jaEnviadoHoje(chaveCanal, hoje)) {
                r = ResultadoEnvio.de(canal.canal(), StatusEnvio.DUPLICADO, "já enviado hoje");

            // 3) Envio real, com isolamento de falha.
            } else {
                try {
                    r = canal.enviar(mensagem);
                } catch (RuntimeException e) {
                    r = ResultadoEnvio.de(canal.canal(), StatusEnvio.FALHA_TEMPORARIA, e.getMessage());
                }
                repositorio.registrar(chaveCanal, hoje, r);
            }
            resultados.add(r);
        }
        return resultados;
    }
}
