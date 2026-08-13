package com.bimacore.usahakecil.scanner

sealed interface BarcodeFrameDecision {
    data class Process(val barcode: String) : BarcodeFrameDecision
    data object MultipleBarcodes : BarcodeFrameDecision
    data object Ignore : BarcodeFrameDecision
}

class BarcodeScanGate {
    private enum class State { READY, PROCESSING, WAIT_FOR_CLEAR }

    private var state = State.READY

    @Synchronized
    fun onFrame(barcodes: List<String>): BarcodeFrameDecision {
        if (barcodes.isEmpty()) {
            if (state == State.WAIT_FOR_CLEAR) state = State.READY
            return BarcodeFrameDecision.Ignore
        }
        if (state != State.READY) return BarcodeFrameDecision.Ignore
        if (barcodes.size > 1) {
            state = State.WAIT_FOR_CLEAR
            return BarcodeFrameDecision.MultipleBarcodes
        }
        state = State.PROCESSING
        return BarcodeFrameDecision.Process(barcodes.single())
    }

    @Synchronized
    fun processed() {
        if (state == State.PROCESSING) state = State.WAIT_FOR_CLEAR
    }

    @Synchronized
    fun reset() {
        state = State.READY
    }
}
