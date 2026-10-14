package com.SgP.demo.forms;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class TarefaForm {
    private String titulo;
    private String descricao;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate prazo;
    private Double pesoPercentual;
    private Long responsavelId;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
    public Double getPesoPercentual() { return pesoPercentual; }
    public void setPesoPercentual(Double pesoPercentual) { this.pesoPercentual = pesoPercentual; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
}
