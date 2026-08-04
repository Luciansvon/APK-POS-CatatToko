package com.bimacore.usahakecil.export

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExcelSheet(
    val name: String,
    val rows: List<List<String>>,
    val headerRows: Set<Int> = setOf(0),
    val titleRows: Set<Int> = emptySet(),
    val subtitleRows: Set<Int> = emptySet(),
)

data class ExcelWorkbook(
    val sheets: List<ExcelSheet>,
)

object ExcelWorkbookExporter {
    private const val MAIN_NAMESPACE = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val RELATIONSHIPS_NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACKAGE_RELATIONSHIPS_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val MIN_COLUMN_WIDTH = 10
    private const val MAX_COLUMN_WIDTH = 72
    private const val COLUMN_WIDTH_PADDING = 3
    private const val TITLE_ROW_HEIGHT = 24
    private const val SUBTITLE_ROW_HEIGHT = 18
    private const val HEADER_ROW_HEIGHT = 32
    private const val EMPTY_ROW_HEIGHT = 8
    private const val DATA_ROW_HEIGHT = 18

    fun toByteArray(workbook: ExcelWorkbook): ByteArray = ByteArrayOutputStream().use { output ->
        write(workbook, output)
        output.toByteArray()
    }

    fun write(workbook: ExcelWorkbook, output: OutputStream) {
        require(workbook.sheets.isNotEmpty()) { "Workbook harus memiliki minimal satu sheet" }
        val sheets = uniqueSheetNames(workbook.sheets)
        ZipOutputStream(output).use { zip ->
            add(zip, "[Content_Types].xml", contentTypes(sheets.size))
            add(zip, "_rels/.rels", packageRelationships())
            add(zip, "xl/workbook.xml", workbookXml(sheets))
            add(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size))
            add(zip, "xl/styles.xml", stylesXml())
            sheets.forEachIndexed { index, sheet ->
                add(zip, "xl/worksheets/sheet${index + 1}.xml", worksheetXml(sheet))
            }
        }
    }

    private fun add(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypes(sheetCount: Int): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        repeat(sheetCount) { index ->
            append("<Override PartName=\"/xl/worksheets/sheet${index + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun packageRelationships(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"$PACKAGE_RELATIONSHIPS_NAMESPACE\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private fun workbookXml(sheets: List<ExcelSheet>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<workbook xmlns=\"$MAIN_NAMESPACE\" xmlns:r=\"$RELATIONSHIPS_NAMESPACE\">")
        append("<sheets>")
        sheets.forEachIndexed { index, sheet ->
            append("<sheet name=\"${xml(sheet.name)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRelationships(sheetCount: Int): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<Relationships xmlns=\"$PACKAGE_RELATIONSHIPS_NAMESPACE\">")
        repeat(sheetCount) { index ->
            append("<Relationship Id=\"rId${index + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${index + 1}.xml\"/>")
        }
        append("<Relationship Id=\"rId${sheetCount + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun stylesXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<styleSheet xmlns=\"$MAIN_NAMESPACE\">" +
            "<fonts count=\"4\"><font><sz val=\"11\"/><name val=\"Aptos\"/></font><font><b/><color rgb=\"FF0B6B61\"/><sz val=\"11\"/><name val=\"Aptos\"/></font><font><b/><color rgb=\"FF0B6B61\"/><sz val=\"16\"/><name val=\"Aptos Display\"/></font><font><color rgb=\"FF667085\"/><sz val=\"10\"/><name val=\"Aptos\"/></font></fonts>" +
            "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0B6B61\"/><bgColor indexed=\"64\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE5F3F0\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"4\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/><xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" applyFont=\"1\"/></cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

    private fun worksheetXml(sheet: ExcelSheet): String = buildString {
        val rows = if (sheet.rows.isEmpty()) listOf(listOf("")) else sheet.rows
        val maxColumns = rows.maxOf { it.size }.coerceAtLeast(1)
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"$MAIN_NAMESPACE\">")
        append("<dimension ref=\"A1:${columnName(maxColumns)}${rows.size}\"/>")
        append("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>")
        append("<sheetFormatPr defaultRowHeight=\"15\"/>")
        val widths = columnWidths(rows, maxColumns, sheet.titleRows)
        append("<cols>")
        widths.forEachIndexed { columnIndex, width ->
            val excelColumn = columnIndex + 1
            append("<col min=\"$excelColumn\" max=\"$excelColumn\" width=\"$width\" bestFit=\"1\" customWidth=\"1\"/>")
        }
        append("</cols>")
        append("<sheetData>")
        rows.forEachIndexed { rowIndex, row ->
            val rowHeight = when {
                rowIndex in sheet.titleRows -> TITLE_ROW_HEIGHT
                rowIndex in sheet.subtitleRows -> SUBTITLE_ROW_HEIGHT
                rowIndex in sheet.headerRows -> HEADER_ROW_HEIGHT
                row.isEmpty() -> EMPTY_ROW_HEIGHT
                else -> DATA_ROW_HEIGHT
            }
            append("<row r=\"${rowIndex + 1}\" ht=\"$rowHeight\" customHeight=\"1\">")
            row.forEachIndexed { columnIndex, value ->
                val cellReference = "${columnName(columnIndex + 1)}${rowIndex + 1}"
                val style = when {
                    rowIndex in sheet.titleRows -> " s=\"2\""
                    rowIndex in sheet.subtitleRows -> " s=\"3\""
                    rowIndex in sheet.headerRows -> " s=\"1\""
                    else -> ""
                }
                val isDataRow = rowIndex !in sheet.titleRows && rowIndex !in sheet.subtitleRows && rowIndex !in sheet.headerRows
                val longVal = if (isDataRow && !value.startsWith("0") || value == "0") value.toLongOrNull() else null
                val doubleVal = if (isDataRow && longVal == null && (!value.startsWith("0") || value == "0")) value.toDoubleOrNull() else null
                when {
                    longVal != null -> append("<c r=\"$cellReference\"$style><v>$longVal</v></c>")
                    doubleVal != null -> append("<c r=\"$cellReference\"$style><v>$doubleVal</v></c>")
                    else -> append("<c r=\"$cellReference\"$style t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value)}</t></is></c>")
                }
            }
            append("</row>")
        }
        append("</sheetData>")
        val mergeRows = sheet.titleRows.filter { it in rows.indices }
        if (mergeRows.isNotEmpty() && maxColumns > 1) {
            append("<mergeCells count=\"${mergeRows.size}\">")
            mergeRows.forEach { rowIndex ->
                append("<mergeCell ref=\"A${rowIndex + 1}:${columnName(maxColumns)}${rowIndex + 1}\"/>")
            }
            append("</mergeCells>")
        }
        append("</worksheet>")
    }

    private fun columnWidths(
        rows: List<List<String>>,
        maxColumns: Int,
        titleRows: Set<Int>,
    ): List<Int> = (0 until maxColumns).map { columnIndex ->
        val longest = rows.asSequence()
            .filterIndexed { rowIndex, _ -> rowIndex !in titleRows }
            .mapNotNull { row -> row.getOrNull(columnIndex) }
            .flatMap { value -> value.lineSequence() }
            .maxOfOrNull(String::length)
            ?: 0
        (longest + COLUMN_WIDTH_PADDING).coerceIn(MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH)
    }

    private fun uniqueSheetNames(sheets: List<ExcelSheet>): List<ExcelSheet> {
        val used = mutableSetOf<String>()
        return sheets.map { sheet ->
            val base = sheet.name
                .replace(Regex("[\\[\\]:*?/\\\\]"), "-")
                .trim()
                .ifBlank { "Sheet" }
                .take(31)
            var candidate = base
            var suffix = 2
            while (!used.add(candidate)) {
                val suffixText = " ($suffix)"
                candidate = base.take(31 - suffixText.length) + suffixText
                suffix++
            }
            sheet.copy(name = candidate)
        }
    }

    private fun columnName(index: Int): String {
        var value = index
        val result = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            result.append(('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return result.reverse().toString()
    }

    private fun xml(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '\"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(character)
            }
        }
    }
}
