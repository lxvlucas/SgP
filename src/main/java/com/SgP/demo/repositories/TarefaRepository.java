package com.SgP.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.SgP.demo.models.Tarefa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long>{
    List<Tarefa> findByProjetoIdOrderByIdAsc(Long projetoId);

    @Query("select t.projeto.id from Tarefa t where t.id = :id")
    Optional<Long> buscarProjetoId(@Param("id") Long id);
}
