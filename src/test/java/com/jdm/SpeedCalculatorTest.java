package com.jdm;

import com.jdm.util.SpeedCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeedCalculatorTest {

    @Test
    void testSpeedAndEtaCalculation() throws InterruptedException {
        SpeedCalculator calc = new SpeedCalculator();
        calc.recordSample(0);

        Thread.sleep(200);
        calc.recordSample(200_000); // 200 KB in ~200ms -> ~1 MB/s

        double speed = calc.getCurrentSpeedBps();
        assertTrue(speed > 500_000, "Speed should be around ~1MB/s, got: " + speed);

        long eta = calc.calculateEtaSeconds(2_000_000);
        assertTrue(eta >= 1 && eta <= 5, "ETA should be around ~2 seconds, got: " + eta);
    }

    @Test
    void testZeroOrSingleSample() {
        SpeedCalculator calc = new SpeedCalculator();
        assertEquals(0.0, calc.getCurrentSpeedBps());
        assertEquals(-1, calc.calculateEtaSeconds(1000));

        calc.recordSample(100);
        assertEquals(0.0, calc.getCurrentSpeedBps());
    }
}
