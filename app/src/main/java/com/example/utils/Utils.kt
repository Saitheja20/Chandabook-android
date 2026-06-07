package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.SummaryTotals
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CurrencyFormatter {
    fun format(amount: Double): String {
        return try {
            val longVal = amount.toLong()
            val str = longVal.toString()
            if (str.length <= 3) return "₹$str"
            
            val lastThree = str.substring(str.length - 3)
            val rest = str.substring(0, str.length - 3)
            
            val sb = java.lang.StringBuilder()
            var len = rest.length
            while (len > 0) {
                if (len > 2) {
                    sb.insert(0, "," + rest.substring(len - 2, len))
                    len -= 2
                } else {
                    sb.insert(0, rest.substring(0, len))
                    break
                }
            }
            "₹$sb,$lastThree"
        } catch (e: Exception) {
            "₹${amount.toInt()}"
        }
    }
}

object DateUtils {
    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val standardFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatDate(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val date = isoParser.parse(isoString) ?: return isoString
            standardFormatter.format(date)
        } catch (e: Exception) {
            // Fallback for simple date strings
            try {
                val altParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val d = altParser.parse(isoString) ?: return isoString
                standardFormatter.format(d)
            } catch (ex: Exception) {
                isoString
            }
        }
    }

    fun getRelativeTime(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val date = isoParser.parse(isoString) ?: return ""
            val diff = System.currentTimeMillis() - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                days == 0L && hours == 0L && minutes < 5L -> "Just now"
                days == 0L && hours == 0L -> "$minutes mins ago"
                days == 0L -> "$hours hrs ago"
                days == 1L -> "Yesterday"
                days < 7L -> "$days days ago"
                else -> standardFormatter.format(date)
            }
        } catch (e: Exception) {
            formatDate(isoString)
        }
    }

    fun formatWithTime(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val date = isoParser.parse(isoString) ?: return ""
            "${standardFormatter.format(date)} at ${timeFormatter.format(date)}"
        } catch (e: Exception) {
            formatDate(isoString)
        }
    }
}

object ShareUtils {
    fun shareText(context: Context, text: String, title: String = "Share ledger") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun shareFile(context: Context, file: File, subject: String = "ChandaBook Attachment") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareViaWhatsApp(context: Context, file: File?, text: String?) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                `package` = "com.whatsapp"
            }
            if (file != null) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.example.fileprovider",
                    file
                )
                intent.type = "application/pdf"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.type = "text/plain"
            }
            if (text != null) {
                intent.putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback direct chooser if WhatsApp is not installed
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                if (file != null) {
                    val uri: Uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                if (text != null) {
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ChandaBook receipt..."))
        }
    }
}

object PdfGenerator {

