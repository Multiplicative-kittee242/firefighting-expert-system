package gui.map.input.controls;

import config.enums.DoorDirection;
import config.enums.DoorOrientation;
import config.enums.DoorSide;
import gui.map.values.DoorKeepType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Headless paint coverage for {@link DoorSealingButton} — the largest untested map control.
 * Parameterizes over the real enum/boolean axes that {@code drawVerticalDoor}/{@code
 * drawHorizontalDoor} branch on (fire-rated, orientation, side, direction, keep-type, selected).
 */
class DoorSealingButtonPaintTest {
    private static final int WIDTH = 32;
    private static final int HEIGHT = 32;

    private static Stream<Arguments> paintComponent_PaintsOffscreenWithoutExceptionHeadless() {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (boolean fireRated : new boolean[]{false, true}) {
            for (DoorOrientation orientation : DoorOrientation.values()) {
                for (DoorSide side : DoorSide.values()) {
                    for (DoorDirection direction : DoorDirection.values()) {
                        for (DoorKeepType keepType : DoorKeepType.values()) {
                            for (boolean selected : new boolean[]{false, true}) {
                                cases.add(Arguments.of(fireRated, orientation, side, direction, keepType, selected));
                            }
                        }
                    }
                }
            }
        }
        return cases.build();
    }

    @ParameterizedTest(name = "fireRated={0}, {1}, side={2}, dir={3}, keep={4}, selected={5}")
    @MethodSource
    void paintComponent_PaintsOffscreenWithoutExceptionHeadless(boolean fireRated, DoorOrientation orientation,
        DoorSide side, DoorDirection direction, DoorKeepType keepType, boolean selected)
    {
        DoorSealingButton button = new DoorSealingButton("AB", fireRated, orientation, side, direction, keepType);
        button.setSize(WIDTH, HEIGHT);
        if (selected)
            button.doClick();

        BufferedImage offscreen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = offscreen.createGraphics();

        assertDoesNotThrow(() -> button.paintComponent(graphics));
        graphics.dispose();
    }
}
