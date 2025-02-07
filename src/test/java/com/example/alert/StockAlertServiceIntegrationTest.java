package com.example.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
@SpringBootTest
public class StockAlertServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(StockAlertServiceIntegrationTest.class);

    @Autowired
    private StockAlertService stockAlertService;

    @Autowired
    private PrimaryConditionRepository primaryConditionRepository;

    private BarSeries barSeries;

    @BeforeEach
    public void setUp() {
        // Reset the bar series before each test
        barSeries = new BaseBarSeries("Test Stock");

        // Simulate initial data (up to Day 5)
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 100, 110, 95, 105, 1000));
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 2, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 105, 115, 100, 110, 1200));
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 3, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 110, 120, 105, 115, 1300));
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 4, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 115, 125, 110, 120, 1400));
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 5, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 120, 130, 115, 125, 1500));

        // Clear the database before each test
        primaryConditionRepository.deleteAll();
    }

    /**
     * Helper method to create a BaseBar object.
     */
    private BaseBar createBar(ZonedDateTime endTime, double open, double high, double low, double close, double volume) {
        return new BaseBar(Duration.ofDays(1), endTime, open, high, low, close, volume);
    }

    @Test
    public void testGoldenCrossWithSequentialConfirmation() {
        // Simulate a Golden Cross on Day 6
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 6, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 130, 140, 125, 135, 1600)); // Golden Cross occurs here
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 7, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 135, 145, 130, 140, 1700)); // Secondary conditions align here

        // Analyze stock data
        List<Alert> alerts = stockAlertService.analyzeStock(barSeries, "TEST");

        // Debugging: Print alerts
        alerts.forEach(alert -> logger.info("Generated Alert: {}", alert));

        // Verify that the Golden Cross was saved in the database
        List<PrimaryCondition> goldenCrossConditions = primaryConditionRepository.findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
                "TEST", "GOLDEN_CROSS", ZonedDateTime.of(2023, 10, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC).toLocalDate());
        assertEquals(0, goldenCrossConditions.size(), "Golden Cross should be removed after confirmation");

        // Verify that an ENTRY SIGNAL alert was generated
        assertEquals(1, alerts.size(), "One alert should be generated for the Golden Cross");
        assertEquals("ENTRY", alerts.get(0).getSignalType(), "Alert should indicate an entry signal");
    }

    @Test
    public void testDeathCrossWithSequentialConfirmation() {
        // Simulate a Death Cross on Day 6
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 6, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 120, 130, 115, 125, 1500)); // Death Cross occurs here
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 7, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 115, 125, 110, 120, 1400)); // Secondary conditions align here

        // Analyze stock data
        List<Alert> alerts = stockAlertService.analyzeStock(barSeries, "TEST");

        // Debugging: Print alerts
        alerts.forEach(alert -> logger.info("Generated Alert: {}", alert));

        // Verify that the Death Cross was saved in the database
        List<PrimaryCondition> deathCrossConditions = primaryConditionRepository.findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
                "TEST", "DEATH_CROSS", ZonedDateTime.of(2023, 10, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC).toLocalDate());
        assertEquals(0, deathCrossConditions.size(), "Death Cross should be removed after confirmation");

        // Verify that an EXIT SIGNAL alert was generated
        assertEquals(1, alerts.size(), "One alert should be generated for the Death Cross");
        assertEquals("EXIT", alerts.get(0).getSignalType(), "Alert should indicate an exit signal");
    }

    @Test
    public void testNoSignalIfSecondaryConditionsDoNotAlign() {
        // Simulate a Golden Cross on Day 6
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 6, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 130, 140, 125, 135, 1600)); // Golden Cross occurs here
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 7, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 135, 145, 130, 140, 1700)); // Secondary conditions do NOT align here

        // Analyze stock data
        List<Alert> alerts = stockAlertService.analyzeStock(barSeries, "TEST");

        // Debugging: Print alerts
        alerts.forEach(alert -> logger.info("Generated Alert: {}", alert));

        // Verify that the Golden Cross was saved in the database
        List<PrimaryCondition> goldenCrossConditions = primaryConditionRepository.findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
                "TEST", "GOLDEN_CROSS", ZonedDateTime.of(2023, 10, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC).toLocalDate());
        assertEquals(1, goldenCrossConditions.size(), "Golden Cross should remain in the database");

        // Verify that no alert was generated
        assertEquals(0, alerts.size(), "No alert should be generated if secondary conditions do not align");
    }

    @Test
    public void testTimeWindowExpiryForPrimaryCondition() {
        // Simulate a Golden Cross on Day 6
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 6, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 130, 140, 125, 135, 1600)); // Golden Cross occurs here

        // Analyze stock data after TIME_WINDOW days
        barSeries.addBar(createBar(ZonedDateTime.of(2023, 10, 17, 0, 0, 0, 0, java.time.ZoneOffset.UTC), 135, 145, 130, 140, 1700)); // Secondary conditions align here (but too late)

        // Analyze stock data
        List<Alert> alerts = stockAlertService.analyzeStock(barSeries, "TEST");

        // Debugging: Print alerts
        alerts.forEach(alert -> logger.info("Generated Alert: {}", alert));

        // Verify that the Golden Cross was removed from the database due to time window expiry
        List<PrimaryCondition> goldenCrossConditions = primaryConditionRepository.findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
                "TEST", "GOLDEN_CROSS", ZonedDateTime.of(2023, 10, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC).toLocalDate());
        assertEquals(0, goldenCrossConditions.size(), "Golden Cross should be removed after time window expiry");

        // Verify that no alert was generated
        assertEquals(0, alerts.size(), "No alert should be generated if time window expires");
    }
}