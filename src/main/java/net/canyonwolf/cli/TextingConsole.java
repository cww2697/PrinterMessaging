package net.canyonwolf.cli;

import java.util.Scanner;
import javax.print.PrintService;
import net.canyonwolf.config.AppConfig;
import net.canyonwolf.config.ConfigManager;
import net.canyonwolf.format.MessageFormatter;
import net.canyonwolf.model.ConnectionType;
import net.canyonwolf.model.PrinterType;
import net.canyonwolf.model.TextMessage;
import net.canyonwolf.printer.DesktopPrinterService;
import net.canyonwolf.printer.PrinterFactory;
import net.canyonwolf.printer.PrinterService;

public class TextingConsole {
    private final ConfigManager configManager;
    private AppConfig config;

    public TextingConsole(ConfigManager configManager) {
        this.configManager = configManager != null ? configManager : new ConfigManager();
        this.config = this.configManager.getConfig();
    }

    public void start() {
        printBanner();
        printStatus();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type your message and press [Enter] to send to printer.");
        System.out.println("Type /help for available commands or /exit to quit.\n");

        while (true) {
            System.out.print("[" + config.getSenderName() + "] > ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("/")) {
                if (handleCommand(line, scanner)) {
                    break;
                }
            } else {
                sendMessage(line);
            }
        }

        System.out.println("\nGoodbye!");
    }

    private void printBanner() {
        System.out.println("=================================================");
        System.out.println("            PRINTER TEXTING SYSTEM               ");
        System.out.println("                                                 ");
        System.out.println("=================================================");
    }

    public void printStatus() {
        System.out.println("---------------- Current Settings ---------------");
        System.out.println(config.getSummary());
        System.out.println("Config file:     " + configManager.getConfigFile().getAbsolutePath());
        System.out.println("-------------------------------------------------");
    }

