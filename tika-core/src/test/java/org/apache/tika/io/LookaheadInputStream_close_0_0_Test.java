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

public class LookaheadInputStream_close_0_0_Test {

    @Test
    public void testCloseWithoutStream() throws IOException {
        LookaheadInputStream inputStream = new LookaheadInputStream(null, 10);
        inputStream.close();
        // No exception should be thrown
    }

    @Test
    public void testCloseWithStream() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        LookaheadInputStream inputStream = new LookaheadInputStream(byteArrayInputStream, 10);
        inputStream.close();
        // No exception should be thrown
        byteArrayInputStream.reset();
        assertEquals(3, byteArrayInputStream.available());
    }
}
