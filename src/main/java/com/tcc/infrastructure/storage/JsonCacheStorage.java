package com.tcc.infrastructure.storage;

import java.util.Optional;

/**
 * Armazenamento de documentos JSON usados como cache de resultado.
 */
public interface JsonCacheStorage {

    /**
     * Grava o JSON sob a chave informada. Falha de escrita não propaga
     * exceção: o cache é acessório e não pode derrubar o fluxo chamador.
     */
    void write(String key, String json);

    /**
     * Lê o JSON da chave informada. Devolve {@code Optional.empty()} quando
     * o objeto não existe ou quando a leitura falha.
     */
    Optional<String> read(String key);
}
