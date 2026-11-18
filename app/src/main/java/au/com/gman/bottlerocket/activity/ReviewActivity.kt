package au.com.gman.bottlerocket.activity

import PhotoAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.gman.bottlerocket.R
import au.com.gman.bottlerocket.domain.BatchPhoto
import au.com.gman.bottlerocket.interfaces.IBatchRepository
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReviewActivity : AppCompatActivity() {

    @Inject
    lateinit var batchRepository: IBatchRepository

    private lateinit var adapter: PhotoAdapter
    private var isSharedFlow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        isSharedFlow = intent.getBooleanExtra("isSharedFlow", false)

        val title = findViewById<TextView>(R.id.titleText)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        val grid = findViewById<RecyclerView>(R.id.grid)
        val addAnother = findViewById<View>(R.id.addAnotherButton)
        val continueBtn = findViewById<View>(R.id.continueButton)

        adapter = PhotoAdapter(
            onDelete = { confirmDelete(it) },
            onOpen = { showFullImage(it) }
        )
        grid.layoutManager = GridLayoutManager(this, 2)
        grid.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                batchRepository.photos.collect { photos ->
                    adapter.submitList(photos)
                    title.text = resources.getQuantityString(
                        R.plurals.review_title,
                        photos.size,
                        photos.size
                    )
                    emptyText.isVisible = photos.isEmpty()
                    continueBtn.isEnabled = photos.isNotEmpty()
                }
            }
        }

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        addAnother.setOnClickListener { addAnotherPhoto() }
        continueBtn.setOnClickListener {
            startActivity(
                Intent(this, ConfirmWorkflowActivity::class.java)
                    .putExtra("isSharedFlow", isSharedFlow)
            )
        }
    }

    private fun addAnotherPhoto() {
        if (isSharedFlow) {
            // no camera underneath in the share flow, so open one
            startActivity(Intent(this, CaptureActivity::class.java))
        }
        finish()
    }

    private fun showFullImage(photo: BatchPhoto) {
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Captured page"
            load(photo.uri)
        }

        val container = FrameLayout(this).apply {
            addView(
                image,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (resources.displayMetrics.heightPixels * 0.7).toInt()
                )
            )
        }

        AlertDialog.Builder(this)
            .setView(container)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmDelete(photo: BatchPhoto) {
        val number = batchRepository.photos.value.indexOf(photo) + 1
        AlertDialog.Builder(this)
            .setTitle("Remove photo $number?")
            .setMessage("It will be removed from this batch. The image stays in your gallery.")
            .setPositiveButton("Remove") { _, _ -> batchRepository.remove(photo.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}