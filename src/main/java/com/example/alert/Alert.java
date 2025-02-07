package com.example.alert;

import java.time.LocalDate;

public class Alert {
    private String signalType; // "ENTRY" or "EXIT"
    private String stockSymbol;
    private LocalDate date;
    private double shortMAValue;
    private double longMAValue;
    private double rsiValue;
    private double macdValue;
    private double macdSignalValue;
    private double bollingerValue;
    private String reasoning;

    // Constructor
    public Alert(String signalType, String stockSymbol, LocalDate date, double shortMAValue, double longMAValue,
                 double rsiValue, double macdValue, double macdSignalValue, double bollingerValue, String reasoning) {
        this.signalType = signalType;
        this.stockSymbol = stockSymbol;
        this.date = date;
        this.shortMAValue = shortMAValue;
        this.longMAValue = longMAValue;
        this.rsiValue = rsiValue;
        this.macdValue = macdValue;
        this.macdSignalValue = macdSignalValue;
        this.bollingerValue = bollingerValue;
        this.reasoning = reasoning;
    }

    // Getters and Setters
    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getShortMAValue() { return shortMAValue; }
    public void setShortMAValue(double shortMAValue) { this.shortMAValue = shortMAValue; }

    public double getLongMAValue() { return longMAValue; }
    public void setLongMAValue(double longMAValue) { this.longMAValue = longMAValue; }

    public double getRsiValue() { return rsiValue; }
    public void setRsiValue(double rsiValue) { this.rsiValue = rsiValue; }

    public double getMacdValue() { return macdValue; }
    public void setMacdValue(double macdValue) { this.macdValue = macdValue; }

    public double getMacdSignalValue() { return macdSignalValue; }
    public void setMacdSignalValue(double macdSignalValue) { this.macdSignalValue = macdSignalValue; }

    public double getBollingerValue() { return bollingerValue; }
    public void setBollingerValue(double bollingerValue) { this.bollingerValue = bollingerValue; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    @Override
    public String toString() {
        return String.format("%s SIGNAL on %s for %s: Short MA = %.2f, Long MA = %.2f, RSI = %.2f, MACD = %.2f, Bollinger = %.2f. Reasoning: %s",
                signalType, date, stockSymbol, shortMAValue, longMAValue, rsiValue, macdValue, bollingerValue, reasoning);
    }
}
