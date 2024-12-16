package com.example.plantgard.ui.analysis

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.plantgard.R
import com.example.plantgard.api.ApiConfig
import com.example.plantgard.databinding.ActivityAnalysisBinding
import com.example.plantgard.getImageUri
import com.example.plantgard.response.PredictionResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Suppress("DEPRECATION")
class AnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisBinding
    private var currentImageUri: Uri? = null
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = binding.progressBar // Pastikan Anda punya progress bar di layout

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil data dari Intent
        val plantType = intent.getStringExtra("PLANT_TYPE") ?: "Tanaman tidak diketahui"
        val plantTextView: TextView = findViewById(R.id.TitleTextView)
        plantTextView.text = "Analisis $plantType"

        setupListener()
        binding.upload.setOnClickListener {
            if (currentImageUri != null) {
                // Ambil token dari SharedPreferences
                val sharedPref = getSharedPreferences("user_data", MODE_PRIVATE)
                val token = sharedPref.getString("auth_token", null)

                if (token != null) {
                    uploadImageToAPI(plantType, token)
                } else {
                    showToast("Token tidak ditemukan. Silakan login terlebih dahulu.")
                }
            } else {
                showToast("Harap pilih atau ambil gambar terlebih dahulu")
            }
        }
    }

    private fun setupListener() {
        // Tombol untuk membuka kamera
        binding.cameraButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                currentImageUri = getImageUri(this)
                cameraLaunch.launch(currentImageUri!!)
            }
        }

        binding.galleryButton.setOnClickListener {
            launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private val cameraLaunch =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                showImage()
            } else {
                currentImageUri = null
                showToast("Gagal mengambil gambar")
            }
        }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            showToast("Tidak ada gambar yang dipilih")
        }
    }

    private fun showImage() {
        val uri = currentImageUri
        if (uri != null) {
            Log.d("Image URI", "showImage: $uri")
            binding.previewImageView.setImageURI(uri) // Menampilkan gambar
        } else {
            Log.d("Image URI", "Tidak ditemukan URI gambar")
        }
    }

    private fun convertToJpeg(uri: Uri, context: Context): File? {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            val file = File(context.cacheDir, "converted_image.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()

            return file
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun uploadImageToAPI(plantType: String, token: String) {
        val filePath = getFilePathFromUri(currentImageUri!!)
        if (filePath != null) {
            val file = convertToJpeg(currentImageUri!!, this)
            if (file != null) {
                val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val headers = mapOf("Authorization" to "Bearer $token")

                progressBar.visibility = ProgressBar.VISIBLE

                val call = ApiConfig.apiService.uploadImage(plantType, headers, body)
                call.enqueue(object : Callback<PredictionResponse> {
                    override fun onResponse(
                        call: Call<PredictionResponse>,
                        response: Response<PredictionResponse>
                    ) {
                        progressBar.visibility = ProgressBar.GONE

                        if (response.isSuccessful) {
                            val predictionResponse = response.body()

                            if (predictionResponse != null) {
                                val disease = predictionResponse.data.disease

                                // Simpan data yang diterima
                                val diseaseType = disease.type
                                val diseaseDescription = disease.description
                                val diseaseTreatment = disease.treatment
                                val diseasePrevention = disease.prevention

                                // Kirim data ke ResultActivity
                                val intent = Intent(this@AnalysisActivity, ResultActivity::class.java)
                                intent.putExtra("PLANT_TYPE", plantType)
                                intent.putExtra("DISEASE_TYPE", diseaseType)
                                intent.putExtra("DISEASE_DESCRIPTION", diseaseDescription)
                                intent.putExtra("DISEASE_TREATMENT", diseaseTreatment)
                                intent.putExtra("DISEASE_PREVENTION", diseasePrevention)
                                intent.putExtra("IMAGE_URI", currentImageUri.toString()) // URI gambar

                                startActivity(intent)
                            } else {
                                showToast("Data tidak valid dari API")
                            }
                        } else {
                            showToast("Gagal mengambil data: ${response.message()}")
                        }
                    }

                    override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                        progressBar.visibility = ProgressBar.GONE
                        showToast("Kesalahan jaringan: ${t.message}")
                    }
                })
            } else {
                showToast("Konversi file gagal")
            }
        } else {
            showToast("File path is null")
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndex("_data")
            it.moveToFirst()
            return it.getString(columnIndex)
        }
        return null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
            false
        } else {
            true
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
}
