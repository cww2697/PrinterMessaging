package net.canyonwolf.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import net.canyonwolf.format.MessageFormatter;

public class TextMessage {
    private final String sender;
    private final String text;
    private final Date timestamp;

    public TextMessage(String text) {
        this("User", text, new Date());
    }

    public TextMessage(String sender, String text) {
        this(sender, text, new Date());
    }

    public TextMessage(String sender, String text, Date timestamp) {
        String s = (sender != null && !sender.trim().isEmpty()) ? sender.trim() : "User";
        this.sender = MessageFormatter.toAscii(s);
        this.text = MessageFormatter.toAscii(text != null ? text : "");
        this.timestamp = timestamp != null ? timestamp : new Date();
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(timestamp);
    }

    public String getShortTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a");
        return sdf.format(timestamp);
    }
}
