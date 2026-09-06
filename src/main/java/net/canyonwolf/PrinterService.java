package net.canyonwolf;

public interface PrinterService extends AutoCloseable {
    void printMessage(TextMessage message) throws Exception;
    void printRaw(String text) throws Exception;
    void testPrint() throws Exception;
    @Override
    default void close() throws Exception {}
}
