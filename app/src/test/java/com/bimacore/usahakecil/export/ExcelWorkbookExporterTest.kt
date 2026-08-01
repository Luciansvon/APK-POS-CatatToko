package com.bimacore.usahakecil.export

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelWorkbookExporterTest {
    @Test
    fun creates_openxml_workbook_with_escaped_values() {
        val bytes = ExcelWorkbookExporter.toByteArray(
            ExcelWorkbook(
                sheets = listOf(
                    ExcelSheet(
                        name = "Produk & Harga",
                        rows = listOf(
                            listOf("Nama", "Catatan"),
                            listOf("Kopi <A>", "Harga 10.000 & siap"),
                        ),
                    ),
                ),
            ),
        )

        val entries = readEntries(bytes)
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.getValue("xl/workbook.xml").contains("Produk &amp; Harga"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("Kopi &lt;A&gt;"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("Harga 10.000 &amp; siap"))
    }

    @Test
    fun sizes_columns_from_longest_text_with_readable_bounds() {
        val entries = readEntries(
            ExcelWorkbookExporter.toByteArray(
                ExcelWorkbook(
                    sheets = listOf(
                        ExcelSheet(
                            name = "Lebar Kolom",
                            rows = listOf(
                                listOf("Nama", "Catatan"),
                                listOf("Kopi", "Teks catatan yang lebih panjang"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val sheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("<col min=\"1\" max=\"1\" width=\"10\""))
        assertTrue(sheet.contains("<col min=\"2\" max=\"2\" width=\"31\""))
        assertTrue(sheet.contains("bestFit=\"1\""))
    }

    @Test
    fun sanitizes_and_deduplicates_sheet_names() {
        val entries = readEntries(
            ExcelWorkbookExporter.toByteArray(
                ExcelWorkbook(
                    sheets = listOf(
                        ExcelSheet("A/B", listOf(listOf("Kolom"))),
                        ExcelSheet("A-B", listOf(listOf("Kolom"))),
                    ),
                ),
            ),
        )

        val workbook = entries.getValue("xl/workbook.xml")
        assertTrue(workbook.contains("name=\"A-B\""))
        assertTrue(workbook.contains("name=\"A-B (2)\""))
        assertEquals(2, entries.count { it.key.startsWith("xl/worksheets/sheet") })
    }

    private fun readEntries(bytes: ByteArray): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return entries
    }
}
