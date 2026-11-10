package airplane.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NavigationSystemTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void constructorRejectsMissingDestination(String destination) {
        assertThrows(IllegalArgumentException.class,
                () -> new NavigationSystem(destination));
    }

    @Test
    void routeIsAvailableByDefaultAndDestinationIsPreserved() {
        NavigationSystem navigation = new NavigationSystem("Shiraz");

        assertEquals("Shiraz", navigation.getDestination());
        assertTrue(navigation.isRouteAvailable());
    }

    @Test
    void constructorCanCreateUnavailableRoute() {
        NavigationSystem navigation = new NavigationSystem("Shiraz", false);

        assertFalse(navigation.isRouteAvailable());
        assertEquals("Shiraz", navigation.getDestination());
    }

    @Test
    void routeAvailabilityCanChangeInBothDirections() {
        NavigationSystem navigation = new NavigationSystem("Shiraz");

        navigation.setRouteAvailable(false);
        assertFalse(navigation.isRouteAvailable());

        navigation.setRouteAvailable(true);
        assertTrue(navigation.isRouteAvailable());
        assertEquals("Shiraz", navigation.getDestination());
    }
}
