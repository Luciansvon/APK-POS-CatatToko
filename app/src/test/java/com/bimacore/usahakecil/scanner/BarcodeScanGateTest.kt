package com.bimacore.usahakecil.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeScanGateTest {
    @Test
    fun `stationary barcode is processed once and can repeat after a clear frame`() {
        val gate = BarcodeScanGate()

        assertEquals(BarcodeFrameDecision.Process("00123"), gate.onFrame(listOf("00123")))
        assertEquals(BarcodeFrameDecision.Ignore, gate.onFrame(listOf("00123")))
        gate.processed()
        assertEquals(BarcodeFrameDecision.Ignore, gate.onFrame(listOf("00123")))
        assertEquals(BarcodeFrameDecision.Ignore, gate.onFrame(emptyList()))
        assertEquals(BarcodeFrameDecision.Process("00123"), gate.onFrame(listOf("00123")))
    }

    @Test
    fun `processing stays locked until repository work is marked complete`() {
        val gate = BarcodeScanGate()

        gate.onFrame(listOf("A"))
        assertEquals(BarcodeFrameDecision.Ignore, gate.onFrame(emptyList()))
        assertEquals(BarcodeFrameDecision.Ignore, gate.onFrame(listOf("B")))
        gate.processed()
        gate.onFrame(emptyList())
        assertEquals(BarcodeFrameDecision.Process("B"), gate.onFrame(listOf("B")))
    }

    @Test
    fun `multiple barcodes are rejected until frame clears`() {
        val gate = BarcodeScanGate()

        assertEquals(
            BarcodeFrameDecision.MultipleBarcodes,
            gate.onFrame(listOf("A", "B")),
        )
        assertEquals(BarcodeFrameDecision.Ignore, gate.onFrame(listOf("A")))
        gate.onFrame(emptyList())
        assertEquals(BarcodeFrameDecision.Process("A"), gate.onFrame(listOf("A")))
    }
}
