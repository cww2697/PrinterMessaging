package net.canyonwolf;

import java.util.Scanner;
import net.canyonwolf.cli.TextingConsole;
import net.canyonwolf.config.AppConfig;
import net.canyonwolf.config.ConfigManager;
import net.canyonwolf.model.ConnectionType;
import net.canyonwolf.model.PrinterType;

public class Main {
    public static void main(String[] args) {
        ConfigManager configManager = new ConfigManager();
        AppConfig config = configManager.getConfig();

        if (args.length == 0) {
            // Interactive texting mode
            TextingConsole console = new TextingConsole(configManager);
            console.start();
            return;
        }

        // Parse command line arguments
        String messageText = null;
        String senderOverride = null;
        boolean showConfig = false;
        boolean runWizard = false;
        boolean sendTest = false;
        boolean showHelp = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--config":
                case "-c":
                    runWizard = true;
                    break;
                case "--status":
                case "-s":
                    showConfig = true;
                    break;
                case "--test":
                case "-t":
                    sendTest = true;
                    break;
                case "--help":
                case "-h":
                    showHelp = true;
                    break;
                case "--type":
                    if (i + 1 < args.length) {
                        config.setPrinterType(PrinterType.fromString(args[++i]));
                    }
                    break;
                case "--conn":
                case "--connection":
                    if (i + 1 < args.length) {
                        config.setConnectionType(ConnectionType.fromString(args[++i]));
                    }
                    break;
                case "--ip":
                case "--address":
                case "-a":
                    if (i + 1 < args.length) {
                        config.setAddress(args[++i]);
                    }
                    break;
                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        try {
                            config.setPort(Integer.parseInt(args[++i]));
                        } catch (NumberFormatException ignored) {}
                    }
                    break;
                case "--sender":
                case "-m":
                    if (i + 1 < args.length) {
                        senderOverride = args[++i];
                        config.setSenderName(senderOverride);
                    }
                    break;
                case "--desktop-printer":
                    if (i + 1 < args.length) {
                        config.setDesktopPrinterName(args[++i]);
                    }
                    break;
                default:
                    if (!arg.startsWith("-")) {
                        if (messageText == null) {
                            messageText = arg;
                        } else {
                            messageText += " " + arg;
                        }
                    }
                    break;
            }
        }

        if (showHelp) {
            printUsage();
            return;
        }

        // Save any CLI configuration changes
        configManager.saveConfig(config);

        if (runWizard) {
            TextingConsole console = new TextingConsole(configManager);
            console.runConfigWizard(new Scanner(System.in));
            return;
        }

        if (showConfig) {
            TextingConsole console = new TextingConsole(configManager);
            console.printStatus();
            return;
        }

        if (sendTest) {
            TextingConsole console = new TextingConsole(configManager);
            console.sendTestPrint();
            return;
        }

        if (messageText != null && !messageText.trim().isEmpty()) {
            TextingConsole console = new TextingConsole(configManager);
            console.sendMessage(messageText.trim());
        } else {
            TextingConsole console = new TextingConsole(configManager);
            console.start();
        }
    }

    private static void printUsage() {
        System.out.println("PrinterMessaging - Pseudo Texting System for Printers");
        System.out.println("Usage:");
        System.out.println("  java -jar PrinterMessaging.jar                    Start interactive texting console");
        System.out.println("  java -jar PrinterMessaging.jar <message>          Print a message immediately");
        System.out.println("  java -jar PrinterMessaging.jar --config           Run configuration wizard");
        System.out.println("  java -jar PrinterMessaging.jar --status           Show current settings");
        System.out.println("  java -jar PrinterMessaging.jar --test             Send a test print");
        System.out.println("\nOptions:");
        System.out.println("  --type <receipt|desktop>                          Set printer type");
        System.out.println("  --conn <ip|usb|serial>                            Set connection type");
        System.out.println("  --ip, --address <address>                         Set IP / USB / Serial address");
        System.out.println("  --port <port>                                     Set port (for IP, e.g. 9100)");
        System.out.println("  --sender <name>                                   Set sender name");
        System.out.println("  --desktop-printer <name>                          Set desktop printer name");
        System.out.println("  --help, -h                                        Show this help message");
    }
}
