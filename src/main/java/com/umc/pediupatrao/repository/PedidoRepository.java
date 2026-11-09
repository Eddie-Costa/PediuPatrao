package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.Instant;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    @Query("{ 'recebidoEm': { $gte: ?0, $lt: ?1 } }")
    List<Pedido> buscarRecebidosNoPeriodo(Instant inicio, Instant fimExclusivo, Sort ordenacao);

    long countByRecebidoEmIsNull();
}
