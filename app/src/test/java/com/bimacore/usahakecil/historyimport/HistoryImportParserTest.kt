package com.bimacore.usahakecil.historyimport

import com.bimacore.usahakecil.domain.BusinessType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryImportParserTest {
    private val parser = HistoryImportParser()

    @Test
    fun `valid sale becomes ready and date only uses noon Jakarta`() {
        val draft = parser.parse(validPayload(), BusinessType.RETAIL)

        val row = draft.rows.single()
        assertEquals(HistoryImportReviewStatus.READY, row.status)
        assertEquals("DATE_ONLY", row.timePrecision)
        assertEquals(
            "2026-08-01 12:00",
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }.format(requireNotNull(row.eventAt)),
        )
    }

    @Test
    fun `uncertain but structurally valid value needs owner review`() {
        val draft = parser.parse(
            validPayload().replaceFirst(
                "\"uncertainFields\": []",
                "\"uncertainFields\": [\"unitPrice\"]",
            ),
            BusinessType.RETAIL,
        )

        assertEquals(HistoryImportReviewStatus.NEEDS_REVIEW, draft.rows.single().status)
        assertTrue(draft.rows.single().canApprove)
    }

    @Test
    fun `stock adjustment is archived and does not become ready`() {
        val payload = validPayload()
            .replace("\"type\": \"SALE\"", "\"type\": \"STOCK_ADJUSTMENT\"")
            .replace("\"amount\": 20000", "\"amount\": null")
            .replace("\"paymentMethod\": \"CASH\"", "\"paymentMethod\": null")
            .replace("\"stockDelta\": null", "\"stockDelta\": 2")

        val row = parser.parse(payload, BusinessType.RETAIL).rows.single()

        assertEquals(HistoryImportReviewStatus.UNRESOLVED, row.status)
        assertTrue(row.issues.any { it.contains("tidak mengubah stok") })
    }

    @Test
    fun `existing fingerprint is marked duplicate`() {
        val first = parser.parse(validPayload(), BusinessType.RETAIL).rows.single()
        val repeated = parser.parse(
            validPayload(),
            BusinessType.RETAIL,
            existingFingerprints = setOf(first.fingerprint),
        )

        assertEquals(HistoryImportReviewStatus.DUPLICATE, repeated.rows.single().status)
    }

    @Test
    fun `unknown field is rejected instead of silently ignored`() {
        val payload = validPayload().replace(
            "\"schemaVersion\": \"catattoko.history-import.v1\"",
            "\"schemaVersion\": \"catattoko.history-import.v1\", \"surprise\": true",
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            parser.parse(payload, BusinessType.RETAIL)
        }

        assertTrue(error.message.orEmpty().contains("field tidak dikenal"))
    }

    @Test
    fun `non credit sale with partial amount paid is not ready`() {
        val draft = parser.parse(
            validPayload().replace("\"amountPaid\": 20000", "\"amountPaid\": 0"),
            BusinessType.RETAIL,
        )

        val row = draft.rows.single()
        assertEquals(HistoryImportReviewStatus.UNRESOLVED, row.status)
        assertTrue(row.issues.any { it.contains("harus sama dengan total") })
    }

    private fun validPayload(): String =
        """
        {
          "schemaVersion": "catattoko.history-import.v1",
          "source": {
            "title": "Buku Agustus",
            "pageCount": 1,
            "businessType": "RETAIL",
            "timezone": "Asia/Jakarta"
          },
          "records": [
            {
              "sourceRef": "hal-1-baris-1",
              "type": "SALE",
              "date": "2026-08-01",
              "time": null,
              "partyName": null,
              "category": "Penjualan",
              "paymentMethod": "CASH",
              "amount": 20000,
              "amountPaid": 20000,
              "items": [
                {
                  "productName": "Dimsum Mentai",
                  "variantName": null,
                  "quantity": 2,
                  "unitLabel": "porsi",
                  "unitPrice": 10000,
                  "subtotal": 20000,
                  "uncertainFields": []
                }
              ],
              "stockDelta": null,
              "note": "Catatan lama",
              "rawText": "2 dimsum mentai 20.000",
              "uncertainFields": [],
              "warnings": []
            }
          ],
          "summary": {
            "recordCount": 1,
            "readyCount": 1,
            "needsReviewCount": 0,
            "dateFrom": "2026-08-01",
            "dateTo": "2026-08-01",
            "warnings": []
          }
        }
        """.trimIndent()
}
