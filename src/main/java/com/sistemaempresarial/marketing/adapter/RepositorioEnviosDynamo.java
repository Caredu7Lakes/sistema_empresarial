package com.sistemaempresarial.marketing.adapter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import com.sistemaempresarial.marketing.port.RepositorioEnvios;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Idempotência em DynamoDB.
 * Tabela: PK "pk" (S) = "chaveCanal#dia"; atributo "ttl" (N) para limpeza.
 * Billing on-demand (PAY_PER_REQUEST).
 */
public class RepositorioEnviosDynamo implements RepositorioEnvios {

    private final DynamoDbClient db;
    private final String tabela;
    private final ZoneId zona;

    public RepositorioEnviosDynamo(DynamoDbClient db, String tabela, ZoneId zona) {
        this.db = db; this.tabela = tabela; this.zona = zona;
    }

    @Override
    public boolean reservar(String chave, LocalDate dia) {
        String pk = chave + "#" + dia;
        long ttl = dia.plusDays(1).atStartOfDay(zona).toEpochSecond(); // expira no dia seguinte
        try {
            db.putItem(PutItemRequest.builder()
                    .tableName(tabela)
                    .item(Map.of(
                            "pk",  AttributeValue.fromS(pk),
                            "ttl", AttributeValue.fromN(Long.toString(ttl))))
                    .conditionExpression("attribute_not_exists(pk)") // gate atômico
                    .build());
            return true;  // reservou
        } catch (ConditionalCheckFailedException e) {
            return false; // já reservada -> duplicado
        }
    }

    @Override
    public void liberar(String chave, LocalDate dia) {
        String pk = chave + "#" + dia;
        db.deleteItem(DeleteItemRequest.builder()
                .tableName(tabela)
                .key(Map.of("pk", AttributeValue.fromS(pk)))
                .build());
    }
}