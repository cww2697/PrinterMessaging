package net.canyonwolf.printer;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import net.canyonwolf.config.AppConfig;
import net.canyonwolf.model.ConnectionType;

public class PrinterStreamFactory {

    public static class StreamHandle implements AutoCloseable {
        private final OutputStream outputStream;
        private final AutoCloseable parentResource;

        public StreamHandle(OutputStream outputStream, AutoCloseable parentResource) {
            this.outputStream = outputStream;
            this.parentResource = parentResource;
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public void close() throws Exception {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } finally {
                if (parentResource != null && parentResource != outputStream) {
                    parentResource.close();
                }
            }
        }
    }

    public static StreamHandle openStream(AppConfig config) throws Exception {
        if (config == null) {
            throw new IllegalArgumentException("AppConfig cannot be null");
        }

        ConnectionType connType = config.getConnectionType();
        String address = config.getAddress() != null ? config.getAddress().trim() : "";

        if (connType == ConnectionType.IP) {
            if (address.isEmpty()) {
                throw new IllegalArgumentException("IP address is not configured");
            }
            int port = config.getPort() > 0 ? config.getPort() : 9100;
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 5000);
            return new StreamHandle(socket.getOutputStream(), socket);
        } else if (connType == ConnectionType.SERIAL || connType == ConnectionType.USB) {
            if (address.isEmpty()) {
                throw new IllegalArgumentException(connType.getDisplayName() + " address/device path is not configured");
            }

            String path = address;
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                if (path.toUpperCase().matches("^COM[0-9]+$")) {
                    path = "\\\\.\\" + path.toUpperCase();
                }
            }

            FileOutputStream fos = new FileOutputStream(path);
            return new StreamHandle(fos, fos);
        } else {
            throw new UnsupportedOperationException("Unsupported connection type: " + connType);
        }
    }
}
