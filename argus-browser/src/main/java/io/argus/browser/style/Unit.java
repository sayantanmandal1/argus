package io.argus.browser.style;

/** CSS length units. {@link #NONE} represents a unit-less number (e.g. {@code line-height: 1.5}). */
public enum Unit {
    PX,
    EM,
    REM,
    EX,
    PT,
    PERCENT,
    NONE;

    static Unit fromToken(String token) {
        if (token == null || token.isEmpty()) {
            return NONE;
        }
        return switch (token) {
            case "px" -> PX;
            case "em" -> EM;
            case "rem" -> REM;
            case "ex" -> EX;
            case "pt" -> PT;
            case "%" -> PERCENT;
            default -> NONE;
        };
    }
}
