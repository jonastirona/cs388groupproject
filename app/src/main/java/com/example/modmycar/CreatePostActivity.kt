package com.example.modmycar

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class CreatePostActivity : AppCompatActivity() {

    private lateinit var captionEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var mediaRecyclerView: RecyclerView
    private lateinit var emptyMediaPlaceholder: LinearLayout
    private lateinit var uploadProgressIndicator: LinearProgressIndicator
    private lateinit var uploadStatusText: TextView
    private lateinit var savePostButton: Button
    private lateinit var uploadMediaButton: Button

    private lateinit var mediaPreviewAdapter: MediaPreviewAdapter

    // Repositories and Services
    private val authRepository: AuthRepository = SupabaseAuthRepository()
    private val postRepository: PostRepository = SupabasePostRepository(SupabaseClient.client)
    private val postStorageService: PostStorageService = SupabasePostStorageService()

    companion object {
        const val MAX_MEDIA_ITEMS = 10
    }

    // Photo/Video picker launcher (supports multiple selection)
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_MEDIA_ITEMS)
    ) { uris: List<Uri> ->
        handleSelectedMedia(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        initViews()
        setupToolbar()
        setupMediaRecyclerView()
        setupClickListeners()
    }

    private fun initViews() {
        captionEditText = findViewById(R.id.createPostCaption)
        descriptionEditText = findViewById(R.id.createPostDescription)
        mediaRecyclerView = findViewById(R.id.mediaPreviewRecyclerView)
        emptyMediaPlaceholder = findViewById(R.id.emptyMediaPlaceholder)
        uploadProgressIndicator = findViewById(R.id.uploadProgressIndicator)
        uploadStatusText = findViewById(R.id.uploadStatusText)
        savePostButton = findViewById(R.id.savePostButton)
        uploadMediaButton = findViewById(R.id.uploadMediaButton)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.createPostToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupMediaRecyclerView() {
        mediaPreviewAdapter = MediaPreviewAdapter(this) { position ->
            // Handle remove click
            mediaPreviewAdapter.removeAt(position)
            updateMediaVisibility()
        }

        mediaRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@CreatePostActivity, 
                LinearLayoutManager.HORIZONTAL, 
                false
            )
            adapter = mediaPreviewAdapter
        }

        updateMediaVisibility()
    }

    private fun setupClickListeners() {
        // Upload media button
        uploadMediaButton.setOnClickListener {
            launchMediaPicker()
        }

        // Save post button
        savePostButton.setOnClickListener {
            if (validateInputs()) {
                savePost()
            }
        }
    }

    private fun launchMediaPicker() {
        val currentCount = mediaPreviewAdapter.getMediaItems().size
        
        // Check if already at max limit
        if (currentCount >= MAX_MEDIA_ITEMS) {
            Snackbar.make(
                savePostButton,
                "Maximum $MAX_MEDIA_ITEMS items allowed. Remove some to add more.",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        
        // Launch picker for images and videos
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    private fun handleSelectedMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val currentCount = mediaPreviewAdapter.getMediaItems().size
        val remainingSlots = MAX_MEDIA_ITEMS - currentCount

        // Check if adding these would exceed the limit
        if (uris.size > remainingSlots) {
            Snackbar.make(
                savePostButton,
                "Only $remainingSlots more item(s) can be added. Maximum is $MAX_MEDIA_ITEMS.",
                Snackbar.LENGTH_LONG
            ).show()
        }

        // Take only what fits within the limit
        val urisToAdd = uris.take(remainingSlots)

        val selectedMediaList = urisToAdd.mapNotNull { uri ->
            // Get MIME type for each URI
            val mimeType = contentResolver.getType(uri) ?: return@mapNotNull null
            
            // Take persistent permission to access the URI later
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some URIs may not support persistable permissions, that's okay
            }

            SelectedMedia(uri, mimeType)
        }

        mediaPreviewAdapter.addAllMedia(selectedMediaList)
        updateMediaVisibility()
    }

    private fun updateMediaVisibility() {
        if (mediaPreviewAdapter.isEmpty()) {
            mediaRecyclerView.visibility = View.GONE
            emptyMediaPlaceholder.visibility = View.VISIBLE
        } else {
            mediaRecyclerView.visibility = View.VISIBLE
            emptyMediaPlaceholder.visibility = View.GONE
        }
    }

    private fun validateInputs(): Boolean {
        val caption = captionEditText.text?.toString()?.trim() ?: ""
        val description = descriptionEditText.text?.toString()?.trim() ?: ""

        // At least caption OR description must be provided
        if (caption.isEmpty() && description.isEmpty()) {
            Snackbar.make(
                savePostButton,
                "Please enter a caption or description",
                Snackbar.LENGTH_SHORT
            ).show()
            return false
        }

        // At least one media item must be selected
        if (mediaPreviewAdapter.isEmpty()) {
            Snackbar.make(
                savePostButton,
                "Please add at least one photo or video",
                Snackbar.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun savePost() {
        lifecycleScope.launch {
            try {
                // Show loading state
                showLoading(true, "Preparing upload...")

                // Get current user
                val userResult = authRepository.getCurrentSession()
                val userId = when (userResult) {
                    is AuthResult.Success -> userResult.data?.id
                    is AuthResult.Error -> null
                }

                if (userId == null) {
                    showLoading(false)
                    Snackbar.make(savePostButton, "Please sign in to create a post", Snackbar.LENGTH_SHORT).show()
                    return@launch
                }

                // Generate unique post ID
                val postId = UUID.randomUUID().toString()

                // Get form data
                val caption = captionEditText.text?.toString()?.trim()?.ifEmpty { null }
                val description = descriptionEditText.text?.toString()?.trim()?.ifEmpty { null }
                val selectedMedia = mediaPreviewAdapter.getMediaItems()

                // Upload media files
                showLoading(true, "Uploading media (0/${selectedMedia.size})...")
                val mediaItems = mutableListOf<MediaItem>()

                for ((index, media) in selectedMedia.withIndex()) {
                    showLoading(true, "Uploading media (${index + 1}/${selectedMedia.size})...")
                    
                    val uploadResult = uploadMediaFile(userId, postId, media, index)
                    if (uploadResult != null) {
                        mediaItems.add(uploadResult)
                    } else {
                        // Upload failed, show error and stop
                        showLoading(false)
                        Snackbar.make(savePostButton, "Failed to upload media. Please try again.", Snackbar.LENGTH_LONG).show()
                        return@launch
                    }
                }

                // Create post object
                showLoading(true, "Creating post...")
                val post = Post(
                    id = postId,
                    userId = userId,
                    caption = caption,
                    description = description,
                    media = mediaItems,
                    createdAt = Instant.now().toString()
                )

                // Save to database
                withContext(Dispatchers.IO) {
                    postRepository.createPost(post)
                }

                // Success!
                showLoading(false)
                Snackbar.make(savePostButton, "Post created successfully!", Snackbar.LENGTH_SHORT).show()
                
                // Return to previous screen
                setResult(RESULT_OK)
                finish()

            } catch (e: Exception) {
                showLoading(false)
                Snackbar.make(
                    savePostButton, 
                    "Error creating post: ${e.message}", 
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun uploadMediaFile(
        userId: String,
        postId: String,
        media: SelectedMedia,
        index: Int
    ): MediaItem? {
        return withContext(Dispatchers.IO) {
            try {
                // Read file bytes from URI
                val inputStream = contentResolver.openInputStream(media.uri)
                    ?: return@withContext null
                val fileBytes = inputStream.readBytes()
                inputStream.close()

                // Determine file extension and type
                val extension = getFileExtension(media.mimeType)
                val mediaType = getMediaType(media.mimeType)
                val fileName = "${mediaType}_${index + 1}.$extension"

                // Upload to storage
                val uploadResult = postStorageService.uploadPostMedia(
                    userId = userId,
                    postId = postId,
                    fileName = fileName,
                    fileBytes = fileBytes
                )

                when (uploadResult) {
                    is AuthResult.Success -> {
                        MediaItem(
                            type = mediaType,
                            url = uploadResult.data,
                            sizeBytes = fileBytes.size
                        )
                    }
                    is AuthResult.Error -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getFileExtension(mimeType: String): String {
        return when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("png") -> "png"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("webm") -> "webm"
            mimeType.contains("mov") || mimeType.contains("quicktime") -> "mov"
            mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
            mimeType.contains("wav") -> "wav"
            else -> "bin"
        }
    }

    private fun getMediaType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "image"
        }
    }

    private fun showLoading(show: Boolean, statusMessage: String = "") {
        uploadProgressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        uploadStatusText.visibility = if (show && statusMessage.isNotEmpty()) View.VISIBLE else View.GONE
        uploadStatusText.text = statusMessage
        savePostButton.isEnabled = !show
        uploadMediaButton.isEnabled = !show
    }
}
