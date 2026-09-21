package com.sistemaempresarial.bootstrap;

import java.time.Clock;
import java.time.ZoneId;
import java.net.http.HttpClient;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sns.SnsClient;

import com.sistemaempresarial.marketing.adapter.ListaSupressaoDynamo;
import com.sistemaempresarial.marketing.adapter.RepositorioEnviosDynamo;
import com.sistemaempresarial.marketing.adapter.ServicoEmailSes;
import com.sistemaempresarial.marketing.adapter.ServicoRedesSociais;
import com.sistemaempresarial.marketing.adapter.ServicoSmsSns;
import com.sistemaempresarial.marketing.adapter.ServicoWhatsAppMeta;
import com.sistemaempresarial.marketing.port.ListaSupressao;
import com.sistemaempresarial.marketing.port.RepositorioEnvios;
import com.sistemaempresarial.marketing.service.DisparadorMarketing;
import com.sistemaempresarial.marketing.service.ProcessadorEntrega;
import com.sistemaempresarial.vendas.adapter.CatalogoProdutosJdbc;
import com.sistemaempresarial.vendas.port.CatalogoProdutos;
import com.sistemaempresarial.vendas.service.Caixa;
import com.sistemaempresarial.vendas.service.LeitorCodigoBarras;

/**
 * Raiz de composição (produção): monta os adapters reais (RDS/DynamoDB) e
 * injeta nas portas. É o ÚNICO ponto que conhece infraestrutura concreta.
 *
 * Configuração vem de variáveis de ambiente — em produção populadas pelo
 * AWS Secrets Manager via runtime (ECS task secrets / Lambda env), sem
 * credencial no código. Credenciais AWS: cadeia padrão do SDK (IAM role).
 */
public final class ContextoAplicacao implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final DynamoDbClient dynamo;
    private final SesV2Client ses;
    private final SnsClient sns;

    private final DisparadorMarketing disparador;
    private final ProcessadorEntrega processadorEntrega;
    private final Caixa caixa;

    public ContextoAplicacao() {
        ZoneId zona = ZoneId.of(env("APP_ZONE", "America/Sao_Paulo"));

        // ----- Infra -----
        this.dataSource = criarDataSource();
        this.dynamo = DynamoDbClient.builder()
                .region(Region.of(obrig("DYNAMO_REGION")))
                .build();
        this.ses = SesV2Client.builder()
                .region(Region.of(env("SES_REGION", obrig("DYNAMO_REGION"))))
                .build();
        this.sns = SnsClient.builder()
                .region(Region.of(env("SNS_REGION", obrig("DYNAMO_REGION"))))
                .build();
        HttpClient http = HttpClient.newHttpClient();

        // ----- Adapters de produção -----
        CatalogoProdutos catalogo = new CatalogoProdutosJdbc(dataSource);
        RepositorioEnvios repositorio =
                new RepositorioEnviosDynamo(dynamo, obrig("DYNAMO_TABELA_ENVIOS"), zona);
        ListaSupressao supressao =
                new ListaSupressaoDynamo(dynamo, obrig("DYNAMO_TABELA_SUPRESSAO"));

        // ----- Núcleo (portas injetadas) -----
        this.disparador = new DisparadorMarketing(repositorio, supressao, Clock.system(zona))
                .registrar(new ServicoSmsSns(sns))
                .registrar(new ServicoEmailSes(ses, obrig("SES_REMETENTE"), env("SES_ASSUNTO", "Novidades")))
                .registrar(new ServicoRedesSociais())
                .registrar(new ServicoWhatsAppMeta(http,
                        env("META_BASE_URL", "https://graph.facebook.com/v21.0"),
                        obrig("META_PHONE_NUMBER_ID"),
                        obrig("META_ACCESS_TOKEN"),
                        obrig("META_TEMPLATE"),
                        env("META_IDIOMA", "pt_BR")));
        this.processadorEntrega = new ProcessadorEntrega(supressao);
        this.caixa = new Caixa(new LeitorCodigoBarras(catalogo));
    }

    private HikariDataSource criarDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(obrig("DB_URL"));      // jdbc:postgresql://host:5432/db
        cfg.setUsername(obrig("DB_USER"));
        cfg.setPassword(obrig("DB_PASSWORD"));
        cfg.setMaximumPoolSize(Integer.parseInt(env("DB_POOL_MAX", "10")));
        cfg.setPoolName("projeto003-pool");
        return new HikariDataSource(cfg);
    }

    public DisparadorMarketing disparador()         { return disparador; }
    public ProcessadorEntrega processadorEntrega()  { return processadorEntrega; }
    public Caixa caixa()                            { return caixa; }

    /** Libera pool e cliente na parada da aplicação. */
    @Override public void close() {
        if (dataSource != null) dataSource.close();
        if (dynamo != null) dynamo.close();
        if (ses != null) ses.close();
        if (sns != null) sns.close();
    }

    // ----- helpers de configuração -----
    private static String obrig(String chave) {
        String v = System.getenv(chave);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Variável de ambiente obrigatória ausente: " + chave);
        return v;
    }
    private static String env(String chave, String padrao) {
        String v = System.getenv(chave);
        return (v == null || v.isBlank()) ? padrao : v;
    }
}