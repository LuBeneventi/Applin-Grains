package com.pedidos360.audit.repository;

import com.pedidos360.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByTimestampAsc(String entityType, String entityId);

    Page<AuditEvent> findByActorIdOrderByTimestampDesc(String actorId, Pageable pageable);

    boolean existsByEventId(String eventId);
}
