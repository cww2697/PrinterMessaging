package net.canyonwolf.model;

public enum PrinterType {
    RECEIPT("Receipt Printer (ESC/POS Thermal)"),
    VERIFONE("Verifone Printer (Printer 250 / 900 / Dot Matrix)"),
    DESKTOP("Standard Desktop Printer (Letter/A4 Page)");

    private final String displayName;

    PrinterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PrinterType fromString(String str) {
        if (str == null) return RECEIPT;
        String trimmed = str.trim().toLowerCase();
        if (trimmed.contains("verifone") || trimmed.contains("p900") || trimmed.contains("p250") ||
            trimmed.equals("900") || trimmed.equals("250") || trimmed.contains("matrix") || trimmed.contains("impact")) {
            return VERIFONE;
        }
        for (PrinterType type : values()) {
            if (type.name().equalsIgnoreCase(str.trim()) || type.displayName.equalsIgnoreCase(str.trim())) {
                return type;
            }
        }
        return RECEIPT;
    }
}
