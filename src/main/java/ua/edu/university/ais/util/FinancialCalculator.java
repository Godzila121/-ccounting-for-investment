package ua.edu.university.ais.util;

import ua.edu.university.ais.models.CashFlow;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FinancialCalculator {

    private static TreeMap<Integer, Double> getNetFlowsByYear(List<CashFlow> flows) {
        return flows.stream()
                .collect(Collectors.groupingBy(
                        CashFlow::getYear,
                        TreeMap::new,
                        Collectors.summingDouble(CashFlow::getAmount)
                ));
    }

    public static double calculateNPV(double discountRate, List<CashFlow> flows) {
        TreeMap<Integer, Double> netFlowsByYear = getNetFlowsByYear(flows);
        if (netFlowsByYear.isEmpty()) {
            return 0.0;
        }

        int startYear = netFlowsByYear.firstKey();
        double npv = 0.0;

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int actualYear = entry.getKey();
            int normalizedYear = actualYear - startYear;
            double netFlow = entry.getValue();

            npv += netFlow / Math.pow(1.0 + discountRate, normalizedYear);
        }
        return npv;
    }

    public static double calculateIRR(List<CashFlow> flows) {
        final int MAX_ITERATIONS = 1000;
        final double ACCURACY = 0.00001;

        double lowRate = -0.99;
        double highRate = 1.0;
        double midRate = 0.0;
        double npvAtLow = calculateNPV(lowRate, flows);
        double npvAtHigh = calculateNPV(highRate, flows);

        if (npvAtLow * npvAtHigh > 0) {
            return Double.NaN;
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            midRate = (lowRate + highRate) / 2.0;
            double npvAtMid = calculateNPV(midRate, flows);

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

    public static double calculatePaybackPeriod(List<CashFlow> flows, boolean discounted, double discountRate) {
        TreeMap<Integer, Double> netFlowsByYear = getNetFlowsByYear(flows);
        if (netFlowsByYear.isEmpty()) {
            return 0.0;
        }

        int startYear = netFlowsByYear.firstKey();

        double initialInvestment = 0;
        if (netFlowsByYear.containsKey(startYear)) {
            initialInvestment = -netFlowsByYear.get(startYear);
        }

        if (initialInvestment <= 0) return 0;

        double cumulativeFlow = 0;
        int lastNegativeNormalizedYear = 0;

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int actualYear = entry.getKey();
            if (actualYear == startYear) continue;

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