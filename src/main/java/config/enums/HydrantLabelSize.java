package config.enums;

/**
 * Display size of a hydrant / extinguisher count label, controlling its rendered width and how much detail is drawn.
 */
public enum HydrantLabelSize {
    FULL("full"),
    SHORT("short");

    private final String code;
    private final String titleFormatKey;

    HydrantLabelSize(String code) {
        this.code = code;
        this.titleFormatKey = "label.hydr.count." + code;
    }

    public String getCode() {
        return code;
    }

    public String getTitleFormatKey() {
        return titleFormatKey;
    }
}
