package gui.map.input.controls;

import domain.types.ExplosiveType;
import domain.types.PreventionType;
import domain.types.VentilationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Headless offscreen paint coverage for the remaining simple map input buttons
 * ({@code AbstractToggleButton}/{@code AbstractStickyFireButton} subclasses). Same technique as
 * {@link gui.map.input.HeadlessSwingButtonTest#paintComponent_PaintsOffscreenWithoutExceptionHeadless}.
 */
class ToggleButtonPaintTest {
    private static final int SIZE = 19;

    @ParameterizedTest(name = "type={0}, selected={1}")
    @EnumSource(ExplosiveType.class)
    void explosionButton_PaintsOffscreenWithoutExceptionHeadless(ExplosiveType type) {
        ExplosionButton unselected = new ExplosionButton(type);
        unselected.setSize(SIZE, SIZE);
        paintOffscreen(unselected);

        ExplosionButton selected = new ExplosionButton(type);
        selected.setSize(SIZE, SIZE);
        selected.doClick();
        paintOffscreen(selected);
    }

    @ParameterizedTest(name = "selected={0}")
    @ValueSource(booleans = {false, true})
    void fireButton_PaintsOffscreenWithoutExceptionHeadless(boolean selected) {
        FireButton button = new FireButton();
        button.setSize(SIZE, SIZE);
        if (selected)
            button.doClick();
        paintOffscreen(button);
    }

    @ParameterizedTest(name = "type={0}")
    @EnumSource(PreventionType.class)
    void preventionButton_PaintsOffscreenWithoutExceptionHeadless(PreventionType type) {
        PreventionButton unselected = new PreventionButton(type);
        unselected.setSize(SIZE, SIZE);
        paintOffscreen(unselected);

        PreventionButton selected = new PreventionButton(type);
        selected.setSize(SIZE, SIZE);
        selected.doClick();
        paintOffscreen(selected);
    }

    @ParameterizedTest(name = "type={0}")
    @EnumSource(VentilationType.class)
    void ventilationButton_PaintsOffscreenWithoutExceptionHeadless(VentilationType type) {
        VentilationButton unselected = new VentilationButton(type, SIZE);
        unselected.setSize(SIZE, SIZE);
        paintOffscreen(unselected);

        VentilationButton selected = new VentilationButton(type, SIZE);
        selected.setSize(SIZE, SIZE);
        selected.doClick();
        paintOffscreen(selected);
    }

    @ParameterizedTest(name = "selected={0}")
    @ValueSource(booleans = {false, true})
    void evacuationButton_PaintsOffscreenWithoutExceptionHeadless(boolean selected) {
        EvacuationButton button = new EvacuationButton();
        button.setSize(SIZE, SIZE);
        if (selected)
            button.doClick();
        paintOffscreen(button);
    }

    private static void paintOffscreen(AbstractToggleButton button) {
        BufferedImage offscreen = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = offscreen.createGraphics();
        assertDoesNotThrow(() -> button.paintComponent(graphics));
        graphics.dispose();
    }
}
