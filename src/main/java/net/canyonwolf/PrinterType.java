package net.canyonwolf;

public enum PrinterType {
    RECEIPT("Receipt Printer (ESC/POS Thermal)"),
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
        for (PrinterType type : values()) {
            if (type.name().equalsIgnoreCase(str.trim()) || type.displayName.equalsIgnoreCase(str.trim())) {
                return type;
            }
        }
        return RECEIPT;
    }
}
