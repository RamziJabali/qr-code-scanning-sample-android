package qrcode.scanning.android

import android.Manifest
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import qrcode.scanning.android.viewmodel.HomeViewModel
import qrcode.scanning.android.views.HomeView
import qrcode.scanning.android.views.Camera
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), CameraXConfig.Provider {


    private val viewModel = HomeViewModel()
    private val imageCapture = ImageCapture.Builder()
        .setFlashMode(FLASH_MODE_OFF)
        .setTargetAspectRatio(RATIO_16_9)
        .build()
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var currentPhotoPath: String
    private var photoURI: Uri = Uri.EMPTY

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeView(viewModel)
        }
        setupViewModel()
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
        outputDirectory = createImageFile()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccessful ->
            if (!isSuccessful) {
                Log.i(this.toString(), "Failure")
                return@registerForActivityResult
            }
            val source = ImageDecoder.createSource(contentResolver, photoURI)
            val imageBitmap = ImageDecoder.decodeBitmap(source)
            val image: InputImage = InputImage.fromFilePath(this, photoURI)
            val result = scanner.process(image)
                .addOnSuccessListener {
                    Log.i("mainactivity", "Success Scanning")
                    Log.i("mainactivity", it[0].displayValue!!)
                    val openURL = Intent(Intent.ACTION_VIEW)
                    openURL.data = Uri.parse(it[0].displayValue!!)
                    startActivity(openURL)
                }
                .addOnFailureListener {
                    Log.i(this.toString(), "Failure Scanning")
                }
            Log.i(this.toString(), result.toString())
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.i(this.toString(), "${it.key} = ${it.value}")
            }
        }

    private fun setupViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collectLatest { viewState: HomeViewState ->
                    if (viewState.isButtonClicked) {
                        setContent{
                            Camera()
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto() {
        val photoFile: File = createImageFile()
        // You must set up file provider to expose the url to Camera app
        val photoURI: Uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".provider",
            photoFile
        )
        this.photoURI = photoURI
        Log.i(this.toString(), photoURI.toString())
        takePhotoLauncher.launch(photoURI)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
}