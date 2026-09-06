package net.canyonwolf.printer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import net.canyonwolf.config.AppConfig;
import net.canyonwolf.format.MessageFormatter;
import net.canyonwolf.model.TextMessage;

public class DesktopPrinterService implements PrinterService {

    private final AppConfig config;

    public DesktopPrinterService(AppConfig config) {
        this.config = config != null ? config : new AppConfig();
    }

    public static PrintService[] getAvailablePrinters() {
        try {
            return PrintServiceLookup.lookupPrintServices(null, null);
        } catch (Exception e) {
            return new PrintService[0];
        }
    }

    public static PrintService getDefaultPrinter() {
        try {
            return PrintServiceLookup.lookupDefaultPrintService();
        } catch (Exception e) {
            return null;
        }
    }

    public PrintService resolvePrintService() {
        String targetName = config.getDesktopPrinterName();
        if (targetName == null || targetName.trim().isEmpty()) {
            targetName = config.getAddress();
        }

        PrintService[] services = getAvailablePrinters();
        if (targetName != null && !targetName.trim().isEmpty()) {
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(targetName.trim()) ||
                    service.getName().toLowerCase().contains(targetName.trim().toLowerCase())) {
                    return service;
                }
            }
        }

        PrintService defaultService = getDefaultPrinter();
        if (defaultService != null) return defaultService;

        if (services.length > 0) return services[0];
        return null;
    }

    @Override
    public void printMessage(TextMessage message) throws Exception {
        // Try OS PrintService printing first
        PrintService printService = resolvePrintService();

        if (printService != null) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(printService);
            job.setJobName("Text Message from " + message.getSender());
            job.setPrintable(new TextMessagePrintable(message));
            job.print();
        } else {
            String formattedText = MessageFormatter.formatPlainMessage(message, 60);
            try (PrinterStreamFactory.StreamHandle handle = PrinterStreamFactory.openStream(config)) {
                OutputStream out = handle.getOutputStream();
                out.write(formattedText.getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }
        }
    }

    @Override
    public void printRaw(String text) throws Exception {
        try (PrinterStreamFactory.StreamHandle handle = PrinterStreamFactory.openStream(config)) {
            OutputStream out = handle.getOutputStream();
            String ascii = MessageFormatter.toAscii(text);
            out.write(ascii.getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }
    }

    @Override
    public void testPrint() throws Exception {
        TextMessage testMsg = new TextMessage(
                config.getSenderName(),
                "This is a test message from the Printer Messaging System!\nConfiguration:\n" +
                        "Type: " + config.getPrinterType().name() + "\n" +
                        "Printer: " + (config.getDesktopPrinterName().isEmpty() ? "Default OS Printer" : config.getDesktopPrinterName())
        );
        printMessage(testMsg);
    }

    public static class TextMessagePrintable implements Printable {
        private final TextMessage message;

        public TextMessagePrintable(TextMessage message) {
            this.message = message;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double pageWidth = pageFormat.getImageableWidth();
            int margin = 40;
            int printX = margin;
            int printY = margin;
            int printWidth = (int) (pageWidth - (margin * 2));
            if (printWidth <= 100) printWidth = 500;

            Font headerFont = new Font(Font.MONOSPACED, Font.BOLD, 14);
            Font metaFont = new Font(Font.MONOSPACED, Font.PLAIN, 11);
            Font bodyFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
            Font footerFont = new Font(Font.MONOSPACED, Font.ITALIC, 9);

            g2d.setColor(Color.BLACK);

            g2d.setFont(headerFont);
            FontMetrics fmHeader = g2d.getFontMetrics();
            int currentY = printY + fmHeader.getAscent();
            g2d.drawString("TEXT MESSAGE", printX, currentY);

            currentY += 22;
            g2d.setFont(metaFont);
            FontMetrics fmMeta = g2d.getFontMetrics();
            g2d.drawString("From: " + message.getSender(), printX, currentY);

            currentY += fmMeta.getHeight() + 2;
            g2d.drawString("Date: " + message.getFormattedTimestamp(), printX, currentY);

            currentY += 10;
            g2d.drawLine(printX, currentY, printX + printWidth, currentY);
            currentY += 16;

            g2d.setFont(bodyFont);
            FontMetrics fmBody = g2d.getFontMetrics();
            List<String> wrapped = wrapLines(message.getText(), fmBody, printWidth);

            for (String line : wrapped) {
                g2d.drawString(line, printX, currentY);
                currentY += fmBody.getHeight() + 2;
            }

            currentY += 10;
            g2d.drawLine(printX, currentY, printX + printWidth, currentY);
            currentY += 15;
            g2d.setFont(footerFont);
            g2d.drawString("Printed via PrinterMessaging", printX, currentY);

            return PAGE_EXISTS;
        }

        private List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
            List<String> result = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                result.add("");
                return result;
            }

            String[] paragraphs = text.split("\\r?\\n", -1);
            for (String p : paragraphs) {
                if (p.isEmpty()) {
                    result.add("");
                    continue;
                }
                String[] words = p.split(" ");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    String testLine = sb.length() == 0 ? word : sb + " " + word;
                    if (fm.stringWidth(testLine) <= maxWidth) {
                        sb = new StringBuilder(testLine);
                    } else {
                        if (sb.length() > 0) {
                            result.add(sb.toString());
                        }
                        sb = new StringBuilder(word);
                    }
                }
                if (sb.length() > 0) {
                    result.add(sb.toString());
                }
            }
            return result;
        }
    }
}
