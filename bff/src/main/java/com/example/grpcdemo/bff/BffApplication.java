package com.example.grpcdemo.bff;

import io.micronaut.runtime.Micronaut;

public final class BffApplication {

    public static void main(String[] args) {
        Micronaut.run(BffApplication.class, args);
    }

    private BffApplication() {}
}
