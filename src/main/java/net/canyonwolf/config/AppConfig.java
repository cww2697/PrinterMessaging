package net.canyonwolf.config;

import java.util.Properties;
import net.canyonwolf.model.ConnectionType;
import net.canyonwolf.model.PrinterType;

public class AppConfig {
    public static final String KEY_PRINTER_TYPE = "printer.type";
    public static final String KEY_CONNECTION_TYPE = "connection.type";
    public static final String KEY_ADDRESS = "connection.address";
    public static final String KEY_PORT = "connection.port";
    public static final String KEY_SENDER_NAME = "message.sender.name";
    public static final String KEY_RECEIPT_WIDTH = "receipt.width";
    public static final String KEY_AUTO_CUT = "receipt.autoCut";
    public static final String KEY_FEED_LINES = "receipt.feedLines";
    public static final String KEY_DESKTOP_PRINTER_NAME = "desktop.printer.name";

    private PrinterType printerType = PrinterType.RECEIPT;
    private ConnectionType connectionType = ConnectionType.IP;
    private String address = "192.168.2.26";
    private int port = 9100;
    private String senderName = "User";
    private int receiptWidth = 42;
    private boolean autoCut = true;
    private int feedLines = 4;
    private String desktopPrinterName = "";

    public AppConfig() {
    }

    public static AppConfig fromProperties(Properties props) {
        AppConfig config = new AppConfig();
        if (props == null) return config;

        String pType = props.getProperty(KEY_PRINTER_TYPE);
        if (pType != null && !pType.trim().isEmpty()) {
            config.setPrinterType(PrinterType.fromString(pType));
        }

        String cType = props.getProperty(KEY_CONNECTION_TYPE);
        if (cType != null && !cType.trim().isEmpty()) {
            config.setConnectionType(ConnectionType.fromString(cType));
        }

        String addr = props.getProperty(KEY_ADDRESS);
        if (addr != null && !addr.trim().isEmpty()) {
            config.setAddress(addr.trim());
        }

        String portStr = props.getProperty(KEY_PORT);
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                config.setPort(Integer.parseInt(portStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String sender = props.getProperty(KEY_SENDER_NAME);
        if (sender != null && !sender.trim().isEmpty()) {
            config.setSenderName(sender.trim());
        }

        String widthStr = props.getProperty(KEY_RECEIPT_WIDTH);
        if (widthStr != null && !widthStr.trim().isEmpty()) {
            try {
                config.setReceiptWidth(Integer.parseInt(widthStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String cutStr = props.getProperty(KEY_AUTO_CUT);
        if (cutStr != null && !cutStr.trim().isEmpty()) {
            config.setAutoCut(Boolean.parseBoolean(cutStr.trim()));
        }

        String feedStr = props.getProperty(KEY_FEED_LINES);
        if (feedStr != null && !feedStr.trim().isEmpty()) {
            try {
                config.setFeedLines(Integer.parseInt(feedStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String dPrinter = props.getProperty(KEY_DESKTOP_PRINTER_NAME);
        if (dPrinter != null) {
            config.setDesktopPrinterName(dPrinter.trim());
        }

        return config;
    }

    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty(KEY_PRINTER_TYPE, printerType.name());
        props.setProperty(KEY_CONNECTION_TYPE, connectionType.name());
        props.setProperty(KEY_ADDRESS, address != null ? address : "");
        props.setProperty(KEY_PORT, String.valueOf(port));
        props.setProperty(KEY_SENDER_NAME, senderName != null ? senderName : "User");
        props.setProperty(KEY_RECEIPT_WIDTH, String.valueOf(receiptWidth));
        props.setProperty(KEY_AUTO_CUT, String.valueOf(autoCut));
        props.setProperty(KEY_FEED_LINES, String.valueOf(feedLines));
        props.setProperty(KEY_DESKTOP_PRINTER_NAME, desktopPrinterName != null ? desktopPrinterName : "");
        return props;
    }

    public PrinterType getPrinterType() {
        return printerType;
    }

    public void setPrinterType(PrinterType printerType) {
        if (printerType != null) {
            this.printerType = printerType;
        }
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        if (connectionType != null) {
            this.connectionType = connectionType;
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getReceiptWidth() {
        return receiptWidth;
    }

    public void setReceiptWidth(int receiptWidth) {
        if (receiptWidth > 10) {
            this.receiptWidth = receiptWidth;
        }
    }

    public boolean isAutoCut() {
        return autoCut;
    }

    public void setAutoCut(boolean autoCut) {
        this.autoCut = autoCut;
    }

    public int getFeedLines() {
        return feedLines;
    }

    public void setFeedLines(int feedLines) {
        if (feedLines >= 0) {
            this.feedLines = feedLines;
        }
    }

    public String getDesktopPrinterName() {
        return desktopPrinterName;
    }

    public void setDesktopPrinterName(String desktopPrinterName) {
        this.desktopPrinterName = desktopPrinterName;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Printer Type:    ").append(printerType.getDisplayName()).append("\n");
        sb.append("Connection:      ").append(connectionType.getDisplayName()).append("\n");
        if (connectionType == ConnectionType.IP) {
            sb.append("IP Address:      ").append(address).append("\n");
            sb.append("Port:            ").append(port).append("\n");
        } else if (connectionType == ConnectionType.USB) {
            sb.append("USB Device/Name: ").append(address).append("\n");
        } else if (connectionType == ConnectionType.SERIAL) {
            sb.append("Serial Port:     ").append(address).append("\n");
        }
        if (printerType == PrinterType.DESKTOP && desktopPrinterName != null && !desktopPrinterName.isEmpty()) {
            sb.append("Desktop Printer: ").append(desktopPrinterName).append("\n");
        }
        sb.append("Sender Name:     ").append(senderName).append("\n");
        if (printerType == PrinterType.RECEIPT) {
            sb.append("Auto Cut Paper:  ").append(autoCut ? "Yes" : "No").append("\n");
            sb.append("Feed Lines:      ").append(feedLines);
        }
        return sb.toString();
    }
}
