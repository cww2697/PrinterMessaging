package net.canyonwolf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ReceiptPrinterService implements PrinterService {

    public static final byte[] ESC_INIT = new byte[]{0x1B, 0x40};               // ESC @ (Initialize)
    public static final byte[] ESC_ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};   // ESC a 0
    public static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01}; // ESC a 1
    public static final byte[] ESC_ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};  // ESC a 2
    public static final byte[] ESC_BOLD_ON = new byte[]{0x1B, 0x45, 0x01};      // ESC E 1
    public static final byte[] ESC_BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};     // ESC E 0
    public static final byte[] ESC_DOUBLE_ON = new byte[]{0x1D, 0x21, 0x11};     // GS ! 0x11 (Double height & width)
    public static final byte[] ESC_NORMAL = new byte[]{0x1D, 0x21, 0x00};        // GS ! 0x00 (Normal)
    public static final byte[] GS_CUT_PARTIAL = new byte[]{0x1D, 0x56, 0x42, 0x00}; // GS V B 0
    public static final byte[] GS_CUT_FULL = new byte[]{0x1D, 0x56, 0x00};          // GS V 0

    private final AppConfig config;

    public ReceiptPrinterService(AppConfig config) {
        this.config = config != null ? config : new AppConfig();
    }

    public byte[] buildEscPosPayload(TextMessage message) {
        int width = config.getReceiptWidth();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(ESC_INIT);

            baos.write(ESC_ALIGN_CENTER);
            baos.write(ESC_BOLD_ON);
            baos.write("=== TEXT MESSAGE ===\n".getBytes(StandardCharsets.US_ASCII));
            baos.write(ESC_BOLD_OFF);

            baos.write(ESC_ALIGN_LEFT);
            baos.write(("FROM: " + message.getSender() + "\n").getBytes(StandardCharsets.US_ASCII));
            baos.write(("DATE: " + message.getFormattedTimestamp() + "\n").getBytes(StandardCharsets.US_ASCII));
            baos.write((MessageFormatter.repeat("-", width) + "\n").getBytes(StandardCharsets.US_ASCII));

            for (String line : MessageFormatter.wrapText(message.getText(), width)) {
                baos.write((line + "\n").getBytes(StandardCharsets.US_ASCII));
            }

            baos.write((MessageFormatter.repeat("=", width) + "\n").getBytes(StandardCharsets.US_ASCII));

            int feed = config.getFeedLines();
            for (int i = 0; i < feed; i++) {
                baos.write("\n".getBytes(StandardCharsets.US_ASCII));
            }

            if (config.isAutoCut()) {
                baos.write(GS_CUT_PARTIAL);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error building ESC/POS payload: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    @Override
    public void printMessage(TextMessage message) throws Exception {
        byte[] payload = buildEscPosPayload(message);
        try (PrinterStreamFactory.StreamHandle handle = PrinterStreamFactory.openStream(config)) {
            OutputStream out = handle.getOutputStream();
            out.write(payload);
            out.flush();
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
                        "Connection: " + config.getConnectionType().name() + " (" + config.getAddress() + ")"
        );
        printMessage(testMsg);
    }
}
