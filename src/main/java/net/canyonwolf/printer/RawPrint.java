package net.canyonwolf.printer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import net.canyonwolf.format.MessageFormatter;

public class RawPrint implements AutoCloseable {

    private final Socket socket;
    private final OutputStream outputStream;

    public RawPrint(String host, int port) throws Exception {
        this(host, port, 5000);
    }

    public RawPrint(String host, int port, int timeoutMs) throws Exception {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), timeoutMs);
        this.outputStream = socket.getOutputStream();
    }

    public void print(String text) throws Exception {
        if (text != null) {
            String ascii = MessageFormatter.toAscii(text);
            this.outputStream.write(ascii.getBytes(StandardCharsets.US_ASCII));
            this.outputStream.flush();
        }
    }

    public void printBytes(byte[] bytes) throws Exception {
        if (bytes != null) {
            this.outputStream.write(bytes);
            this.outputStream.flush();
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
