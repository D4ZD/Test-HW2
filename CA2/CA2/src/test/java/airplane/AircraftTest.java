package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftTest {
    private Engine engine;
    private FuelSystem fuelSystem;
    private LandingGear landingGear;
    private NavigationSystem navigationSystem;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engine = mock(Engine.class);
        fuelSystem = mock(FuelSystem.class);
        landingGear = mock(LandingGear.class);
        navigationSystem = mock(NavigationSystem.class);
        aircraft = new Aircraft(engine, fuelSystem, landingGear, navigationSystem);
    }

    @Test
    void newAircraftIsGroundedWithoutUsingComponents() {
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verifyNoInteractions(engine, fuelSystem, landingGear, navigationSystem);
    }

    @ParameterizedTest
    @ValueSource(strings = {"engine", "fuelSystem", "landingGear", "navigationSystem"})
    void constructorRejectsNullComponents(String component) {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new Aircraft(
                        component.equals("engine") ? null : engine,
                        component.equals("fuelSystem") ? null : fuelSystem,
                        component.equals("landingGear") ? null : landingGear,
                        component.equals("navigationSystem") ? null : navigationSystem));

        assertEquals(component + " must not be null", exception.getMessage());
        verifyNoInteractions(engine, fuelSystem, landingGear, navigationSystem);
    }

    @Test
    void preparationChecksComponentsAndMakesAircraftReady() {
        allowPreparation();

        aircraft.prepareForTakeoff();

        assertEquals(FlightStatus.READY, aircraft.getStatus());
        InOrder order = inOrder(engine, fuelSystem, navigationSystem);
        order.verify(engine).isOperational();
        order.verify(fuelSystem).hasEnoughFuel();
        order.verify(navigationSystem).isRouteAvailable();
        verify(engine, never()).start();
        verifyNoInteractions(landingGear);
    }

    @Test
    void preparationStopsAtFailedEngine() {
        when(engine.isOperational()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine, times(1)).isOperational();
        verify(engine, never()).start();
        verifyNoInteractions(fuelSystem, landingGear, navigationSystem);
    }

    @Test
    void preparationStopsAtInsufficientFuel() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine).isOperational();
        verify(fuelSystem).hasEnoughFuel();
        verify(engine, never()).start();
        verifyNoInteractions(landingGear, navigationSystem);
    }

    @Test
    void preparationRejectsUnavailableRoute() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(false);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);

        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(navigationSystem).isRouteAvailable();
        verify(engine, never()).start();
        verifyNoInteractions(landingGear);
    }

    @Test
    void preparationPropagatesComponentFailureWithoutChangingStatus() {
        IllegalStateException failure = new IllegalStateException("Engine sensor unavailable");
        when(engine.isOperational()).thenThrow(failure);

        FlightStatus statusBefore = aircraft.getStatus();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                aircraft::prepareForTakeoff);

        assertSame(failure, exception);
        assertEquals(statusBefore, aircraft.getStatus());
        verify(engine).isOperational();
        verifyNoInteractions(fuelSystem, landingGear, navigationSystem);
    }

    @Test
    void preparationCanBeRetriedAfterFuelBecomesAvailable() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(false, true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());

        aircraft.prepareForTakeoff();

        assertEquals(FlightStatus.READY, aircraft.getStatus());
        verify(engine, times(2)).isOperational();
        verify(fuelSystem, times(2)).hasEnoughFuel();
        verify(navigationSystem, times(1)).isRouteAvailable();
    }

    @Test
    void takeoffStartsEngineBeforeRetractingGear() {
        allowPreparation();
        aircraft.prepareForTakeoff();
        doNothing().when(engine).start();
        doNothing().when(landingGear).retract();

        aircraft.takeOff();

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        InOrder order = inOrder(engine, landingGear);
        order.verify(engine).start();
        order.verify(landingGear).retract();
        verify(engine, times(1)).start();
        verify(landingGear, times(1)).retract();
        verify(engine, never()).stop();
        verify(landingGear, never()).deploy();
    }

    @Test
    void engineStartFailureAbortsTakeoffAndPreservesCause() {
        allowPreparation();
        aircraft.prepareForTakeoff();
        IllegalStateException failure = new IllegalStateException("Engine cannot start");
        doThrow(failure).when(engine).start();

        FlightException exception = assertThrows(FlightException.class, aircraft::takeOff);

        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        assertSame(failure, exception.getCause());
        verify(engine).start();
        verifyNoInteractions(landingGear);
    }

    @Test
    void gearRetractionFailureAbortsTakeoffAndPreservesCause() {
        allowPreparation();
        aircraft.prepareForTakeoff();
        IllegalStateException failure = new IllegalStateException("Gear is stuck");
        doThrow(failure).when(landingGear).retract();

        FlightException exception = assertThrows(FlightException.class, aircraft::takeOff);

        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        assertSame(failure, exception.getCause());
        InOrder order = inOrder(engine, landingGear);
        order.verify(engine).start();
        order.verify(landingGear).retract();
    }

    @Test
    void cruiseConsumesRequestedFuelWithoutLanding() {
        startFlight();
        doNothing().when(fuelSystem).consumeFuel(any(Double.class));

        aircraft.cruise(12.5);

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(engine, times(2)).isOperational();
        verify(fuelSystem, times(1)).consumeFuel(12.5);
        verify(landingGear, never()).deploy();
        verify(engine, never()).stop();
    }

    @Test
    void repeatedCruiseCallsConsumeFuelOncePerCall() {
        startFlight();

        aircraft.cruise(8.0);
        aircraft.cruise(3.5);

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(fuelSystem, times(2)).consumeFuel(anyDouble());
        InOrder order = inOrder(fuelSystem);
        order.verify(fuelSystem).consumeFuel(8.0);
        order.verify(fuelSystem).consumeFuel(3.5);
        verify(engine, times(3)).isOperational();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.5, -100.0})
    void cruiseRejectsNonPositiveAmountBeforeUsingComponents(double amount) {
        startFlight();
        clearInvocations(engine, fuelSystem, landingGear, navigationSystem);

        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(amount));

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verifyNoInteractions(engine, fuelSystem, landingGear, navigationSystem);
    }

    @Test
    void engineFailureDuringCruiseCausesEmergencyLandingWithoutConsumingFuel() {
        when(engine.isOperational()).thenReturn(true, false);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);
        aircraft.prepareForTakeoff();
        aircraft.takeOff();

        assertDoesNotThrow(() -> aircraft.cruise(10.0));

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(engine, times(2)).isOperational();
        verify(fuelSystem, never()).consumeFuel(anyDouble());
        InOrder order = inOrder(landingGear, engine);
        order.verify(landingGear).deploy();
        order.verify(engine).stop();
    }

    @Test
    void fuelFailureDuringCruiseLandsAndWrapsOriginalFailure() {
        startFlight();
        IllegalStateException failure = new IllegalStateException("Fuel pump failed");
        doThrow(failure).when(fuelSystem).consumeFuel(20.0);

        FlightException exception = assertThrows(FlightException.class,
                () -> aircraft.cruise(20.0));

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertSame(failure, exception.getCause());
        InOrder order = inOrder(fuelSystem, landingGear, engine);
        order.verify(fuelSystem).consumeFuel(20.0);
        order.verify(landingGear).deploy();
        order.verify(engine).stop();
        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }

    @Test
    void normalLandingDeploysGearBeforeStoppingEngine() {
        startFlight();

        aircraft.land();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        InOrder order = inOrder(landingGear, engine);
        order.verify(landingGear).deploy();
        order.verify(engine).stop();
        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
        verify(fuelSystem, never()).consumeFuel(anyDouble());
    }

    @Test
    void explicitEmergencyLandingDeploysGearAndStopsEngine() {
        startFlight();

        aircraft.emergencyLand();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        InOrder order = inOrder(landingGear, engine);
        order.verify(landingGear).deploy();
        order.verify(engine).stop();
        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }

    @Test
    void landedAircraftCanPrepareAndTakeOffAgain() {
        startFlight();
        aircraft.land();

        aircraft.prepareForTakeoff();
        assertEquals(FlightStatus.READY, aircraft.getStatus());
        aircraft.takeOff();

        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(engine, times(2)).start();
        verify(landingGear, times(2)).retract();
        verify(engine, times(2)).isOperational();
    }

    @Test
    void resetClearsStubbingAndInvocationsBetweenFlights() {
        startFlight();
        aircraft.land();
        verify(engine, times(1)).start();
        verify(engine, times(1)).stop();

        reset(engine, fuelSystem, landingGear, navigationSystem);
        verifyNoInteractions(engine, fuelSystem, landingGear, navigationSystem);

        assertThrows(FlightException.class, aircraft::prepareForTakeoff);
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(engine, times(1)).isOperational();
        verifyNoInteractions(fuelSystem, landingGear, navigationSystem);

        allowPreparation();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();
        aircraft.land();

        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(engine, times(1)).start();
        verify(engine, times(1)).stop();
        verify(landingGear, times(1)).retract();
        verify(landingGear, times(1)).deploy();
        verify(engine, times(2)).isOperational();
        verify(fuelSystem, times(1)).hasEnoughFuel();
        verify(navigationSystem, times(1)).isRouteAvailable();
    }

    @ParameterizedTest(name = "{0}: {1} is rejected")
    @MethodSource("invalidOperations")
    void invalidLifecycleOperationPreservesStateAndDoesNotUseComponents(
            FlightStatus initialStatus, String operation) {
        moveTo(initialStatus);
        clearInvocations(engine, fuelSystem, landingGear, navigationSystem);

        assertThrows(FlightException.class, () -> perform(operation));

        assertEquals(initialStatus, aircraft.getStatus());
        verifyNoInteractions(engine, fuelSystem, landingGear, navigationSystem);
    }

    private static Stream<Arguments> invalidOperations() {
        return Stream.of(FlightStatus.values()).flatMap(status ->
                Stream.of("prepare", "takeoff", "cruise", "land", "emergency").filter(operation ->
                        switch (operation) {
                            case "prepare" -> status != FlightStatus.GROUNDED
                                    && status != FlightStatus.LANDED;
                            case "takeoff" -> status != FlightStatus.READY;
                            default -> status != FlightStatus.IN_FLIGHT;
                        }).map(operation -> Arguments.of(status, operation)));
    }

    private void allowPreparation() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);
    }

    private void startFlight() {
        allowPreparation();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();
    }

    private void moveTo(FlightStatus status) {
        if (status == FlightStatus.GROUNDED) {
            return;
        }
        allowPreparation();
        aircraft.prepareForTakeoff();
        if (status == FlightStatus.READY) {
            return;
        }
        if (status == FlightStatus.ABORTED) {
            doThrow(new IllegalStateException("Engine cannot start")).when(engine).start();
            assertThrows(FlightException.class, aircraft::takeOff);
            return;
        }
        aircraft.takeOff();
        if (status == FlightStatus.LANDED) {
            aircraft.land();
        }
    }

    private void perform(String operation) {
        switch (operation) {
            case "prepare" -> aircraft.prepareForTakeoff();
            case "takeoff" -> aircraft.takeOff();
            case "cruise" -> aircraft.cruise(5.0);
            case "land" -> aircraft.land();
            case "emergency" -> aircraft.emergencyLand();
            default -> throw new IllegalArgumentException(operation);
        }
    }
}
