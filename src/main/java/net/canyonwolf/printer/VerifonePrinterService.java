package net.canyonwolf.printer;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import net.canyonwolf.config.AppConfig;
import net.canyonwolf.format.MessageFormatter;
import net.canyonwolf.model.TextMessage;

public class VerifonePrinterService implements PrinterService {

    private final AppConfig config;

    public VerifonePrinterService(AppConfig config) {
        this.config = config != null ? config : new AppConfig();
    }

    public byte[] buildPayload(TextMessage message) {
        int width = config.getReceiptWidth();
        if (width <= 0 || width > 42) {
            width = 42; // Verifone 250 / 900 column width
        }

        String receipt = MessageFormatter.formatReceiptMessage(message, width);
        // Normalize line endings to CRLF for serial impact printers
        receipt = receipt.replace("\r\n", "\n").replace("\n", "\r\n");

        StringBuilder sb = new StringBuilder(receipt);
        int feed = config.getFeedLines();
        for (int i = 0; i < feed; i++) {
            sb.append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public void printMessage(TextMessage message) throws Exception {
        byte[] payload = buildPayload(message);
        writeStreaming(payload);
    }

    @Override
    public void printRaw(String text) throws Exception {
        String ascii = MessageFormatter.toAscii(text).replace("\r\n", "\n").replace("\n", "\r\n");
        byte[] payload = ascii.getBytes(StandardCharsets.US_ASCII);
        writeStreaming(payload);
    }

    public static java.util.List<byte[]> splitIntoChunks(byte[] payload, int maxLinesPerChunk) {
        java.util.List<byte[]> chunks = new java.util.ArrayList<>();
        if (payload == null || payload.length == 0) {
            return chunks;
        }

        String content = new String(payload, StandardCharsets.US_ASCII);
        String[] lines = content.split("\r\n", -1);

        int linesInChunk = 0;
        StringBuilder chunk = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            chunk.append(lines[i]);
            if (i < lines.length - 1) {
                chunk.append("\r\n");
                linesInChunk++;
            }

            if (linesInChunk >= maxLinesPerChunk || i == lines.length - 1) {
                if (chunk.length() > 0) {
                    chunks.add(chunk.toString().getBytes(StandardCharsets.US_ASCII));
                    chunk.setLength(0);
                    linesInChunk = 0;
                }
            }
        }
        return chunks;
    }

    private void writeStreaming(byte[] payload) throws Exception {
        try (PrinterStreamFactory.StreamHandle handle = PrinterStreamFactory.openStream(config)) {
            OutputStream out = handle.getOutputStream();
            boolean isSerial = config.getConnectionType() == net.canyonwolf.model.ConnectionType.SERIAL;

            if (!isSerial) {
                out.write(payload);
                out.flush();
                return;
            }

            // Stream no more than 3 lines at a time for serial verifone
            java.util.List<byte[]> chunks = splitIntoChunks(payload, 3);
            for (byte[] chunk : chunks) {
                out.write(chunk);
                out.flush();

                int lineCount = 0;
                for (byte b : chunk) {
                    if (b == '\n') {
                        lineCount++;
                    }
                }
                if (lineCount == 0) {
                    lineCount = 1;
                }
                // Dot matrix printers print ~2 lines/sec; delay ~500ms per line to prevent buffer overflow
                long delay = Math.max(500L, lineCount * 500L);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            // Allow trailing feed lines and mechanical printing to finish before closing port
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void testPrint() throws Exception {
        TextMessage testMsg = new TextMessage(
                config.getSenderName(),
                "This is a test message from the Printer Messaging System!\nConfiguration:\n" +
                        "Type: " + config.getPrinterType().name() + "\n" +
                        "Connection: " + config.getConnectionType().name() + " (" + config.getAddress() + " @ " + config.getBaudRate() + " baud)"
        );
        printMessage(testMsg);
    }
}