    private boolean handleCommand(String commandLine, Scanner scanner) {
        String[] parts = commandLine.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/exit":
            case "/quit":
            case "/q":
                return true;

            case "/help":
            case "/?":
                printHelp();
                break;

            case "/status":
            case "/settings":
                printStatus();
                break;

            case "/config":
            case "/configure":
            case "/setup":
                runConfigWizard(scanner);
                break;

            case "/sender":
            case "/name":
                if (!arg.isEmpty()) {
                    config.setSenderName(arg);
                    configManager.saveConfig(config);
                    System.out.println("Sender name updated to: " + config.getSenderName());
                } else {
                    System.out.print("Enter new sender name: ");
                    String name = scanner.nextLine().trim();
                    if (!name.isEmpty()) {
                        config.setSenderName(name);
                        configManager.saveConfig(config);
                        System.out.println("Sender name updated to: " + config.getSenderName());
                    }
                }
                break;

            case "/test":
                sendTestPrint();
                break;

            case "/printers":
                listDesktopPrinters();
                break;

            default:
                System.out.println("Unknown command: " + cmd + ". Type /help for available commands.");
        }
        return false;
    }

    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  /config          - Launch interactive configuration wizard");
        System.out.println("  /sender <name>   - Change sender name");
        System.out.println("  /status          - Display current printer & connection settings");
        System.out.println("  /printers        - List all detected OS desktop printers");
        System.out.println("  /test            - Send a test print with current configuration");
        System.out.println("  /help            - Show this help screen");
        System.out.println("  /exit            - Exit the application\n");
    }

    public void runConfigWizard(Scanner scanner) {
        System.out.println("\n========== PRINTER CONFIGURATION WIZARD ==========");

        System.out.println("\nSelect Printer Type:");
        System.out.println("  1. Receipt Printer (ESC/POS Thermal)");
        System.out.println("  2. Standard Desktop Printer (Page/Letter/A4)");
        System.out.print("Choice [" + (config.getPrinterType() == PrinterType.RECEIPT ? "1" : "2") + "]: ");
        String pChoice = scanner.nextLine().trim();
        if ("1".equals(pChoice)) {
            config.setPrinterType(PrinterType.RECEIPT);
        } else if ("2".equals(pChoice)) {
            config.setPrinterType(PrinterType.DESKTOP);
        }

        System.out.println("\nSelect Connection Type:");
        System.out.println("  1. Network / IP (Ethernet / Wi-Fi)");
        System.out.println("  2. USB Port / Device");
        System.out.println("  3. Serial / COM Port");
        int currentConn = config.getConnectionType() == ConnectionType.IP ? 1 :
                         (config.getConnectionType() == ConnectionType.USB ? 2 : 3);
        System.out.print("Choice [" + currentConn + "]: ");
        String cChoice = scanner.nextLine().trim();
        if ("1".equals(cChoice)) {
            config.setConnectionType(ConnectionType.IP);
        } else if ("2".equals(cChoice)) {
            config.setConnectionType(ConnectionType.USB);
        } else if ("3".equals(cChoice)) {
            config.setConnectionType(ConnectionType.SERIAL);
        }

        if (config.getConnectionType() == ConnectionType.IP) {
            System.out.print("\nEnter IP Address / Hostname [" + config.getAddress() + "]: ");
            String addr = scanner.nextLine().trim();
            if (!addr.isEmpty()) {
                config.setAddress(addr);
            }

            System.out.print("Enter Port (default 9100) [" + config.getPort() + "]: ");
            String portStr = scanner.nextLine().trim();
            if (!portStr.isEmpty()) {
                try {
                    config.setPort(Integer.parseInt(portStr));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number, keeping " + config.getPort());
                }
            }
        } else if (config.getConnectionType() == ConnectionType.USB) {
            String defaultUsb = config.getAddress().isEmpty() ? "USB001 or /dev/usb/lp0" : config.getAddress();
            System.out.print("\nEnter USB Port / Device Path [" + defaultUsb + "]: ");
            String addr = scanner.nextLine().trim();
            if (!addr.isEmpty()) {
                config.setAddress(addr);
            }
        } else if (config.getConnectionType() == ConnectionType.SERIAL) {
            String defaultSerial = config.getAddress().isEmpty() ? "COM1 or /dev/ttyUSB0" : config.getAddress();
            System.out.print("\nEnter Serial Port Name [" + defaultSerial + "]: ");
            String addr = scanner.nextLine().trim();
            if (!addr.isEmpty()) {
                config.setAddress(addr);
            }
        }

        if (config.getPrinterType() == PrinterType.DESKTOP) {
            PrintService[] services = DesktopPrinterService.getAvailablePrinters();
            if (services.length > 0) {
                System.out.println("\nDetected System Printers:");
                for (int i = 0; i < services.length; i++) {
                    System.out.println("  " + (i + 1) + ". " + services[i].getName());
                }
                System.out.print("Select printer number or leave blank to keep [" + (config.getDesktopPrinterName().isEmpty() ? "Default OS Printer" : config.getDesktopPrinterName()) + "]: ");
                String pNum = scanner.nextLine().trim();
                if (!pNum.isEmpty()) {
                    try {
                        int idx = Integer.parseInt(pNum) - 1;
                        if (idx >= 0 && idx < services.length) {
                            config.setDesktopPrinterName(services[idx].getName());
                        }
                    } catch (NumberFormatException e) {
                        config.setDesktopPrinterName(pNum);
                    }
                }
            } else {
                System.out.print("\nEnter Desktop Printer Name [" + config.getDesktopPrinterName() + "]: ");
                String name = scanner.nextLine().trim();
                if (!name.isEmpty()) {
                    config.setDesktopPrinterName(name);
                }
            }
        }

        System.out.print("\nEnter Default Sender Name [" + config.getSenderName() + "]: ");
        String sName = scanner.nextLine().trim();
        if (!sName.isEmpty()) {
            config.setSenderName(sName);
        }

        if (config.getPrinterType() == PrinterType.RECEIPT) {
            System.out.print("Auto-cut paper? (y/n) [" + (config.isAutoCut() ? "y" : "n") + "]: ");
            String cutStr = scanner.nextLine().trim();
            if (!cutStr.isEmpty()) {
                config.setAutoCut(cutStr.equalsIgnoreCase("y") || cutStr.equalsIgnoreCase("yes"));
            }

            System.out.print("Receipt column width (characters) [" + config.getReceiptWidth() + "]: ");
            String widthStr = scanner.nextLine().trim();
            if (!widthStr.isEmpty()) {
                try {
                    config.setReceiptWidth(Integer.parseInt(widthStr));
                } catch (NumberFormatException ignored) {}
            }
        }

        boolean saved = configManager.saveConfig(config);
        if (saved) {
            System.out.println("\nSettings saved successfully to: " + configManager.getConfigFile().getAbsolutePath());
        } else {
            System.out.println("\nFailed to save settings to disk.");
        }

        printStatus();
    }

    public void listDesktopPrinters() {
        System.out.println("\nDetected Operating System Printers:");
        PrintService[] services = DesktopPrinterService.getAvailablePrinters();
        PrintService defaultPrinter = DesktopPrinterService.getDefaultPrinter();
        if (services.length == 0) {
            System.out.println("  (No system print services detected)");
        } else {
            for (int i = 0; i < services.length; i++) {
                String isDef = (defaultPrinter != null && services[i].getName().equals(defaultPrinter.getName())) ? " [DEFAULT]" : "";
                System.out.println("  " + (i + 1) + ". " + services[i].getName() + isDef);
            }
        }
        System.out.println();
    }

    public void sendMessage(String text) {
        TextMessage msg = new TextMessage(config.getSenderName(), text);
        System.out.println("\n--- Outgoing Message Preview ---");
        System.out.print(MessageFormatter.formatBoxedMessage(msg, 45));
        System.out.println("Sending to " + config.getPrinterType().getDisplayName() + " (" + config.getConnectionType().name() + ")...");

        try (PrinterService service = PrinterFactory.createPrinterService(config)) {
            service.printMessage(msg);
            System.out.println("Message successfully printed!\n");
        } catch (Exception e) {
            System.err.println("Print Error: " + e.getMessage());
            System.err.println("Tip: Check connection settings with /status or configure with /config.\n");
        }
    }

    public void sendTestPrint() {
        System.out.println("\nSending test print to configured printer...");
        try (PrinterService service = PrinterFactory.createPrinterService(config)) {
            service.testPrint();
            System.out.println("Test print sent successfully!\n");
        } catch (Exception e) {
            System.err.println("Test Print Error: " + e.getMessage());
            System.err.println("Tip: Check connection settings with /status or configure with /config.\n");
        }
    }
}
