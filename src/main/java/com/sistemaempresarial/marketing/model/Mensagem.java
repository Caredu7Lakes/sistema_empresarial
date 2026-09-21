package com.sistemaempresarial.marketing.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Objeto de valor imutável que representa UMA mensagem para UM destinatário.
 *
 * <p>O destinatário é genérico (e-mail, telefone, @usuário) porque cada canal
 * usa um formato próprio — quem valida o formato é o adaptador do canal.</p>
 */
public final class Mensagem {

    private final String destinatario; // ex.: "ana@mail.com" ou "+5511999999999"
    private final String conteudo;

    public Mensagem(String destinatario, String conteudo) {
        if (destinatario == null || destinatario.isBlank())
            throw new IllegalArgumentException("Destinatário obrigatório.");
        if (conteudo == null || conteudo.isBlank())
            throw new IllegalArgumentException("Conteúdo obrigatório.");
        this.destinatario = destinatario;
        this.conteudo = conteudo;
    }

    public String destinatario() { return destinatario; }
    public String conteudo()     { return conteudo; }

    /**
     * Chave de idempotência = destinatário + hash do conteúdo.
     * Combinada com o DIA (no repositório) impede reenvio da MESMA mensagem
     * para a MESMA pessoa no MESMO dia.
     */
    public String chaveIdempotencia() {
        return destinatario + ":" + sha256(conteudo);
    }

    private static String sha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                                    .digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            // Só os 8 primeiros bytes bastam para diferenciar conteúdos.
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}