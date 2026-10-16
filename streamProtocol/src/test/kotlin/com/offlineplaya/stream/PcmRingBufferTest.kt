package com.offlineplaya.stream

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmRingBufferTest {

    @Test
    fun writeThenReadRoundTrips() {
        val ring = PcmRingBuffer(16)
        val written = ring.write(byteArrayOf(1, 2, 3, 4))
        assertEquals(4, written)
        assertEquals(4, ring.available)

        val dst = ByteArray(4)
        val n = ring.read(dst, 0, 4, timeoutMillis = 0)
        assertEquals(4, n)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), dst)
        assertEquals(0, ring.available)
    }

    @Test
    fun wrapsAroundCapacity() {
        val ring = PcmRingBuffer(4)
        ring.write(byteArrayOf(1, 2, 3))
        val tmp = ByteArray(2)
        ring.read(tmp, 0, 2, 0) // consume 1,2 → head advances
        ring.write(byteArrayOf(4, 5)) // wraps past the end of the backing array
        val dst = ByteArray(3)
        val n = ring.read(dst, 0, 3, 0)
        assertEquals(3, n)
        assertContentEquals(byteArrayOf(3, 4, 5), dst)
    }

    @Test
    fun readReturnsZeroWhenEmptyAndNoWait() {
        val ring = PcmRingBuffer(8)
        assertEquals(0, ring.read(ByteArray(8), 0, 8, timeoutMillis = 0))
    }

    @Test
    fun partialReadWhenLessAvailableThanRequested() {
        val ring = PcmRingBuffer(8)
        ring.write(byteArrayOf(9, 9))
        val dst = ByteArray(8)
        assertEquals(2, ring.read(dst, 0, 8, 0))
    }

    @Test
    fun flushDropsBufferedBytes() {
        val ring = PcmRingBuffer(8)
        ring.write(byteArrayOf(1, 2, 3, 4))
        ring.flush()
        assertEquals(0, ring.available)
        assertEquals(0, ring.read(ByteArray(4), 0, 4, 0))
    }

    @Test
    fun closeWakesBlockedWriterAndReturnsShort() {
        val ring = PcmRingBuffer(4)
        ring.write(byteArrayOf(1, 2, 3, 4)) // full
        val producer = Thread {
            // blocks because the ring is full, until close() wakes it
            ring.write(byteArrayOf(5, 6))
        }
        producer.start()
        Thread.sleep(50)
        ring.close()
        producer.join(1000)
        assertTrue(ring.isClosed)
        assertTrue(!producer.isAlive)
    }

    @Test
    fun producerConsumerStreamsAllBytesInOrder() {
        val capacity = 64
        val total = 10_000
        val ring = PcmRingBuffer(capacity)
        val received = ByteArray(total)
        val consumer = Thread {
            var read = 0
            val chunk = ByteArray(7)
            while (read < total) {
                val n = ring.read(chunk, 0, chunk.size, 50)
                System.arraycopy(chunk, 0, received, read, n)
                read += n
            }
        }
        consumer.start()
        val src = ByteArray(total) { (it % 251).toByte() }
        var off = 0
        while (off < total) {
            val n = ring.write(src, off, minOf(13, total - off))
            off += n
        }
        consumer.join(5000)
        assertContentEquals(src, received)
    }
}
