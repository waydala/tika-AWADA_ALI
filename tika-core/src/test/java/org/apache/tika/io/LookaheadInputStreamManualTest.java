/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

class LookaheadInputStreamManualTest {
    @Test
    void closeRestoresTheCurrentDelegatePosition() throws IOException {
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{10, 20, 30, 40});
        assertEquals(10, source.read());
        try (LookaheadInputStream lookahead = new LookaheadInputStream(source, 2)) {
            assertEquals(20, lookahead.read());
            assertEquals(30, lookahead.read());
            assertEquals(-1, lookahead.read());
        }
        assertEquals(20, source.read());
        assertEquals(30, source.read());
        assertEquals(40, source.read());
    }

    @Test
    void reachingDelegateEofRestoresItsPositionImmediately() throws IOException {
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{21, 42});
        try (LookaheadInputStream lookahead = new LookaheadInputStream(source, 5)) {
            assertEquals(21, lookahead.read());
            assertEquals(42, lookahead.read());
            assertEquals(-1, lookahead.read());
            assertEquals(21, source.read());
            assertEquals(-1, lookahead.read());
        }
    }

    @Test
    void fragmentedReadsUseOnlyTheRemainingBufferCapacity() throws IOException {
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{11, 22, 33, 44}) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                Objects.checkFromIndexSize(offset, length, buffer.length);
                return super.read(buffer, offset, Math.min(1, length));
            }
        };
        byte[] output = new byte[]{9, 9, 9, 9, 9};
        try (LookaheadInputStream lookahead = new LookaheadInputStream(source, 3)) {
            assertEquals(1, lookahead.read(output, 1, 3));
            assertEquals(1, lookahead.read(output, 2, 2));
            assertEquals(1, lookahead.read(output, 3, 1));
            assertArrayEquals(new byte[]{9, 11, 22, 33, 9}, output);
            assertEquals(-1, lookahead.read());
        }
    }

    @Test
    void bulkReadsRespectOffsetCountAndLookaheadLimit() throws IOException {
        ByteArrayInputStream source = new ByteArrayInputStream(
                new byte[]{(byte) 0x80, 0x22, (byte) 0xfe, 0x11});
        byte[] output = new byte[]{9, 9, 9, 9, 9, 9};
        try (LookaheadInputStream lookahead = new LookaheadInputStream(source, 3)) {
            assertEquals(2, lookahead.read(output, 2, 2));
            assertArrayEquals(new byte[]{9, 9, (byte) 0x80, 0x22, 9, 9}, output);
            assertEquals(1, lookahead.available());
            assertEquals(1, lookahead.read(output, 4, 2));
            assertArrayEquals(new byte[]{9, 9, (byte) 0x80, 0x22, (byte) 0xfe, 9}, output);
            assertEquals(0, lookahead.available());
            assertEquals(-1, lookahead.read(output, 0, output.length));
        }
    }

    @Test
    void reachingLookaheadLimitDoesNotTriggerAnotherDelegateRead() throws IOException {
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{11, 22, 33}) {
            private boolean alreadyRead;

            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                if (alreadyRead) {
                    throw new IllegalStateException("Unexpected read beyond the lookahead window");
                }
                alreadyRead = true;
                return super.read(buffer, offset, length);
            }
        };
        try (LookaheadInputStream lookahead = new LookaheadInputStream(source, 2)) {
            assertEquals(11, lookahead.read());
            assertEquals(22, lookahead.read());
            assertEquals(-1, lookahead.read());
            assertEquals(0, lookahead.skip(1));
        }
    }
}
