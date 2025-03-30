package com.example.pdfintegeration

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var invoiceLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        invoiceLayout = findViewById(R.id.invoice_view) // Get the root layout of the invoice
        val btnGenerate = findViewById<Button>(R.id.btn_generate)

        btnGenerate.setOnClickListener {
            generateInvoicePDF(this, invoiceLayout)
        }

    }

    /**
     * Generate pdf invoice
     */
    private fun generateInvoicePDF(context: Context, view: View) {
        view.post {
            val pdfDocument = PdfDocument()
            val bitmap = getBitmapFromView(view)

            // Create a PDF page
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            // Load and draw the seal image (only in PDF)
            // Load the seal image and scale it
            val sealBitmap = BitmapFactory.decodeResource(resources, R.drawable.seal)

            // Resize the seal to fit well in the PDF
            val sealSize = bitmap.width / 4  // Adjust size to 1/4th of invoice width
            val scaledSeal = Bitmap.createScaledBitmap(sealBitmap, sealSize, sealSize, true)

            // Position the seal at the bottom-right corner
            val sealX = bitmap.width - sealSize - 40f  // 40px padding
            val sealY = bitmap.height - sealSize - 40f

            canvas.drawBitmap(scaledSeal, sealX, sealY, null)

            pdfDocument.finishPage(page)

            // Save to Downloads folder for easy access
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "invoice_ojaswa.pdf"
            )

            try {
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                pdfDocument.close()
                fos.close()
                println("✅ PDF saved at: ${file.absolutePath}")

                sendPdfViaWhatsApp(file)
                //openPDF(file) // Open after saving

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     *     Helper function to convert a view to Bitmap
     */
    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    /**
     * Open pdf
     */
    private fun openPDF(file: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        startActivity(Intent.createChooser(intent, "Open PDF"))
    }

    /**
     * Send pdf to Wtsp
     */
    private fun sendPdfViaWhatsApp(file: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Here is your invoice PDF.")
            `package` = "com.whatsapp" // Ensures it opens in WhatsApp only
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Send pdf to particular Wtsp number
     */
    private fun sendPdfViaWhatsApp(file: File, phoneNumber: String) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Here is your invoice PDF.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                `package` = "com.whatsapp"
            }

            // Create WhatsApp direct message URI
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")

            startActivity(whatsappIntent)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp is not installed or error occurred", Toast.LENGTH_SHORT).show()
        }
    }


}