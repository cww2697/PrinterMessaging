package net.canyonwolf;

public class PrinterFactory {
    public static PrinterService createPrinterService(AppConfig config) {
        if (config == null) {
            config = new AppConfig();
        }

        if (config.getPrinterType() == PrinterType.DESKTOP) {
            return new DesktopPrinterService(config);
        } else {
            return new ReceiptPrinterService(config);
        }
    }
}
