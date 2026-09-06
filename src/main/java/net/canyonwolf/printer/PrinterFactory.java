package net.canyonwolf.printer;

import net.canyonwolf.config.AppConfig;
import net.canyonwolf.model.PrinterType;

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
