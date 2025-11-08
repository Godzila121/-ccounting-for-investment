package ua.edu.university.ais.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FinancialCalculator {

    /**
     * Розраховує чистий грошовий потік (NCF) для одного року на основі драйверів.
     * Це ядро нової логіки.
     */
    public static double calculateNetCashFlow(Map<String, Double> drivers) {
        // Приклад логіки: (Кількість * Ціна) - (Кількість * Собівартість) - Постійні витрати

        double quantity = drivers.getOrDefault("Кількість продажів", 0.0);
        double price = drivers.getOrDefault("Ціна за одиницю", 0.0);
        double variableCost = drivers.getOrDefault("Собівартість одиниці", 0.0);
        double fixedCost = drivers.getOrDefault("Постійні витрати", 0.0);

        double totalIncome = quantity * price;
        double totalCost = (quantity * variableCost) + fixedCost;

        // Додаємо інші драйвери, які є прямими доходом/витратою
        double otherIncome = drivers.getOrDefault("Інші доходи", 0.0);
        double otherCosts = drivers.getOrDefault("Інші витрати", 0.0);
        double initialInvestment = drivers.getOrDefault("Інвестиція", 0.0);

        return (totalIncome + otherIncome) - (totalCost + otherCosts) - initialInvestment;
    }

    /**
     * Розраховує NPV на основі вже розрахованих річних NCF.
     */
    public static double calculateNPV(double discountRate, TreeMap<Integer, Double> netFlowsByYear) {
        if (netFlowsByYear.isEmpty()) {
            return 0.0;
        }

        int startYear = netFlowsByYear.firstKey();
        double npv = 0.0;

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int actualYear = entry.getKey();
            int normalizedYear = actualYear - startYear; // Нормалізація 2024 -> 0
            double netFlow = entry.getValue();

            npv += netFlow / Math.pow(1.0 + discountRate, normalizedYear);
        }
        return npv;
    }

    /**
     * Розраховує IRR на основі вже розрахованих річних NCF.
     */
    public static double calculateIRR(TreeMap<Integer, Double> netFlowsByYear) {
        final int MAX_ITERATIONS = 1000;
        final double ACCURACY = 0.00001;

        double lowRate = -0.99;
        double highRate = 1.0;
        double midRate = 0.0;
        double npvAtLow = calculateNPV(lowRate, netFlowsByYear);
        double npvAtHigh = calculateNPV(highRate, netFlowsByYear);

        if (npvAtLow * npvAtHigh > 0) {
            return Double.NaN;
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            midRate = (lowRate + highRate) / 2.0;
            double npvAtMid = calculateNPV(midRate, netFlowsByYear);

            if (Math.abs(npvAtMid) < ACCURACY) {
                return midRate;
            }

            if (npvAtMid > 0) {
                lowRate = midRate;
            } else {
                highRate = midRate;
            }
        }
        return midRate;
    }

    /**
     * Розраховує PP/DPP на основі вже розрахованих річних NCF.
     */
    public static double calculatePaybackPeriod(TreeMap<Integer, Double> netFlowsByYear, boolean discounted, double discountRate) {
        if (netFlowsByYear.isEmpty()) {
            return 0.0;
        }

        int startYear = netFlowsByYear.firstKey();

        double initialInvestment = 0;
        if (netFlowsByYear.containsKey(startYear)) {
            // Інвестиція - це потік у 0-му році (він має бути від'ємним)
            initialInvestment = -netFlowsByYear.get(startYear);
        }

        if (initialInvestment <= 0) return 0;

        double cumulativeFlow = 0;
        int lastNegativeNormalizedYear = 0;

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int actualYear = entry.getKey();
            if (actualYear == startYear) continue; // Пропускаємо 0-й рік

            int normalizedYear = actualYear - startYear;
            double flow = entry.getValue();

            if (discounted) {
                flow = flow / Math.pow(1.0 + discountRate, normalizedYear);
            }

            cumulativeFlow += flow;

            if (cumulativeFlow < initialInvestment) {
                lastNegativeNormalizedYear = normalizedYear;
            } else {
                double flowInPaybackYear = flow;
                double cumulativeFlowBeforePayback = cumulativeFlow - flowInPaybackYear;

                return (double) lastNegativeNormalizedYear + (initialInvestment - cumulativeFlowBeforePayback) / flowInPaybackYear;
            }
        }
        return Double.POSITIVE_INFINITY;
    }
}