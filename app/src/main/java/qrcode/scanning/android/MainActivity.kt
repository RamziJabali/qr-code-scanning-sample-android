package qrcode.scanning.android

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), CameraXConfig.Provider {

    private val viewModel = HomeViewModel()
    private val imageCapture = ImageCapture.Builder().build()
    private val barcodeScanningOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(barcodeScanningOptions)

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
            )
        )
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
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
                        setContent {
                            Camera(imageCapture = imageCapture,
                                takePicture = { takePicture() })
                        }
                    }
                }
            }
        }
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat(DateFormat.FULL.toString(), Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.i("mainactivity", "Success Saving Picture")
                    val image: InputImage =
                        InputImage.fromFilePath(this@MainActivity, outputFileResults.savedUri!!)
                    scanner.process(image)
                        .addOnSuccessListener {
                            Log.i("mainactivity", "Success Scanning")
                            if (it.isNotEmpty()) {
                                val openURL = Intent(Intent.ACTION_VIEW)
                                openURL.data = Uri.parse(it[0].displayValue!!)
                                startActivity(openURL)
                            }
                            deleteGalleryImage(outputFileResults.savedUri!!)
                            viewModel.resetViewState()
                            setContent {
                                HomeView(viewModel = viewModel)
                            }
                        }
                        .addOnFailureListener {
                            Log.i(this.toString(), "Failure Scanning")
                            setContent {
                                HomeView(viewModel = viewModel)
                            }
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.i("mainactivity", "Error Saving Picture")
                }

            }
        )
    }

    private fun deleteGalleryImage(uri: Uri) {
        this.contentResolver.delete(uri, null, null)
    }
}