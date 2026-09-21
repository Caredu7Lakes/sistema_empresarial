package com.sistemaempresarial;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import com.sistemaempresarial.marketing.model.*;
import com.sistemaempresarial.marketing.port.*;
import com.sistemaempresarial.marketing.service.*;
import com.sistemaempresarial.marketing.adapter.*;
import com.sistemaempresarial.vendas.model.*;
import com.sistemaempresarial.vendas.service.*;

/**
 * Demonstração de ponta a ponta (fio condutor). Em produção, os adaptadores
 * em memória seriam substituídos por RDS/DynamoDB/SES sem alterar este fluxo.
 */
public class Main {
    public static void main(String[] args) {

        // ---------- Infra de marketing (adaptadores em memória) ----------
        RepositorioEnvios repo = new RepositorioEnviosMemoria();
        ListaSupressao supressao = new ListaSupressaoMemoria();

        DisparadorMarketing disparador = new DisparadorMarketing(repo, supressao, Clock.systemDefaultZone())
                .registrar(new ServicoSms())
                .registrar(new ServicoEmail())
                .registrar(new ServicoRedesSociais())
                .registrar(new ServicoWhatsApp());

        Mensagem promo = new Mensagem("ana@mail.com", "Promoção de inauguração: 10% OFF hoje!");

        System.out.println("== 1º disparo ==");
        disparador.disparar(promo).forEach(System.out::println);

        System.out.println("== 2º disparo (mesmo dia -> DUPLICADO) ==");
        disparador.disparar(promo).forEach(System.out::println);

        // Webhook do provedor informa que o e-mail não existe (bounce):
        new ProcessadorEntrega(supressao)
                .processar("ana@mail.com", ProcessadorEntrega.Evento.BOUNCE, "550 5.1.1 no such user");
        System.out.println("== Nova mensagem após bounce -> SUPRIMIDO ==");
        disparador.disparar(new Mensagem("ana@mail.com", "Outra campanha"))
                  .forEach(System.out::println);

        // ---------- Vendas: código de barras + imposto ----------
        Map<String, Produto> catalogo = new HashMap<>();
        catalogo.put("789100000001",
            new Produto("789100000001", "Arroz 5kg", new BigDecimal("29.90"), TipoProduto.ALIMENTACAO, 0.0));
        catalogo.put("789100000002",
            new Produto("789100000002", "Camiseta", new BigDecimal("59.90"), TipoProduto.VESTUARIO, +0.005));
        catalogo.put("789100000003",
            new Produto("789100000003", "Livro", new BigDecimal("45.00"), TipoProduto.CULTURA, -0.010));

        Caixa caixa = new Caixa(new LeitorCodigoBarras(catalogo));
        caixa.adicionarPorCodigo("789100000001");
        caixa.adicionarPorCodigo("789100000002");
        caixa.adicionarPorCodigo("789100000003");

        System.out.printf("%nTotal venda:   R$ %s%n", caixa.totalVenda());
        System.out.printf("Total imposto: R$ %s%n", caixa.totalImposto());
    }
}