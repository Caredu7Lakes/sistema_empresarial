package com.sistemaempresarial.marketing.adapter;

import java.util.Map;

import com.sistemaempresarial.marketing.port.ListaSupressao;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Supressão em DynamoDB. Tabela: PK "destinatario" (S), atributo "motivo" (S).
 * Sem TTL — bounce/reclamação é permanente.
 */
public class ListaSupressaoDynamo implements ListaSupressao {

    private final DynamoDbClient db;
    private final String tabela;

    public ListaSupressaoDynamo(DynamoDbClient db, String tabela) {
        this.db = db; this.tabela = tabela;
    }

    @Override
    public boolean suprimido(String destinatario) {
        GetItemResponse r = db.getItem(GetItemRequest.builder()
                .tableName(tabela)
                .key(Map.of("destinatario", AttributeValue.fromS(destinatario)))
                .build());
        return r.hasItem();
    }

    @Override
    public void suprimir(String destinatario, String motivo) {
        db.putItem(PutItemRequest.builder()
                .tableName(tabela)
                .item(Map.of(
                        "destinatario", AttributeValue.fromS(destinatario),
                        "motivo",       AttributeValue.fromS(motivo == null ? "" : motivo)))
                .build());
    }
}