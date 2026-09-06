package net.canyonwolf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import net.canyonwolf.config.AppConfig;
import net.canyonwolf.config.ConfigManager;
import net.canyonwolf.format.MessageFormatter;
import net.canyonwolf.model.ConnectionType;
import net.canyonwolf.model.PrinterType;
import net.canyonwolf.model.TextMessage;
import net.canyonwolf.printer.DesktopPrinterService;
import net.canyonwolf.printer.PrinterFactory;
import net.canyonwolf.printer.PrinterService;
import net.canyonwolf.printer.ReceiptPrinterService;

public class PrinterMessagingTest {

    public static void main(String[] args) {
        System.out.println("Running PrinterMessaging tests...");
        try {
            testAppConfigDefaults();
            testAppConfigProperties();
            testConfigManagerSaveAndLoad();
            testMessageFormatterWrapping();
            testMessageFormatterBoxed();
            testMessageFormatterReceipt();
            testMessageFormatterPlain();
            testAsciiConversion();
            testReceiptPayloadGeneration();
            testPrinterFactorySelection();
            System.out.println("ALL 10 TEST SUITES PASSED SUCCESSFULLY!");
        } catch (Throwable t) {
            System.err.println("TEST FAILURE: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static void testAppConfigDefaults() {
        AppConfig config = new AppConfig();
        assertEq(PrinterType.RECEIPT, config.getPrinterType(), "Default printer type should be RECEIPT");
        assertEq(ConnectionType.IP, config.getConnectionType(), "Default connection type should be IP");
        assertEq("192.168.2.26", config.getAddress(), "Default IP address");
        assertEq(9100, config.getPort(), "Default port should be 9100");
        assertEq("User", config.getSenderName(), "Default sender name");
        assertTrue(config.isAutoCut(), "Auto cut should default to true");
        assertEq(4, config.getFeedLines(), "Feed lines should default to 4");
        System.out.println("✓ testAppConfigDefaults passed");
    }

    public static void testAppConfigProperties() {
        AppConfig config = new AppConfig();
        config.setPrinterType(PrinterType.DESKTOP);
        config.setConnectionType(ConnectionType.USB);
        config.setAddress("/dev/usb/lp0");
        config.setSenderName("Alice");
        config.setAutoCut(false);
        config.setFeedLines(2);
        config.setDesktopPrinterName("HP LaserJet");

        Properties props = config.toProperties();
        AppConfig reloaded = AppConfig.fromProperties(props);

        assertEq(PrinterType.DESKTOP, reloaded.getPrinterType(), "PrinterType roundtrip");
        assertEq(ConnectionType.USB, reloaded.getConnectionType(), "ConnectionType roundtrip");
        assertEq("/dev/usb/lp0", reloaded.getAddress(), "Address roundtrip");
        assertEq("Alice", reloaded.getSenderName(), "Sender name roundtrip");
        assertTrue(!reloaded.isAutoCut(), "AutoCut roundtrip");
        assertEq(2, reloaded.getFeedLines(), "Feed lines roundtrip");
        assertEq("HP LaserJet", reloaded.getDesktopPrinterName(), "Desktop printer name roundtrip");
        System.out.println("✓ testAppConfigProperties passed");
    }

    public static void testConfigManagerSaveAndLoad() throws IOException {
        File tempFile = File.createTempFile("printer_test_config", ".properties");
        tempFile.deleteOnExit();

        ConfigManager manager = new ConfigManager(tempFile);
        AppConfig config = manager.getConfig();
        config.setPrinterType(PrinterType.RECEIPT);
        config.setConnectionType(ConnectionType.SERIAL);
        config.setAddress("COM3");
        config.setSenderName("Bob");

        boolean saved = manager.saveConfig(config);
        assertTrue(saved, "Save should succeed");
        assertTrue(tempFile.exists(), "Config file should exist on disk");

        // Load into new manager instance
        ConfigManager reloadManager = new ConfigManager(tempFile);
        AppConfig loaded = reloadManager.getConfig();

        assertEq(PrinterType.RECEIPT, loaded.getPrinterType(), "Loaded printer type");
        assertEq(ConnectionType.SERIAL, loaded.getConnectionType(), "Loaded connection type");
        assertEq("COM3", loaded.getAddress(), "Loaded COM address");
        assertEq("Bob", loaded.getSenderName(), "Loaded sender name");
        System.out.println("✓ testConfigManagerSaveAndLoad passed");
    }

    public static void testMessageFormatterWrapping() {
        String longText = "This is a simple test to verify that word wrapping correctly splits words without breaking mid-word when possible.";
        List<String> lines = MessageFormatter.wrapText(longText, 20);
        for (String line : lines) {
            assertTrue(line.length() <= 20, "Line length (" + line.length() + ") should be <= 20: [" + line + "]");
        }

        // Test with forced break of extra long word
        String superWord = "Supercalifragilisticexpialidocious";
        List<String> broken = MessageFormatter.wrapText(superWord, 10);
        assertTrue(broken.size() > 1, "Long word should be split into multiple lines");
        for (String line : broken) {
            assertTrue(line.length() <= 10, "Broken segment length should be <= 10");
        }
        System.out.println("✓ testMessageFormatterWrapping passed");
    }

    public static void testMessageFormatterBoxed() {
        TextMessage msg = new TextMessage("Charlie", "Hello from pseudo texting!");
        String boxed = MessageFormatter.formatBoxedMessage(msg, 40);
        assertTrue(boxed.contains("TEXT MESSAGE"), "Should contain title");
        assertTrue(boxed.contains("FROM: Charlie"), "Should contain sender");
        assertTrue(boxed.contains("Hello from pseudo texting!"), "Should contain message body");
        assertTrue(boxed.startsWith("+"), "Should start with border");
        System.out.println("✓ testMessageFormatterBoxed passed");
    }

    public static void testMessageFormatterReceipt() {
        TextMessage msg = new TextMessage("Dave", "Receipt style test message.");
        String receipt = MessageFormatter.formatReceiptMessage(msg, 40);
        assertTrue(receipt.contains("*** NEW TEXT MESSAGE ***"), "Should contain header");
        assertTrue(receipt.contains("FROM: Dave"), "Should contain sender");
        assertTrue(receipt.contains("Receipt style test message."), "Should contain body");
        System.out.println("✓ testMessageFormatterReceipt passed");
    }

    public static void testMessageFormatterPlain() {
        TextMessage msg = new TextMessage("Dave", "Plain desktop style test message.");
        String plain = MessageFormatter.formatPlainMessage(msg, 50);
        assertTrue(plain.contains("TEXT MESSAGE"), "Should contain header");
        assertTrue(plain.contains("FROM: Dave"), "Should contain sender");
        assertTrue(plain.contains("Plain desktop style test message."), "Should contain body");
        assertTrue(!plain.contains("+"), "Plain message should not contain box corners");
        assertTrue(!plain.contains("|"), "Plain message should not contain vertical box borders");
        System.out.println("✓ testMessageFormatterPlain passed");
    }

    public static void testAsciiConversion() {
        // Smart quotes, dashes, accents, bullets
        String unicodeInput = "“Hello” ‘world’ — let’s café & naïve • test…";
        String asciiOutput = MessageFormatter.toAscii(unicodeInput);

        for (int i = 0; i < asciiOutput.length(); i++) {
            char c = asciiOutput.charAt(i);
            assertTrue(c <= 127, "All characters must be ASCII (<= 127): found code " + (int) c + " ('" + c + "')");
        }

        assertTrue(asciiOutput.contains("\"Hello\""), "Smart double quotes converted: " + asciiOutput);
        assertTrue(asciiOutput.contains("'world'"), "Smart single quotes converted: " + asciiOutput);
        assertTrue(asciiOutput.contains("let's"), "Apostrophe converted: " + asciiOutput);
        assertTrue(asciiOutput.contains("cafe"), "Accented e converted: " + asciiOutput);
        assertTrue(asciiOutput.contains("naive"), "Diaeresis i converted: " + asciiOutput);
        assertTrue(asciiOutput.contains("*"), "Bullet converted: " + asciiOutput);
        assertTrue(asciiOutput.contains("..."), "Ellipsis converted: " + asciiOutput);

        // Test TextMessage stores ASCII
        TextMessage msg = new TextMessage("Renée", "Special 🌟 emoji and “smart quotes”");
        assertTrue(msg.getSender().equals("Renee"), "Sender converted to ASCII");
        for (int i = 0; i < msg.getText().length(); i++) {
            char c = msg.getText().charAt(i);
            assertTrue(c <= 127, "TextMessage body must be strictly ASCII");
        }

        System.out.println("✓ testAsciiConversion passed");
    }

    public static void testReceiptPayloadGeneration() {
        AppConfig config = new AppConfig();
        config.setReceiptWidth(32);
        config.setAutoCut(true);
        config.setFeedLines(3);
        config.setSenderName("Eve");

        ReceiptPrinterService service = new ReceiptPrinterService(config);
        TextMessage msg = new TextMessage("Eve", "Test payload generation");
        byte[] payload = service.buildEscPosPayload(msg);

        assertTrue(payload != null && payload.length > 0, "Payload should not be empty");

        // Verify ESC @ (0x1B, 0x40) init header
        assertEq((byte) 0x1B, payload[0], "First byte should be ESC");
        assertEq((byte) 0x40, payload[1], "Second byte should be @");

        // Verify cut command is present at the end
        int len = payload.length;
        assertEq((byte) 0x1D, payload[len - 4], "GS command");
        assertEq((byte) 0x56, payload[len - 3], "V command");
        assertEq((byte) 0x42, payload[len - 2], "B (partial cut)");
        assertEq((byte) 0x00, payload[len - 1], "0 param");
        System.out.println("✓ testReceiptPayloadGeneration passed");
    }

    public static void testPrinterFactorySelection() {
        AppConfig receiptConfig = new AppConfig();
        receiptConfig.setPrinterType(PrinterType.RECEIPT);
        PrinterService rService = PrinterFactory.createPrinterService(receiptConfig);
        assertTrue(rService instanceof ReceiptPrinterService, "Should create ReceiptPrinterService");

        AppConfig desktopConfig = new AppConfig();
        desktopConfig.setPrinterType(PrinterType.DESKTOP);
        PrinterService dService = PrinterFactory.createPrinterService(desktopConfig);
        assertTrue(dService instanceof DesktopPrinterService, "Should create DesktopPrinterService");
        System.out.println("✓ testPrinterFactorySelection passed");
    }

    private static void assertEq(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion failed: " + message + ". Expected: [" + expected + "], Actual: [" + actual + "]");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}
