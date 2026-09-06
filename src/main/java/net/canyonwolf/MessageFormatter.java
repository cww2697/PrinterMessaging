package net.canyonwolf;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class MessageFormatter {

    /**
     * Converts any string to strictly standard ASCII (0x00 - 0x7F) characters.
     * Replaces smart quotes, dashes, bullets, accented characters, etc. with ASCII equivalents.
     */
    public static String toAscii(String input) {
        if (input == null) return "";

        String s = input
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201A", "'")
                .replace("\u201B", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u201E", "\"")
                .replace("\u00AB", "\"")
                .replace("\u00BB", "\"")
                .replace("\u2013", "-")
                .replace("\u2014", "-")
                .replace("\u2015", "-")
                .replace("\u2212", "-")
                .replace("\u2026", "...")
                .replace("\u2022", "*")
                .replace("\u00B7", "*")
                .replace("\u00A0", " ");

        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || (c >= 32 && c <= 126)) {
                sb.append(c);
            } else if (c > 127) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    public static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        if (maxWidth <= 0) maxWidth = 42;

        String asciiText = toAscii(text);
        String[] paragraphs = asciiText.split("\\r?\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isEmpty()) {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    continue;
                }

                while (word.length() > maxWidth) {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                    }
                    lines.add(word.substring(0, maxWidth));
                    word = word.substring(maxWidth);
                }

                if (word.isEmpty()) continue;

                if (currentLine.length() == 0) {
                    currentLine.append(word);
                } else if (currentLine.length() + 1 + word.length() <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    public static String formatPlainMessage(TextMessage message, int width) {
        if (width < 20) width = 60;
        StringBuilder sb = new StringBuilder();
        String separator = repeat("-", width);

        sb.append(separator).append("\n");
        sb.append("TEXT MESSAGE\n");
        sb.append("FROM: ").append(toAscii(message.getSender())).append("\n");
        sb.append("DATE: ").append(toAscii(message.getFormattedTimestamp())).append("\n");
        sb.append(separator).append("\n");

        List<String> bodyLines = wrapText(message.getText(), width);
        for (String line : bodyLines) {
            sb.append(line).append("\n");
        }

        sb.append(separator).append("\n");
        return sb.toString();
    }

    public static String formatBoxedMessage(TextMessage message, int width) {
        if (width < 20) width = 42;
        int innerWidth = width - 4; // 2 for borders, 2 for padding

        StringBuilder sb = new StringBuilder();
        String horizontalBorder = "+" + repeat("-", width - 2) + "+";

        sb.append(horizontalBorder).append("\n");
        sb.append("| ").append(padRight("TEXT MESSAGE", innerWidth)).append(" |\n");
        sb.append("| ").append(padRight("FROM: " + toAscii(message.getSender()), innerWidth)).append(" |\n");
        sb.append("| ").append(padRight("DATE: " + toAscii(message.getFormattedTimestamp()), innerWidth)).append(" |\n");
        sb.append("+").append(repeat("=", width - 2)).append("+\n");

        List<String> wrappedLines = wrapText(message.getText(), innerWidth);
        for (String line : wrappedLines) {
            sb.append("| ").append(padRight(line, innerWidth)).append(" |\n");
        }

        sb.append(horizontalBorder).append("\n");
        return sb.toString();
    }

    public static String formatReceiptMessage(TextMessage message, int width) {
        if (width < 20) width = 42;
        StringBuilder sb = new StringBuilder();
        String separator = repeat("=", width);
        String divider = repeat("-", width);

        sb.append(separator).append("\n");
        sb.append(centerText("*** NEW TEXT MESSAGE ***", width)).append("\n");
        sb.append(separator).append("\n");
        sb.append("FROM: ").append(toAscii(message.getSender())).append("\n");
        sb.append("TIME: ").append(toAscii(message.getFormattedTimestamp())).append("\n");
        sb.append(divider).append("\n");

        List<String> bodyLines = wrapText(message.getText(), width);
        for (String line : bodyLines) {
            sb.append(line).append("\n");
        }

        sb.append(separator).append("\n");
        return sb.toString();
    }

    public static String repeat(String s, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s.substring(0, n);
        return s + repeat(" ", n - s.length());
    }

    public static String centerText(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        int remainder = width - text.length() - padding;
        return repeat(" ", padding) + text + repeat(" ", remainder);
    }
}
