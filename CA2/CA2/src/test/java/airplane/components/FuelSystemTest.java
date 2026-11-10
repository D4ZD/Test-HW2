package airplane.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FuelSystemTest {
    @ParameterizedTest
    @CsvSource({"-1, 10", "10, -1", "-1, -1"})
    void constructorRejectsNegativeFuelValues(double initialFuel, double minimumFuel) {
        assertThrows(IllegalArgumentException.class,
                () -> new FuelSystem(initialFuel, minimumFuel));
    }

    @ParameterizedTest
    @CsvSource({"29.5, 30, false", "30, 30, true", "30.5, 30, true", "0, 0, true"})
    void fuelAvailabilityIncludesExactThreshold(double initialFuel, double minimumFuel,
                                               boolean expected) {
        FuelSystem fuelSystem = new FuelSystem(initialFuel, minimumFuel);

        assertEquals(expected, fuelSystem.hasEnoughFuel());
        assertEquals(initialFuel, fuelSystem.getRemainingFuel(), 0.000001);
    }

    @Test
    void consumptionReducesFuelByRequestedAmount() {
        FuelSystem fuelSystem = new FuelSystem(40.0, 30.0);

        fuelSystem.consumeFuel(12.5);

        assertEquals(27.5, fuelSystem.getRemainingFuel(), 0.000001);
        assertFalse(fuelSystem.hasEnoughFuel());
    }

    @Test
    void consumingAllRemainingFuelIsAllowed() {
        FuelSystem fuelSystem = new FuelSystem(40.0, 30.0);

        fuelSystem.consumeFuel(40.0);

        assertEquals(0.0, fuelSystem.getRemainingFuel(), 0.000001);
        assertFalse(fuelSystem.hasEnoughFuel());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.5, -10.0})
    void nonPositiveConsumptionDoesNotChangeFuel(double amount) {
        FuelSystem fuelSystem = new FuelSystem(40.0, 30.0);

        assertThrows(IllegalArgumentException.class, () -> fuelSystem.consumeFuel(amount));

        assertEquals(40.0, fuelSystem.getRemainingFuel(), 0.000001);
    }

    @Test
    void excessiveConsumptionDoesNotChangeFuel() {
        FuelSystem fuelSystem = new FuelSystem(40.0, 30.0);

        assertThrows(IllegalStateException.class, () -> fuelSystem.consumeFuel(40.5));

        assertEquals(40.0, fuelSystem.getRemainingFuel(), 0.000001);
    }
}
