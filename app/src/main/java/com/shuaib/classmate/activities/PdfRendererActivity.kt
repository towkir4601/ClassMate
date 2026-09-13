package com.shuaib.classmate.activities

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityPdfRendererBinding
import com.shuaib.classmate.repositories.TelegramPdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfRendererBinding
    private lateinit var repository: TelegramPdfRepository

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfRendererBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TelegramPdfRepository(this)

        val fileId = intent.getStringExtra("FILE_ID")
        val fileName = intent.getStringExtra("FILE_NAME") ?: "document.pdf"
        val fallbackUrl = intent.getStringExtra("FALLBACK_URL")

        binding.toolbar.title = fileName
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (fileId.isNullOrBlank()) {
            showError("Invalid File ID")
            return
        }

        binding.rvPdfPages.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            binding.layoutLoading.isVisible = true
            binding.rvPdfPages.isVisible = false
            binding.tvError.isVisible = false

            val result = repository.downloadPdf(fileId, fileName)
            
            result.onSuccess { file ->
                binding.layoutLoading.isVisible = false
                binding.rvPdfPages.isVisible = true
                renderPdf(file)
            }.onFailure { error ->
                binding.layoutLoading.isVisible = false
                binding.tvError.isVisible = true
                binding.tvError.text = "Error loading PDF: \${error.localizedMessage}\n\nFalling back to browser..."
                
                // Fallback to browser
                if (!fallbackUrl.isNullOrBlank()) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fallbackUrl))
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        binding.tvError.text = "Error: Could not open PDF."
                    }
                }
            }
        }
    }

    private suspend fun renderPdf(file: File) {
        withContext(Dispatchers.IO) {
            try {
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                
                val pageCount = pdfRenderer!!.pageCount
                
                withContext(Dispatchers.Main) {
                    binding.rvPdfPages.adapter = PdfPageAdapter(pdfRenderer!!, pageCount)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Could not render PDF: \${e.message}")
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.layoutLoading.isVisible = false
        binding.rvPdfPages.isVisible = false
        binding.tvError.isVisible = true
        binding.tvError.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }

    private inner class PdfPageAdapter(
        private val renderer: PdfRenderer,
        private val pageCount: Int
    ) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivPage: ImageView = view.findViewById(R.id.ivPdfPage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            // Render the page in a background thread for smooth scrolling
            lifecycleScope.launch(Dispatchers.IO) {
                // Synchronize access to PdfRenderer because it is not thread-safe and only one page can be open at a time
                val bitmap = synchronized(renderer) {
                    try {
                        val page = renderer.openPage(position)
                        
                        // Render at higher resolution for better readability
                        val density = resources.displayMetrics.density
                        val width = (page.width * density * 2).toInt()
                        val height = (page.height * density * 2).toInt()
                        
                        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Fill white background
                        b.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        b
                    } catch (e: Exception) {
                        null
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        holder.ivPage.setImageBitmap(bitmap)
                    }
                }
            }
        }

        override fun getItemCount(): Int = pageCount
    }
}
