package com.SgP.demo.repositories;

import com.SgP.demo.models.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, Long>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Projeto p where p.id = :id")
    Optional<Projeto> buscarParaAtualizacao(@Param("id") Long id);
}