    fun generateDonationReceiptPdf(context: Context, donation: Donation, orgName: String): File? {
        val pdfDocument = PdfDocument()
        // Width: 400 pt, Height: 550 pt (Custom clean receipt/bill visual size)
        val pageInfo = PdfDocument.PageInfo.Builder(400, 550, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Design colors
        val primaryPaint = Paint().apply {
            color = Color.parseColor("#FF6B35") // Saffron primary
            style = Paint.Style.FILL
        }
        val secondaryPaint = Paint().apply {
            color = Color.parseColor("#2E7D32") // Deep Green
            style = Paint.Style.FILL
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#2E2620") // Dark charcoal text
            textSize = 14f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#EFE8E2")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // 1. Draw header background card
        canvas.drawRect(10f, 10f, 390f, 90f, primaryPaint)
        
        // Header Text
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("CHANDABOOK RECEIPT", 200f, 45f, titlePaint)
        titlePaint.textSize = 13f
        canvas.drawText(orgName.uppercase(Locale.getDefault()), 200f, 70f, titlePaint)

        // Outer white border wrapper
        canvas.drawRect(10f, 10f, 390f, 540f, borderPaint)

        // Receipt Meta Details (Vertical aligned columns)
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        canvas.drawText("Receipt No:", 30f, 130f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(donation.receiptNumber, 120f, 130f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Date & Time:", 30f, 160f, textPaint)
        val dateVal = DateUtils.formatWithTime(donation.createdAt)
        canvas.drawText(dateVal, 120f, 160f, textPaint)

        // Draw light grey divider
        canvas.drawLine(25f, 180f, 375f, 180f, borderPaint)

        // Donor details
        canvas.drawText("Received From:", 30f, 210f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 15f
        canvas.drawText(donation.donorName, 130f, 210f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 12f
        canvas.drawText("Phone Number:", 30f, 245f, textPaint)
        canvas.drawText(donation.phoneNumber ?: "N/A", 130f, 245f, textPaint)

        canvas.drawText("Category Purpose:", 30f, 280f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(donation.category.uppercase(Locale.getDefault()), 130f, 280f, textPaint)

        canvas.drawText("Payment Mode:", 30f, 315f, textPaint)
        canvas.drawText(donation.paymentMethod.uppercase(Locale.getDefault()), 130f, 315f, textPaint)

        if (!donation.address.isNullOrEmpty()) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Address / Area:", 30f, 350f, textPaint)
            val addr = if (donation.address.length > 30) donation.address.take(28) + "..." else donation.address
            canvas.drawText(addr, 130f, 350f, textPaint)
        }

        // Draw light grey divider
        canvas.drawLine(25f, 375f, 375f, 375f, borderPaint)

        // AMOUNT DISPLAY CARD (Big & bold)
        val amtPaint = Paint().apply {
            color = Color.parseColor("#E8F5E9") // Light festive green background
            style = Paint.Style.FILL
        }
        canvas.drawRect(50f, 395f, 350f, 455f, amtPaint)
        canvas.drawRect(50f, 395f, 350f, 455f, borderPaint)

        secondaryPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(CurrencyFormatter.format(donation.amount), 200f, 435f, secondaryPaint)

        // Footer Thank You
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 13f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Thank You for your Contribution! 🙏", 200f, 490f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textPaint.textSize = 10f
        canvas.drawText("Recorded securely by: ${donation.receivedByName}", 200f, 515f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF file to cache
        val file = File(context.cacheDir, "Receipt_${donation.receiptNumber}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateLedgerReportPdf(
        context: Context,
        orgName: String,
        donations: List<Donation>,
        expenses: List<Expense>,
        stats: SummaryTotals?
    ): File? {
        val pdfDocument = PdfDocument()
        // Page Info Standard A4: Width: 595, Height: 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Design colors
        val headerPaint = Paint().apply {
            color = Color.parseColor("#E65100") // Deep Orange
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            strokeWidth = 1f
        }

        // Draw Header
        canvas.drawRect(20f, 20f, 575f, 100f, headerPaint)
        canvas.drawText("CHANDABOOK FINANCIAL STATEMENT", 40f, 60f, titlePaint)
        
        val itemPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Committee: ${orgName.uppercase(Locale.getDefault())}", 40f, 85f, itemPaint)

        // Summary Information Section
        var yPos = 130f
        canvas.drawText("LEDGER CONSOLIDATED SUMMARY", 30f, yPos, boldPaint)
        yPos += 5f
        canvas.drawLine(25f, yPos, 570f, yPos, linePaint)
        yPos += 20f

        val totalDonationsSum = donations.sumOf { it.amount }
        val totalExpensesSum = expenses.sumOf { it.amount }
        val netBalance = totalDonationsSum - totalExpensesSum

        canvas.drawText("Total Collection (Chanda Received):", 40f, yPos, textPaint)
        canvas.drawText(CurrencyFormatter.format(totalDonationsSum), 280f, yPos, boldPaint)
        yPos += 20f

        canvas.drawText("Total Expenditure:", 40f, yPos, textPaint)
        canvas.drawText(CurrencyFormatter.format(totalExpensesSum), 280f, yPos, boldPaint)
        yPos += 20f

        canvas.drawText("Net Remaining Surplus Balance:", 40f, yPos, textPaint)
        canvas.drawText(CurrencyFormatter.format(netBalance), 280f, yPos, boldPaint.apply { color = Color.parseColor("#2E7D32") })
        boldPaint.color = Color.BLACK // Restore
        yPos += 20f

        canvas.drawText("Total Registered Contributors Count:", 40f, yPos, textPaint)
        canvas.drawText("${donations.size} Members", 280f, yPos, textPaint)
        yPos += 30f

        // Draw Donations Grid
        canvas.drawText("RECENT DONATION TRANSACTIONS (Top 10)", 30f, yPos, boldPaint)
        yPos += 5f
        canvas.drawLine(25f, yPos, 570f, yPos, linePaint)
        yPos += 15f

        // Table Header
        canvas.drawText("Donor Name", 35f, yPos, boldPaint.apply { textSize = 9f })
        canvas.drawText("Category", 200f, yPos, boldPaint)
        canvas.drawText("Payment Mode", 330f, yPos, boldPaint)
        canvas.drawText("Date", 450f, yPos, boldPaint)
        canvas.drawText("Amount", 515f, yPos, boldPaint)
        yPos += 10f
        canvas.drawLine(25f, yPos, 570f, yPos, linePaint)
        yPos += 15f

        boldPaint.textSize = 12f // Restore Text Size

        // List Donations
        val recentDonations = donations.take(10)
        textPaint.textSize = 9f
        for (don in recentDonations) {
            val name = if (don.donorName.length > 20) don.donorName.take(18) + "..." else don.donorName
            canvas.drawText(name, 35f, yPos, textPaint)
            canvas.drawText(don.category.uppercase(Locale.getDefault()), 200f, yPos, textPaint)
            canvas.drawText(don.paymentMethod.uppercase(Locale.getDefault()), 330f, yPos, textPaint)
            canvas.drawText(DateUtils.formatDate(don.createdAt), 450f, yPos, textPaint)
            canvas.drawText(CurrencyFormatter.format(don.amount), 515f, yPos, textPaint)
            yPos += 18f
        }

        yPos += 20f

        // Draw Expenses Grid
        canvas.drawText("RECENT EXPENSE PAYOUTS (Top 10)", 30f, yPos, boldPaint)
        yPos += 5f
        canvas.drawLine(25f, yPos, 570f, yPos, linePaint)
        yPos += 15f

        // Table Header
        canvas.drawText("Expenditure Title", 35f, yPos, boldPaint.apply { textSize = 9f })
        canvas.drawText("Category", 200f, yPos, boldPaint)
        canvas.drawText("Payment Mode", 330f, yPos, boldPaint)
        canvas.drawText("Date", 450f, yPos, boldPaint)
        canvas.drawText("Amount", 515f, yPos, boldPaint)
        yPos += 10f
        canvas.drawLine(25f, yPos, 570f, yPos, linePaint)
        yPos += 15f

        boldPaint.textSize = 12f // Restore Text Size

        // List Expenses
        val recentExpenses = expenses.take(10)
        for (exp in recentExpenses) {
            val title = if (exp.title.length > 20) exp.title.take(18) + "..." else exp.title
            canvas.drawText(title, 35f, yPos, textPaint)
            canvas.drawText(exp.category.uppercase(Locale.getDefault()), 200f, yPos, textPaint)
            canvas.drawText(exp.paymentMethod.uppercase(Locale.getDefault()), 330f, yPos, textPaint)
            canvas.drawText(DateUtils.formatDate(exp.createdAt), 450f, yPos, textPaint)
            canvas.drawText(CurrencyFormatter.format(exp.amount), 515f, yPos, textPaint)
            yPos += 18f
        }

        // Footer Statement
        yPos = 810f
        canvas.drawLine(25f, yPos, 570f, yPos, linePaint)
        yPos += 15f
        textPaint.textSize = 8f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated electronically via ChandaBook App — Festival Committee Transparency Digital Ledger Solutions.", 297f, yPos, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF file to cache
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "ChandaBook_Report_${timestamp}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
