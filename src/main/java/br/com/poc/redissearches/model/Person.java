package br.com.poc.redissearches.model;

import java.util.UUID;

public record Person(UUID id, String name, String tipoPessoa) {
    public Person() {
        this(null, null, null);
    }
}
