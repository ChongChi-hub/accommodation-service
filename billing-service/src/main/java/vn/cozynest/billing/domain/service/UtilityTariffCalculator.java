package vn.cozynest.billing.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure domain calculator supporting 3 Vietnamese utility tariff models: EVN_TIERED, FIXED, and PER_PERSON.
 */
public final class UtilityTariffCalculator {

    private UtilityTariffCalculator() {}

    public enum PricingModel {
        EVN_TIERED,
        FIXED,
        PER_PERSON
    }

    public static long calculateElectricityCost(
            BigDecimal consumptionKwh,
            PricingModel model,
            Long fixedUnitPriceVnd,
            Long perPersonRateVnd,
            int occupantCount
    ) {
        if (consumptionKwh == null || consumptionKwh.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("San luong dien tieu thu khong hop le");
        }
        PricingModel resolved = model != null ? model : PricingModel.EVN_TIERED;
        return switch (resolved) {
            case EVN_TIERED -> calculateEvnTieredElectricity(consumptionKwh);
            case FIXED -> {
                long unit = fixedUnitPriceVnd != null && fixedUnitPriceVnd > 0 ? fixedUnitPriceVnd : 3500L;
                yield consumptionKwh.multiply(BigDecimal.valueOf(unit)).setScale(0, RoundingMode.HALF_UP).longValue();
            }
            case PER_PERSON -> {
                long rate = perPersonRateVnd != null && perPersonRateVnd > 0 ? perPersonRateVnd : 100_000L;
                int people = Math.max(1, occupantCount);
                yield rate * people;
            }
        };
    }

    public static long calculateWaterCost(
            BigDecimal consumptionM3,
            PricingModel model,
            Long fixedWaterUnitPriceVnd,
            Long perPersonWaterRateVnd,
            int occupantCount
    ) {
        if (consumptionM3 == null || consumptionM3.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("San luong nuoc tieu thu khong hop le");
        }
        PricingModel resolved = model != null ? model : PricingModel.EVN_TIERED;
        if (resolved == PricingModel.PER_PERSON) {
            long rate = perPersonWaterRateVnd != null && perPersonWaterRateVnd > 0 ? perPersonWaterRateVnd : 100_000L;
            return rate * Math.max(1, occupantCount);
        }
        long unit = fixedWaterUnitPriceVnd != null && fixedWaterUnitPriceVnd > 0 ? fixedWaterUnitPriceVnd : 25_000L;
        return consumptionM3.multiply(BigDecimal.valueOf(unit)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static long calculateEvnTieredElectricity(BigDecimal kwh) {
        double remaining = kwh.doubleValue();
        double[] caps = {50, 50, 100, 100, 100, Double.MAX_VALUE};
        long[] rates = {1806L, 1866L, 2167L, 2729L, 3050L, 3151L};
        double total = 0.0;
        for (int i = 0; i < caps.length && remaining > 0; i++) {
            double slice = Math.min(remaining, caps[i]);
            total += slice * rates[i];
            remaining -= slice;
        }
        return Math.round(total);
    }
}

