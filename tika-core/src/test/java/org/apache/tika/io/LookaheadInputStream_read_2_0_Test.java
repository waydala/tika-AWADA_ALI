package org.apache.tika.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.InputStream;

public class LookaheadInputStream_read_2_0_Test {

    @Test
    public void testReadSingleByte() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        LookaheadInputStream lookaheadInputStream = new LookaheadInputStream(inputStream, 3);
        assertEquals(1, lookaheadInputStream.read());
        assertEquals(2, lookaheadInputStream.read());
        assertEquals(3, lookaheadInputStream.read());
        assertEquals(-1, lookaheadInputStream.read());
    }

    @Test
    public void testReadMultipleBytes() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 });
        LookaheadInputStream lookaheadInputStream = new LookaheadInputStream(inputStream, 3);
        byte[] buffer = new byte[3];
        int bytesRead = lookaheadInputStream.read(buffer, 0, 3);
        assertEquals(3, bytesRead);
        assertArrayEquals(new byte[] { 1, 2, 3 }, buffer);
        bytesRead = lookaheadInputStream.read(buffer, 0, 2);
        assertEquals(-1, bytesRead);
        assertArrayEquals(new byte[] { 1, 2, 3 }, buffer);
        bytesRead = lookaheadInputStream.read(buffer, 0, 1);
        assertEquals(-1, bytesRead);
        assertArrayEquals(new byte[] { 1, 2, 3 }, buffer);
        bytesRead = lookaheadInputStream.read(buffer, 0, 1);
        assertEquals(-1, bytesRead);
    }
}
