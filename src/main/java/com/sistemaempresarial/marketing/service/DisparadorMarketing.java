package com.sistemaempresarial.marketing.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.sistemaempresarial.marketing.model.Mensagem;
import com.sistemaempresarial.marketing.model.ResultadoEnvio;
import com.sistemaempresarial.marketing.model.StatusEnvio;
import com.sistemaempresarial.marketing.port.ServicoMensagem;
import com.sistemaempresarial.marketing.port.RepositorioEnvios;
import com.sistemaempresarial.marketing.port.ListaSupressao;

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

    public List<ResultadoEnvio> disparar(Mensagem mensagem) {
        LocalDate hoje = LocalDate.now(clock);
        String chave = mensagem.chaveIdempotencia();
        List<ResultadoEnvio> resultados = new ArrayList<>();

        for (ServicoMensagem canal : canais) {
            ResultadoEnvio r;
            String chaveCanal = chave + "#" + canal.canal();

            if (supressao.suprimido(mensagem.destinatario())) {
                r = ResultadoEnvio.de(canal.canal(), StatusEnvio.SUPRIMIDO, "destinatario suprimido");

            } else if (!repositorio.reservar(chaveCanal, hoje)) {
                r = ResultadoEnvio.de(canal.canal(), StatusEnvio.DUPLICADO, "ja enviado hoje");

            } else {
                try {
                    r = canal.enviar(mensagem);
                } catch (RuntimeException e) {
                    repositorio.liberar(chaveCanal, hoje);
                    r = ResultadoEnvio.de(canal.canal(), StatusEnvio.FALHA_TEMPORARIA, e.getMessage());
                }
            }
            resultados.add(r);
        }
        return resultados;
    }
}