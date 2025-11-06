package ua.edu.university.ais.util;

import ua.edu.university.ais.models.CashFlow;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FinancialCalculator {

    private static Map<Integer, Double> getNetFlowsByYear(List<CashFlow> flows) {
        return flows.stream()
                .collect(Collectors.groupingBy(
                        CashFlow::getYear,
                        TreeMap::new,
                        Collectors.summingDouble(CashFlow::getAmount)
                ));
    }

    public static double calculateNPV(double discountRate, List<CashFlow> flows) {
        Map<Integer, Double> netFlowsByYear = getNetFlowsByYear(flows);

        double npv = 0.0;
        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int year = entry.getKey();
            double netFlow = entry.getValue();
            npv += netFlow / Math.pow(1.0 + discountRate, year);
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
        Map<Integer, Double> netFlowsByYear = getNetFlowsByYear(flows);

        double initialInvestment = 0;
        if (netFlowsByYear.containsKey(0)) {
            initialInvestment = -netFlowsByYear.get(0);
        }

        if (initialInvestment <= 0) return 0;

        double cumulativeFlow = 0;
        int lastNegativeYear = 0;

        for (Map.Entry<Integer, Double> entry : netFlowsByYear.entrySet()) {
            int year = entry.getKey();
            if (year == 0) continue;

            double flow = entry.getValue();
            if (discounted) {
                flow = flow / Math.pow(1.0 + discountRate, year);
            }

            cumulativeFlow += flow;

            if (cumulativeFlow < initialInvestment) {
                lastNegativeYear = year;
            } else {
                double flowInPaybackYear = flow;
                double cumulativeFlowBeforePayback = cumulativeFlow - flowInPaybackYear;

                return (double) lastNegativeYear + (initialInvestment - cumulativeFlowBeforePayback) / flowInPaybackYear;
            }
        }
        return Double.POSITIVE_INFINITY;
    }
}