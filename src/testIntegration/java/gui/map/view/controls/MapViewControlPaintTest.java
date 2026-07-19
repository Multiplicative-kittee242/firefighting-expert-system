package gui.map.view.controls;

import config.enums.HydrantLabelSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Headless offscreen paint coverage for map <em>view</em> controls (hydrant toggle buttons and
 * labels). Titles use domain-shaped values ({@code hydr_f}, {@code hydr_d1}) because
 * {@link HydrantToggleButton#drawTitle} and {@link HydrantOutletsLabel} call {@code title.substring(5)}
 * when the glyph size is not 1.
 */
class MapViewControlPaintTest {
    private static final String HYDRANT_TITLE = "hydr_d1";
    private static final int BUTTON_WIDTH = 66;
    private static final int BUTTON_HEIGHT = 26;
    private static final int LABEL_WIDTH = 80;
    private static final int LABEL_HEIGHT = 30;

    @ParameterizedTest(name = "toggleSize={0}")
    @ValueSource(ints = {1, 2})
    void fireHoseButton_PaintsOffscreenWithoutExceptionHeadless(int toggleSize) {
        paintHydrantButton(FireHoseButton::new, toggleSize);
    }

    @ParameterizedTest(name = "toggleSize={0}")
    @ValueSource(ints = {1, 2})
    void hydrExtBorderFromButton_PaintsOffscreenWithoutExceptionHeadless(int toggleSize) {
        paintHydrantButton(HydrExtBorderFromButton::new, toggleSize);
    }

    @ParameterizedTest(name = "toggleSize={0}")
    @ValueSource(ints = {1, 2})
    void hydrExtBorderToButton_PaintsOffscreenWithoutExceptionHeadless(int toggleSize) {
        paintHydrantButton(HydrExtBorderToButton::new, toggleSize);
    }

    @ParameterizedTest(name = "toggleSize={0}")
    @ValueSource(ints = {1, 2})
    void hydrExtButton_PaintsOffscreenWithoutExceptionHeadless(int toggleSize) {
        paintHydrantButton(HydrExtButton::new, toggleSize);
    }

    @ParameterizedTest(name = "size={0}")
    @EnumSource(HydrantLabelSize.class)
    void firefightingStepLabel_PaintsOffscreenWithoutExceptionHeadless(HydrantLabelSize size) {
        FirefightingStepLabel label = new FirefightingStepLabel("A", "1", size);
        label.setLabels("D", "2");
        label.setSize(LABEL_WIDTH, LABEL_HEIGHT);
        paintOffscreen(label, LABEL_WIDTH, LABEL_HEIGHT);
    }

    @ParameterizedTest(name = "size={0}, balanced={1}")
    @CsvSource({
        "FULL, true",
        "FULL, false",
        "SHORT, true",
        "SHORT, false"
    })
    void frontlineBalanceLabel_PaintsOffscreenWithoutExceptionHeadless(HydrantLabelSize size, boolean balanced) {
        FrontlineBalanceLabel label = new FrontlineBalanceLabel(0, 0, size);
        if (balanced) {
            label.setNumbers(2, 0);
        } else {
            label.setNumbers(1, 2);
        }
        label.setSize(LABEL_WIDTH, LABEL_HEIGHT);
        paintOffscreen(label, LABEL_WIDTH, LABEL_HEIGHT);
    }

    @ParameterizedTest(name = "size={0}, free={1}")
    @CsvSource({
        "1, 0",
        "1, 1",
        "3, 0",
        "3, 2",
        "3, 3"
    })
    void hydrantOutletsLabel_PaintsOffscreenWithoutExceptionHeadless(int size, int free) {
        HydrantOutletsLabel label = new HydrantOutletsLabel(0, size);
        label.setNumbers(String.valueOf(free), HYDRANT_TITLE);
        label.setSize(LABEL_WIDTH, LABEL_HEIGHT);
        paintOffscreen(label, LABEL_WIDTH, LABEL_HEIGHT);
    }

    @Test
    void hydrantOutletsLabel_SizeOne_UsesSubstringOfTitle() {
        HydrantOutletsLabel label = new HydrantOutletsLabel(1, 1);
        label.setNumbers("1", "hydr_f");
        label.setSize(LABEL_WIDTH, LABEL_HEIGHT);
        paintOffscreen(label, LABEL_WIDTH, LABEL_HEIGHT);
    }

    private static void paintHydrantButton(Function<String, HydrantToggleButton> factory, int toggleSize) {
        HydrantToggleButton button = factory.apply(HYDRANT_TITLE);
        button.setSize(toggleSize);
        button.setBounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);

        paintOffscreen(button, BUTTON_WIDTH, BUTTON_HEIGHT);

        button.doClick();
        paintOffscreen(button, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private static void paintOffscreen(java.awt.Component component, int width, int height) {
        BufferedImage offscreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = offscreen.createGraphics();
        // JLabel keeps paintComponent protected; Component#paint is public and cascades into it.
        assertDoesNotThrow(() -> component.paint(graphics));
        graphics.dispose();
    }
}
