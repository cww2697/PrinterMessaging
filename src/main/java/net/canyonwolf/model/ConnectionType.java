package net.canyonwolf.model;

public enum ConnectionType {
    IP("Network / IP (Ethernet / Wi-Fi)"),
    USB("USB Port / Printer Name"),
    SERIAL("Serial / COM Port");

    private final String displayName;

    ConnectionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ConnectionType fromString(String str) {
        if (str == null) return IP;
        for (ConnectionType type : values()) {
            if (type.name().equalsIgnoreCase(str.trim()) || type.displayName.equalsIgnoreCase(str.trim())) {
                return type;
            }
        }
        return IP;
    }
}
