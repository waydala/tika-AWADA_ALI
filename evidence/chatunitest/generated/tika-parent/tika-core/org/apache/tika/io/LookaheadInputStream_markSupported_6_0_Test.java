package org.apache.tika.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

public class LookaheadInputStream_markSupported_6_0_Test {

    @Test
    public void testMarkSupported() throws IOException {
        // Create a ByteArrayInputStream with some data
        ByteArrayInputStream bais = new ByteArrayInputStream("Hello, World!".getBytes());
        // Create a LookaheadInputStream with the ByteArrayInputStream
        LookaheadInputStream lsi = new LookaheadInputStream(bais, 10);
        // Check that markSupported() returns true
        assertTrue(lsi.markSupported());
    }

    @Test
    public void testReadWithMark() throws IOException {
        // Create a ByteArrayInputStream with some data
        ByteArrayInputStream bais = new ByteArrayInputStream("Hello, World!".getBytes());
        // Create a LookaheadInputStream with the ByteArrayInputStream
        LookaheadInputStream lsi = new LookaheadInputStream(bais, 10);
        // Mark the current position
        lsi.mark(10);
        // Read some bytes
        byte[] buffer = new byte[5];
        int bytesRead = lsi.read(buffer, 0, 5);
        // Check that the read bytes are correct
        assertArrayEquals("Hello".getBytes(), buffer, 0, bytesRead);
        // Reset to the marked position
        lsi.reset();
        // Read more bytes
        bytesRead = lsi.read(buffer, 0, 5);
        // Check that the read bytes are correct
        assertArrayEquals("World!".getBytes(), buffer, 0, bytesRead);
    }
}
