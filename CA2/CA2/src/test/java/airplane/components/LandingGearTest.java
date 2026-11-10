package airplane.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandingGearTest {
    @Test
    void newGearIsDeployed() {
        assertTrue(new LandingGear().isDeployed());
    }

    @Test
    void retractAndDeployUpdateGearState() {
        LandingGear gear = new LandingGear();

        gear.retract();
        assertFalse(gear.isDeployed());

        gear.deploy();
        assertTrue(gear.isDeployed());
    }
}
