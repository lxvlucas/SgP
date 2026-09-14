package com.SgP.demo.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

@Entity
public class Projeto {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String nome;
	private String descricao;
	
	private Double percentualConclusao = 0.0;
	

    @ManyToMany
    @JoinTable(
        name = "projeto_colaboradores",
        joinColumns = @JoinColumn(name = "projeto_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> colaboradores = new ArrayList<>();

    public List<Usuario> getColaboradores() { 
        return colaboradores; 
    }
    
    public void setColaboradores(List<Usuario> colaboradores) { 
        this.colaboradores = colaboradores; 
    }
	@OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL)
	private List<Tarefa> tarefas = new ArrayList<>();
	
	public Projeto() {
		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Double getPercentualConclusao() {
		return percentualConclusao;
	}

	public void setPercentualConclusao(Double percentualConclusao) {
		this.percentualConclusao = percentualConclusao;
	}

	public List<Tarefa> getTarefas() {
		return tarefas;
	}

	public void setTarefas(List<Tarefa> tarefas) {
		this.tarefas = tarefas;
	}
	
	
}
	
	
