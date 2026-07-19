package gui.map;

public interface Visible {

    void show();

    void hide();

    default void showIfEnabled() {
        show();
    }
}
