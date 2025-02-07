package com.example.alert;

import org.apache.juli.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Service
public class StockAlertService {

    private static final Logger logger = LoggerFactory.getLogger(StockAlertService.class);

    private static final int TIME_WINDOW = 10; // Maximum days for signal confirmation
    private static final int SHORT_MA_PERIOD = 50;
    private static final int LONG_MA_PERIOD = 200;
    private static final int RSI_PERIOD = 14;
    private static final int MACD_SHORT_PERIOD = 12;
    private static final int MACD_LONG_PERIOD = 26;
    private static final int MACD_SIGNAL_PERIOD = 9;
    private static final int BBANDS_PERIOD = 20;

    // Weights for secondary conditions
    private static final double RSI_WEIGHT = 0.3;
    private static final double MACD_WEIGHT = 0.4;
    private static final double BOLLINGER_WEIGHT = 0.3;

    @Autowired
    private PrimaryConditionRepository primaryConditionRepository;

    /**
     * Analyze stock data and generate alerts for entry or exit signals.
     *
     * @param barSeries A BarSeries object containing daily stock data.
     * @param stockSymbol The stock symbol being analyzed.
     * @return A list of Alert objects with signal details.
     */
    public List<Alert> analyzeStock(BarSeries barSeries, String stockSymbol) {
        List<Alert> alerts = new ArrayList<>();

        // Initialize indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        SMAIndicator shortMA = new SMAIndicator(closePrice, SHORT_MA_PERIOD);
        SMAIndicator longMA = new SMAIndicator(closePrice, LONG_MA_PERIOD);
        RSIIndicator rsi = new RSIIndicator(closePrice, RSI_PERIOD);
        MACDIndicator macd = new MACDIndicator(closePrice, MACD_SHORT_PERIOD, MACD_LONG_PERIOD);
        EMAIndicator macdSignal = new EMAIndicator(macd, MACD_SIGNAL_PERIOD);
        BollingerBandsMiddleIndicator middleBB = new BollingerBandsMiddleIndicator(closePrice);
        BollingerBandsUpperIndicator upperBB = new BollingerBandsUpperIndicator(middleBB, closePrice);
        BollingerBandsLowerIndicator lowerBB = new BollingerBandsLowerIndicator(middleBB, closePrice);

        // Check conditions over the last TIME_WINDOW days
        int endIndex = barSeries.getEndIndex();
        int startIndex = Math.max(0, endIndex - TIME_WINDOW);

        for (int i = startIndex; i <= endIndex; i++) {
            Num close = closePrice.getValue(i);
            Num shortMAValue = shortMA.getValue(i);
            Num longMAValue = longMA.getValue(i);
            Num rsiValue = rsi.getValue(i);
            Num macdValue = macd.getValue(i);
            Num macdSignalValue = macdSignal.getValue(i);
            Num upperBBValue = upperBB.getValue(i);
            Num lowerBBValue = lowerBB.getValue(i);

            // Check for Golden Cross (Entry Signal)
            if (shortMAValue.isGreaterThan(longMAValue) &&
                    shortMA.getValue(i - 1).isLessThanOrEqual(longMA.getValue(i - 1))) {
                savePrimaryCondition(stockSymbol, "GOLDEN_CROSS", barSeries.getBar(i).getEndTime().toLocalDate());
            }

            // Check for Death Cross (Exit Signal)
            if (shortMAValue.isLessThan(longMAValue) &&
                    shortMA.getValue(i - 1).isGreaterThanOrEqual(longMA.getValue(i - 1))) {
                savePrimaryCondition(stockSymbol, "DEATH_CROSS", barSeries.getBar(i).getEndTime().toLocalDate());
            }

            // Confirm Entry Signal within TIME_WINDOW
            List<PrimaryCondition> goldenCrossConditions = primaryConditionRepository.findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
                    stockSymbol, "GOLDEN_CROSS", barSeries.getBar(i).getEndTime().toLocalDate().minusDays(TIME_WINDOW));

            for (PrimaryCondition condition : goldenCrossConditions) {
                boolean rsiOversold = rsiValue.isLessThan(closePrice.numOf(30));
                boolean macdBullish = macdValue.isGreaterThan(macdSignalValue) &&
                        macd.getValue(i - 1).isLessThanOrEqual(macdSignal.getValue(i - 1));
                boolean bollingerBreakout = close.isGreaterThan(upperBBValue);

                // Weighted scoring for secondary conditions
                double totalScore = (rsiOversold ? RSI_WEIGHT : 0) +
                        (macdBullish ? MACD_WEIGHT : 0) +
                        (bollingerBreakout ? BOLLINGER_WEIGHT : 0);

                if (totalScore >= 0.8) { // Threshold for signal confirmation
                    Alert alert = new Alert(
                            "ENTRY",
                            stockSymbol,
                            barSeries.getBar(i).getEndTime().toLocalDate(),
                            shortMAValue.doubleValue(),
                            longMAValue.doubleValue(),
                            rsiValue.doubleValue(),
                            macdValue.doubleValue(),
                            macdSignalValue.doubleValue(),
                            upperBBValue.doubleValue(),
                            "Golden Cross, RSI Oversold, MACD Bullish, Bollinger Breakout"
                    );
                    alerts.add(alert);
                    primaryConditionRepository.delete(condition);
                }
            }

            // Confirm Exit Signal within TIME_WINDOW
            List<PrimaryCondition> deathCrossConditions = primaryConditionRepository.findByStockSymbolAndConditionTypeAndOccurrenceDateAfter(
                    stockSymbol, "DEATH_CROSS", barSeries.getBar(i).getEndTime().toLocalDate().minusDays(TIME_WINDOW));

            for (PrimaryCondition condition : deathCrossConditions) {
                boolean rsiOverbought = rsiValue.isGreaterThan(closePrice.numOf(70));
                boolean macdBearish = macdValue.isLessThan(macdSignalValue) &&
                        macd.getValue(i - 1).isGreaterThanOrEqual(macdSignal.getValue(i - 1));
                boolean bollingerBreakdown = close.isLessThan(lowerBBValue);

                // Weighted scoring for secondary conditions
                double totalScore = (rsiOverbought ? RSI_WEIGHT : 0) +
                        (macdBearish ? MACD_WEIGHT : 0) +
                        (bollingerBreakdown ? BOLLINGER_WEIGHT : 0);

                if (totalScore >= 0.8) { // Threshold for signal confirmation
                    Alert alert = new Alert(
                            "EXIT",
                            stockSymbol,
                            barSeries.getBar(i).getEndTime().toLocalDate(),
                            shortMAValue.doubleValue(),
                            longMAValue.doubleValue(),
                            rsiValue.doubleValue(),
                            macdValue.doubleValue(),
                            macdSignalValue.doubleValue(),
                            lowerBBValue.doubleValue(),
                            "Death Cross, RSI Overbought, MACD Bearish, Bollinger Breakdown"
                    );
                    alerts.add(alert);
                    primaryConditionRepository.delete(condition);
                }
            }
        }

        return alerts;
    }

    private void savePrimaryCondition(String stockSymbol, String conditionType, LocalDate date) {
        PrimaryCondition condition = new PrimaryCondition();
        condition.setStockSymbol(stockSymbol);
        condition.setConditionType(conditionType);
        condition.setOccurrenceDate(date);
        primaryConditionRepository.save(condition);
    }
}