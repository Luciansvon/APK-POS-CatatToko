package com.bimacore.usahakecil.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.domain.Receipt
import com.bimacore.usahakecil.ui.displayName
import com.bimacore.usahakecil.ui.formatReceiptDate
import com.bimacore.usahakecil.ui.formatRupiah
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReceiptImageExporter {
    suspend fun createShareIntent(
        context: Context,
        receipt: Receipt,
        primaryColor: Int,
    ): Intent = withContext(Dispatchers.IO) {
        val width = 720
        val headerHeight = 210
        val itemHeight = 82
        val summaryHeight = if (receipt.paymentMethod == PaymentMethod.CASH) 390 else 320
        val height = headerHeight + receipt.items.size * itemHeight + summaryHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        paint.color = primaryColor
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        canvas.drawText(receipt.businessName, 42f, 62f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 23f
        canvas.drawText("STRUK PENJUALAN", 42f, 104f, paint)
        canvas.drawText(receipt.receiptNumber, 42f, 145f, paint)
        canvas.drawText(formatReceiptDate(receipt.createdAt), 42f, 180f, paint)

        var y = headerHeight + 46f
        paint.color = Color.rgb(25, 32, 30)
        receipt.items.forEach { item ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 25f
            canvas.drawText(item.productName.take(34), 42f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 21f
            val detail = buildString {
                if (!item.variantName.isNullOrBlank()) {
                    append(item.variantName)
                    append(" · ")
                }
                append("${item.quantity} × ${formatRupiah(item.unitPrice)}")
            }
            canvas.drawText(detail.take(44), 42f, y + 31f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatRupiah(item.subtotal), width - 42f, y + 31f, paint)
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.rgb(225, 230, 227)
            canvas.drawRect(42f, y + 51f, width - 42f, y + 53f, paint)
            paint.color = Color.rgb(25, 32, 30)
            y += itemHeight
        }

        y += 24f
        drawSummaryLine(canvas, paint, "Metode", receipt.paymentMethod.displayName(), y, width)
        y += 46f
        if (receipt.paymentMethod == PaymentMethod.CASH) {
            drawSummaryLine(canvas, paint, "Uang diterima", formatRupiah(receipt.amountReceived), y, width)
            y += 46f
            drawSummaryLine(canvas, paint, "Kembalian", formatRupiah(receipt.changeAmount), y, width)
            y += 52f
        }

        paint.color = primaryColor
        canvas.drawRoundRect(42f, y, width - 42f, y + 94f, 18f, 18f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText("TOTAL", 70f, y + 57f, paint)
        paint.textSize = 34f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatRupiah(receipt.total), width - 70f, y + 60f, paint)
        paint.textAlign = Paint.Align.LEFT

        paint.color = Color.rgb(95, 107, 103)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 21f
        canvas.drawText("Terima kasih sudah berbelanja.", 42f, y + 142f, paint)

        val receiptDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val output = File(receiptDir, "${receipt.receiptNumber}.png")
        FileOutputStream(output).use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                "Struk gagal dibuat"
            }
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output,
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Struk ${receipt.receiptNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun drawSummaryLine(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        y: Float,
        width: Int,
    ) {
        paint.color = Color.rgb(95, 107, 103)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 23f
        canvas.drawText(label, 42f, y, paint)
        paint.color = Color.rgb(25, 32, 30)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, width - 42f, y, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}
