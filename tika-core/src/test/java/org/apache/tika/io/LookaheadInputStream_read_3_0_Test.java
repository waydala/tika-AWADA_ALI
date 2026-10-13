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

public class LookaheadInputStream_read_3_0_Test {

    @Test
    public void testReadWithAvailableBytes() throws IOException {
        // Create a ByteArrayInputStream with some data
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
        LookaheadInputStream lookaheadInputStream = new LookaheadInputStream(inputStream, 5);
        // Read 3 bytes
        byte[] b = new byte[3];
        int bytesRead = lookaheadInputStream.read(b, 0, 3);
        assertEquals(3, bytesRead);
        assertArrayEquals(new byte[] { 1, 2, 3 }, b);
        // Check available bytes
        assertEquals(2, lookaheadInputStream.available());
    }

    @Test
    public void testReadWithBufferedBytes() throws IOException {
        // Create a ByteArrayInputStream with some data
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
        LookaheadInputStream lookaheadInputStream = new LookaheadInputStream(inputStream, 5);
        // Read 5 bytes
        byte[] b = new byte[5];
        int bytesRead = lookaheadInputStream.read(b, 0, 5);
        assertEquals(5, bytesRead);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, b);
        // Check available bytes
        assertEquals(0, inputStream.available());
    }
}
