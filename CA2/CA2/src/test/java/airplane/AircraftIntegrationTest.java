package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftIntegrationTest {
    private Engine engine;
    private FuelSystem fuelSystem;
    private LandingGear landingGear;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engine = new Engine();
        fuelSystem = new FuelSystem(100.0, 30.0);
        landingGear = new LandingGear();
        aircraft = new Aircraft(engine, fuelSystem, landingGear, new NavigationSystem("Tehran"));
    }

    @Test
    void completeFlightUpdatesRealComponents() {
        aircraft.prepareForTakeoff();
        assertEquals(FlightStatus.READY, aircraft.getStatus());
        assertFalse(engine.isRunning());
        assertTrue(landingGear.isDeployed());

        aircraft.takeOff();
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        assertTrue(engine.isRunning());
        assertFalse(landingGear.isDeployed());

        aircraft.cruise(20.0);
        aircraft.cruise(15.5);
        assertEquals(64.5, fuelSystem.getRemainingFuel(), 0.000001);

        aircraft.land();
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertFalse(engine.isRunning());
        assertTrue(landingGear.isDeployed());
    }

    @Test
    void realEngineFailureTriggersEmergencyLanding() {
        aircraft.prepareForTakeoff();
        aircraft.takeOff();
        engine.fail();

        aircraft.cruise(10.0);

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertFalse(engine.isRunning());
        assertTrue(landingGear.isDeployed());
        assertEquals(100.0, fuelSystem.getRemainingFuel(), 0.000001);
        assertThrows(FlightException.class, aircraft::prepareForTakeoff);
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
    }

    @Test
    void realFuelShortageTriggersEmergencyLandingAndPreservesFuel() {
        aircraft.prepareForTakeoff();
        aircraft.takeOff();

        FlightException exception = assertThrows(FlightException.class,
                () -> aircraft.cruise(101.0));

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertFalse(engine.isRunning());
        assertTrue(landingGear.isDeployed());
        assertEquals(100.0, fuelSystem.getRemainingFuel(), 0.000001);
    }

    @Test
    void fuelBelowThresholdPreventsAnotherTakeoffAfterLanding() {
        aircraft.prepareForTakeoff();
        aircraft.takeOff();
        aircraft.cruise(75.0);
        aircraft.land();

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertFalse(engine.isRunning());
        assertTrue(landingGear.isDeployed());
        assertEquals(25.0, fuelSystem.getRemainingFuel(), 0.000001);
    }
}
