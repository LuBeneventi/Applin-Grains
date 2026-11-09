package com.pedidos360.report.repository;

import com.pedidos360.report.entity.ReportOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportOrderRepository extends JpaRepository<ReportOrder, Long> {

    // 1. Datos para SalesChart (Ventas por fecha)
    @Query("SELECT r.saleDate, COUNT(r), SUM(r.total) FROM ReportOrder r " +
           "WHERE r.saleDate BETWEEN :from AND :to AND r.currentStatus <> 'CANCELADO' " +
           "GROUP BY r.saleDate ORDER BY r.saleDate ASC")
    List<Object[]> getSalesBetweenDates(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // 2. Datos para LeadTimeChart (Tiempos de ciclo promedio en segundos)
    @Query("SELECT AVG(r.leadTimeAcceptSeconds), AVG(r.leadTimePrepareSeconds), " +
           "AVG(r.leadTimeDispatchSeconds), AVG(r.leadTimeDeliverySeconds), AVG(r.totalLeadTimeSeconds) " +
           "FROM ReportOrder r WHERE r.deliveredAt IS NOT NULL")
    List<Object[]> getAverageLeadTimes();

    // 3. Datos para TopProductsChart (Productos más vendidos)
    @Query("SELECT i.productoId, i.descripcion, SUM(i.cantidad), SUM(i.subtotal) " +
           "FROM ReportOrderItem i JOIN i.order o " +
           "WHERE o.currentStatus <> 'CANCELADO' " +
           "GROUP BY i.productoId, i.descripcion ORDER BY SUM(i.cantidad) DESC")
    List<Object[]> getTopSellingProducts();
}
