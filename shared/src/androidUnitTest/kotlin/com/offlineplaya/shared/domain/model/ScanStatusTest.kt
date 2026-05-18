package com.offlineplaya.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ScanStatusTest {

    @Test
    fun `dbValue maps every enum to a distinct lowercase string`() {
        assertEquals("pending", ScanStatus.PENDING.dbValue)
        assertEquals("scanned", ScanStatus.SCANNED.dbValue)
        assertEquals("error", ScanStatus.ERROR.dbValue)
    }

    @Test
    fun `fromDbValue is the inverse of dbValue for every enum`() {
        ScanStatus.entries.forEach { status ->
            assertEquals(status, ScanStatus.fromDbValue(status.dbValue))
        }
    }

    @Test
    fun `fromDbValue defaults to PENDING for unknown strings`() {
        assertEquals(ScanStatus.PENDING, ScanStatus.fromDbValue(""))
        assertEquals(ScanStatus.PENDING, ScanStatus.fromDbValue("garbage"))
        assertEquals(ScanStatus.PENDING, ScanStatus.fromDbValue("SCANNED")) // case-sensitive
    }
}
