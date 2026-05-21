package com.ABenhadar.smartlife

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ABenhadar.smartlife.models.UserData
import com.ABenhadar.smartlife.viewmodel.UserViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var ivUserAvatarProfile: ShapeableImageView

    private var latestTmpUri: Uri? = null

    private val pickImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            ivUserAvatarProfile.setImageURI(it)
            uploadImageToBackend(it)
        }
    }

    private val takePictureLauncher: ActivityResultLauncher<Uri> = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            latestTmpUri?.let { uri ->
                ivUserAvatarProfile.setImageURI(uri)
                uploadImageToBackend(uri)
            }
        } else {
            Toast.makeText(this, "Prise de photo annulée", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            showImagePickerDialog()
        } else {
            Toast.makeText(this, "Permissions requises non accordées", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvEmail = findViewById(R.id.tvEmail)
        tvBirthDate = findViewById(R.id.tvBirthDate)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
        ivUserAvatarProfile = findViewById(R.id.ivUserAvatarProfile)

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnEditProfile.isEnabled = !isLoading
            btnLogout.isEnabled = !isLoading
            ivUserAvatarProfile.isClickable = !isLoading
        }

        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                tvFirstName.text = user.firstName
                tvLastName.text = user.lastName
                tvEmail.text = user.email
                tvBirthDate.text = user.birthDate
                
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    // Utilisation de l'IP correcte (192.168.11.171)
                    val baseUrl = "http://192.168.11.171:8000" 
                    val fullImageUrl = if (user.profileImageUrl.startsWith("http")) {
                        user.profileImageUrl
                    } else {
                        baseUrl + user.profileImageUrl
                    }

                    Log.d("ProfileActivity", "Chargement image : $fullImageUrl")

                    Glide.with(this)
                        .load(fullImageUrl)
                        .placeholder(R.mipmap.ic_logo)
                        .error(R.mipmap.ic_logo)
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Désactive le cache disque pour forcer le rafraîchissement
                        .skipMemoryCache(true) // Désactive le cache mémoire
                        .circleCrop()
                        .into(ivUserAvatarProfile)
                } else {
                    Glide.with(this)
                        .load(R.mipmap.ic_logo)
                        .circleCrop()
                        .into(ivUserAvatarProfile)
                }
            }
        }

        auth.currentUser?.uid?.let { viewModel.getUser(it) }

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
        
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        ivUserAvatarProfile.setOnClickListener { 
            checkAndRequestPermissions()
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, "Erreur: $error", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser?.uid?.let { viewModel.getUser(it) }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Prendre une photo", "Choisir depuis la galerie")
        AlertDialog.Builder(this)
            .setTitle("Photo de profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun openCamera() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val photoFile: File? = createImageFile()
                photoFile?.also { file ->
                    latestTmpUri = FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.fileprovider",
                        file
                    )
                    withContext(Dispatchers.Main) {
                        latestTmpUri?.let { takePictureLauncher.launch(it) }
                    }
                }
            } catch (ex: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Erreur fichier image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadImageToBackend(fileUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, fileUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, fileUri)
            }
        } catch (e: Exception) {
            (ivUserAvatarProfile.drawable as? BitmapDrawable)?.bitmap
        }

        if (bitmap == null) {
            Toast.makeText(this, "Image invalide", Toast.LENGTH_SHORT).show()
            return
        }

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val imageBytes = baos.toByteArray()

        val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", "profile.jpg", requestFile)

        viewModel.uploadProfileImage(userId, body)
    }
}
