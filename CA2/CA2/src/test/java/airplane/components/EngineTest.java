package airplane.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    @Test
    void newEngineIsOperationalAndStopped() {
        Engine engine = new Engine();

        assertTrue(engine.isOperational());
        assertFalse(engine.isRunning());
    }

    @Test
    void startAndStopUpdateRunningState() {
        Engine engine = new Engine();

        engine.start();
        assertTrue(engine.isRunning());

        engine.stop();
        assertFalse(engine.isRunning());
        assertTrue(engine.isOperational());
    }

    @Test
    void nonOperationalEngineCannotStart() {
        Engine engine = new Engine(false);

        assertThrows(IllegalStateException.class, engine::start);

        assertFalse(engine.isOperational());
        assertFalse(engine.isRunning());
    }

    @Test
    void failureStopsEngineAndPreventsRestart() {
        Engine engine = new Engine();
        engine.start();

        engine.fail();

        assertFalse(engine.isOperational());
        assertFalse(engine.isRunning());
        assertThrows(IllegalStateException.class, engine::start);
    }
}
