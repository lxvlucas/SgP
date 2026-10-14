package com.SgP.demo.exceptions;

public class DadosInvalidosException extends RuntimeException {
    private final String codigo;

    public DadosInvalidosException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
