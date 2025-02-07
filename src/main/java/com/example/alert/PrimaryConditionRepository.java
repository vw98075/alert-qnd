package com.example.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrimaryConditionRepository extends JpaRepository<PrimaryCondition, Long> {

    List<PrimaryCondition> findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
            String stockSymbol, String conditionType, LocalDate date);
}