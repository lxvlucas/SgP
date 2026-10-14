package com.SgP.demo.services;

import com.SgP.demo.exceptions.DadosInvalidosException;

final class ValidacaoCadastro {
    private ValidacaoCadastro() { }

    static String textoObrigatorio(String texto, String campo) {
        if (texto == null || texto.isBlank() || texto.trim().length() > 255) {
            throw new DadosInvalidosException("texto", "Preencha " + campo + " com até 255 caracteres.");
        }
        return texto.trim();
    }
}
