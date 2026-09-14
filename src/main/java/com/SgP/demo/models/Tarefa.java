package com.SgP.demo.models;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Tarefa {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String titulo;
	private String descricao;
	

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate prazo;
	private Double pesoPercentual;
	
	private Double progresso = 0.0;
	
	private String status = "PENDENTE";
	
	@ManyToOne
	@JoinColumn(name = "responsavel_id")
	private Usuario responsavel;
	
	public Usuario getResponsavel() {
		return responsavel;
	}
	
	public void setResponsavel(Usuario responsavel) {
		this.responsavel = responsavel;
	}
	
	@ManyToOne
	@JoinColumn(name = "projeto_id")
	private Projeto projeto;
	
	public Tarefa() {
		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public LocalDate getPrazo() {
		return prazo;
	}

	public void setPrazo(LocalDate prazo) {
		this.prazo = prazo;
	}

	public Double getPesoPercentual() {
		return pesoPercentual;
	}

	public void setPesoPercentual(Double pesoPercentual) {
		this.pesoPercentual = pesoPercentual;
	}

	public Double getProgresso() {
		return progresso;
	}

	public void setProgresso(Double progresso) {
		this.progresso = progresso;
	}

	public String getStatus() {
		return normalizarStatus(status);
	}

	public void setStatus(String status) {
		this.status = normalizarStatus(status);
	}

	// Aceita também os valores já gravados pelas versões anteriores.
	private static String normalizarStatus(String status) {
		if (status == null) return "PENDENTE";
		String valor = java.text.Normalizer.normalize(status.trim(), java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
		return switch (valor) {
			case "", "PENDENTE" -> "PENDENTE";
			case "EM_VALIDACAO", "AGUARDANDO_VALIDACAO" -> "AGUARDANDO_VALIDACAO";
			case "VALIDADA", "CONCLUIDA" -> "CONCLUIDA";
			default -> valor;
		};
	}

	public boolean podeExecutar(Usuario usuario) {
		return usuario != null && ("PENDENTE".equals(getStatus()) || "EM_ANDAMENTO".equals(getStatus()))
				&& ("GESTOR".equals(usuario.getFuncao()) || responsavel == null
						|| responsavel.getId().equals(usuario.getId()));
	}

	public Projeto getProjeto() {
		return projeto;
	}

	public void setProjeto(Projeto projeto) {
		this.projeto = projeto;
	}
	
}
