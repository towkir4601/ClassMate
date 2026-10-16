package com.shuaib.classmate.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityOfflinePdfViewerBinding
import com.shuaib.classmate.databinding.DialogJumpToPageBinding
import com.shuaib.classmate.databinding.ItemPdfPageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class OfflinePdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfflinePdfViewerBinding
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var currentPdfFile: File? = null

    private var pageCount: Int = 0
    private var isNightMode: Boolean = false
    private var isFullscreen: Boolean = false

    private val hidePillHandler = Handler(Looper.getMainLooper())
    private var hidePillRunnable: Runnable? = null

    private var bitmapCache: LruCache<Int, Bitmap>? = null
    private val renderLock = Any()
    private var pagesAdapter: PdfPagesAdapter? = null

    private val nightModeFilter by lazy {
        val matrix = ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f,  0f, 255f,
                 0f, -1f,  0f,  0f, 255f,
                 0f,  0f, -1f,  0f, 255f,
                 0f,  0f,  0f,  1f,   0f
            )
        )
        ColorMatrixColorFilter(matrix)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflinePdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize 25% heap memory LruCache for crisp rendering without OOM
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = (maxMemoryKb / 4).coerceAtLeast(1024 * 16)
        bitmapCache = object : LruCache<Int, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: Int, bitmap: Bitmap): Int {
                return (bitmap.byteCount / 1024).coerceAtLeast(1)
            }
        }

        // Restore saved Night Mode preference
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isNightMode = prefs.getBoolean(KEY_NIGHT_MODE, false)
        applyNightModeVisuals(animate = false)

        setupToolbar()
        setupFloatingPill()

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "ClassMate PDF" }
        binding.tvTitle.text = title

        val path = intent.getStringExtra(EXTRA_FILE_PATH).orEmpty()
        if (path.isBlank()) {
            showError("Offline file missing")
            return
        }

        val file = File(path)
        currentPdfFile = file
        renderPdf(file)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnNightMode.setOnClickListener {
            isNightMode = !isNightMode
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NIGHT_MODE, isNightMode)
                .apply()

            applyNightModeVisuals(animate = true)
            pagesAdapter?.notifyDataSetChanged()
        }

        binding.btnShare.setOnClickListener {
            shareCurrentPdf()
        }

        binding.btnMore.setOnClickListener { v ->
            showOverflowMenu(v)
        }
    }

    private fun setupFloatingPill() {
        binding.cardPagePill.setOnClickListener {
            val layoutManager = binding.rvPages.layoutManager as? LinearLayoutManager ?: return@setOnClickListener
            val currentVisible = (layoutManager.findFirstVisibleItemPosition() + 1).coerceIn(1, pageCount.coerceAtLeast(1))
            showJumpToPageDialog(currentVisible)
        }

        binding.rvPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible != RecyclerView.NO_POSITION && pageCount > 0) {
                    val currentPage = (firstVisible + 1).coerceIn(1, pageCount)
                    updatePagePillText(currentPage, pageCount)
                    showPagePillTemporarily()
                }
            }
        })
    }

    private fun updatePagePillText(current: Int, total: Int) {
        binding.tvPagePill.text = String.format(Locale.US, "%d / %d", current, total)
    }

    private fun showPagePillTemporarily() {
        if (isFullscreen) return

        binding.cardPagePill.animate().cancel()
        binding.cardPagePill.isVisible = true
        binding.cardPagePill.alpha = 1f

        hidePillRunnable?.let { hidePillHandler.removeCallbacks(it) }
        hidePillRunnable = Runnable {
            binding.cardPagePill.animate()
                .alpha(0f)
                .setDuration(350L)
                .withEndAction { binding.cardPagePill.isVisible = false }
                .start()
        }
        hidePillHandler.postDelayed(hidePillRunnable!!, 2500L)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val duration = 220L

        if (isFullscreen) {
            binding.layoutTopBar.animate()
                .alpha(0f)
                .translationY(-binding.layoutTopBar.height.toFloat())
                .setDuration(duration)
                .withEndAction { binding.layoutTopBar.isVisible = false }
                .start()

            binding.cardPagePill.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction { binding.cardPagePill.isVisible = false }
                .start()
        } else {
            binding.layoutTopBar.isVisible = true
            binding.layoutTopBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .start()

            showPagePillTemporarily()
        }
    }

    private fun applyNightModeVisuals(animate: Boolean) {
        val rootBg = if (isNightMode) Color.parseColor("#121212") else Color.parseColor("#E8EAED")
        binding.rootView.setBackgroundColor(rootBg)

        if (isNightMode) {
            binding.btnNightMode.setImageResource(R.drawable.ic_light_mode)
            binding.btnNightMode.setColorFilter(ContextCompat.getColor(this, R.color.cm_warning))
        } else {
            binding.btnNightMode.setImageResource(R.drawable.ic_dark_mode)
            binding.btnNightMode.setColorFilter(ContextCompat.getColor(this, R.color.cm_text_primary))
        }
    }

    private fun renderPdf(file: File) {
        if (!file.exists() || file.length() == 0L) {
            showError("Offline copy not found or empty")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(descriptor!!)
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount

                val sizeBytes = file.length()
                val sizeMb = sizeBytes / (1024.0 * 1024.0)
                val subtitleText = if (sizeMb >= 1.0) {
                    String.format(Locale.US, "%d %s • %.1f MB", pageCount, if (pageCount == 1) "page" else "pages", sizeMb)
                } else {
                    String.format(Locale.US, "%d %s • %.0f KB", pageCount, if (pageCount == 1) "page" else "pages", sizeBytes / 1024.0)
                }

                withContext(Dispatchers.Main) {
                    binding.layoutLoading.isVisible = false
                    binding.tvSubtitle.text = subtitleText
                    updatePagePillText(1, pageCount)
                    showPagePillTemporarily()

                    val displayWidth = resources.displayMetrics.widthPixels
                    val adapter = PdfPagesAdapter(pdfRenderer, pageCount, displayWidth)
                    pagesAdapter = adapter
                    binding.rvPages.layoutManager = LinearLayoutManager(this@OfflinePdfViewerActivity)
                    binding.rvPages.adapter = adapter
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to render offline PDF", e)
                withContext(Dispatchers.Main) {
                    showError("Unable to open offline PDF: Corrupt or invalid document")
                }
            }
        }
    }

    private fun shareCurrentPdf() {
        val file = currentPdfFile ?: return
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, "Document is not available to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(this, authority, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, binding.tvTitle.text.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share PDF", e)
            Toast.makeText(this, "Unable to share PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOverflowMenu(anchor: android.view.View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, MENU_JUMP_PAGE, 0, "Go to page...")
        popup.menu.add(0, MENU_OPEN_EXTERNAL, 1, "Open in external app")
        popup.menu.add(0, MENU_PRINT, 2, "Print document")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_JUMP_PAGE -> {
                    val layoutManager = binding.rvPages.layoutManager as? LinearLayoutManager
                    val currentVisible = ((layoutManager?.findFirstVisibleItemPosition() ?: 0) + 1).coerceIn(1, pageCount.coerceAtLeast(1))
                    showJumpToPageDialog(currentVisible)
                    true
                }
                MENU_OPEN_EXTERNAL -> {
                    openInExternalApp()
                    true
                }
                MENU_PRINT -> {
                    printPdfDocument()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openInExternalApp() {
        val file = currentPdfFile ?: return
        try {
            val authority = "${packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(this, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open external app", e)
            Toast.makeText(this, "No external PDF app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printPdfDocument() {
        val file = currentPdfFile ?: return
        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
            if (printManager != null) {
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: android.print.PrintAttributes?,
                        newAttributes: android.print.PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(file.name)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(pageCount)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            file.inputStream().use { input ->
                                ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(binding.tvTitle.text.toString(), printAdapter, null)
            } else {
                openInExternalApp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Print failed", e)
            openInExternalApp()
        }
    }

    private fun showJumpToPageDialog(currentPage: Int) {
        if (pageCount <= 1) return

        val dialogBinding = DialogJumpToPageBinding.inflate(layoutInflater)
        dialogBinding.tvPageRange.text = String.format(Locale.US, "Page range: 1 - %d", pageCount)
        dialogBinding.etPageNumber.setText(currentPage.toString())
        dialogBinding.etPageNumber.setSelection(dialogBinding.etPageNumber.text?.length ?: 0)

        dialogBinding.sbPage.max = pageCount - 1
        dialogBinding.sbPage.progress = (currentPage - 1).coerceAtLeast(0)

        dialogBinding.sbPage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    dialogBinding.etPageNumber.setText((progress + 1).toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnGo.setOnClickListener {
            val targetPage = dialogBinding.etPageNumber.text?.toString()?.toIntOrNull()
            if (targetPage != null && targetPage in 1..pageCount) {
                dialog.dismiss()
                binding.rvPages.scrollToPosition(targetPage - 1)
                updatePagePillText(targetPage, pageCount)
                showPagePillTemporarily()
            } else {
                dialogBinding.tilPage.error = "Enter a valid page (1 - $pageCount)"
            }
        }

        dialog.show()
    }

    private fun showError(message: String) {
        binding.layoutLoading.isVisible = false
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        hidePillRunnable?.let { hidePillHandler.removeCallbacks(it) }
        pagesAdapter = null
        bitmapCache?.evictAll()
        try {
            renderer?.close()
            descriptor?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing renderer", e)
        }
        super.onDestroy()
    }

    private inner class PdfPagesAdapter(
        private val pdfRenderer: PdfRenderer,
        private val count: Int,
        private val displayWidth: Int
    ) : RecyclerView.Adapter<PdfPagesAdapter.PageViewHolder>() {

        inner class PageViewHolder(val itemBinding: ItemPdfPageBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val itemBinding = ItemPdfPageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return PageViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val photoView = holder.itemBinding.ivPdfPage
            val card = holder.itemBinding.cardPage

            // Card background styling based on reading mode
            card.setCardBackgroundColor(if (isNightMode) Color.parseColor("#1E1E1E") else Color.WHITE)

            // Configure PhotoView Zoom boundaries
            photoView.minimumScale = 1.0f
            photoView.mediumScale = 2.5f
            photoView.maximumScale = 4.5f

            // Single tap toggles fullscreen (Google Drive experience)
            photoView.setOnViewTapListener { _, _, _ ->
                toggleFullscreen()
            }

            // Apply Night Reading Mode color inversion filter
            photoView.colorFilter = if (isNightMode) nightModeFilter else null

            // Check in-memory bitmap cache first
            val cachedBitmap = bitmapCache?.get(position)
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                photoView.setImageBitmap(cachedBitmap)
                return
            }

            // Clear bitmap and mark tag for safe asynchronous loading
            photoView.setImageBitmap(null)
            photoView.tag = position

            val targetIndex = position
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = renderPageBitmap(targetIndex)
                withContext(Dispatchers.Main) {
                    if (photoView.tag == targetIndex && bitmap != null && !bitmap.isRecycled) {
                        photoView.setImageBitmap(bitmap)
                    }
                }
            }
        }

        override fun getItemCount(): Int = count

        private fun renderPageBitmap(index: Int): Bitmap? {
            val cached = bitmapCache?.get(index)
            if (cached != null && !cached.isRecycled) return cached

            return synchronized(renderLock) {
                val secondCheck = bitmapCache?.get(index)
                if (secondCheck != null && !secondCheck.isRecycled) return@synchronized secondCheck

                try {
                    pdfRenderer.openPage(index).use { page ->
                        // Render at crisp 1.5x - 2.0x DPI scale for razor sharp text
                        val baseScale = displayWidth.toFloat() / page.width.toFloat()
                        val densityScale = (baseScale * 1.5f).coerceIn(1.0f, 2.5f)

                        val width = (page.width * densityScale).toInt().coerceAtLeast(1)
                        val height = (page.height * densityScale).toInt().coerceAtLeast(1)

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        bitmapCache?.put(index, bitmap)
                        bitmap
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering page $index", e)
                    null
                }
            }
        }
    }

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_TITLE = "extra_title"
        private const val TAG = "OfflinePdfViewer"
        private const val PREFS_NAME = "classmate_pdf_viewer_prefs"
        private const val KEY_NIGHT_MODE = "key_night_mode"

        private const val MENU_JUMP_PAGE = 101
        private const val MENU_OPEN_EXTERNAL = 102
        private const val MENU_PRINT = 103
    }
}
