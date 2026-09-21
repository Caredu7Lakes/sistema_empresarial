package com.sistemaempresarial.marketing.model;

/** Retorno imutável de cada tentativa de envio, por canal. */
public final class ResultadoEnvio {

    private final String canal;
    private final StatusEnvio status;
    private final String idProvedor; // id da mensagem no provedor (nulo se não enviou)
    private final String motivo;     // descrição em caso de não-ACEITO

    private ResultadoEnvio(String canal, StatusEnvio status, String idProvedor, String motivo) {
        this.canal = canal; this.status = status; this.idProvedor = idProvedor; this.motivo = motivo;
    }

    public static ResultadoEnvio aceito(String canal, String idProvedor) {
        return new ResultadoEnvio(canal, StatusEnvio.ACEITO, idProvedor, null);
    }
    public static ResultadoEnvio de(String canal, StatusEnvio status, String motivo) {
        return new ResultadoEnvio(canal, status, null, motivo);
    }

    public String canal()        { return canal; }
    public StatusEnvio status()  { return status; }
    public String idProvedor()   { return idProvedor; }
    public String motivo()       { return motivo; }

    @Override public String toString() {
        return "%s -> %s%s".formatted(canal, status,
                idProvedor != null ? " (" + idProvedor + ")"
                                   : motivo != null ? " [" + motivo + "]" : "");
    }
}