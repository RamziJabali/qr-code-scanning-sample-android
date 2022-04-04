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
import androidx.core.content.FileProvider
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.ScanQRCode
import io.github.g00fy2.quickie.content.QRContent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import qrcode.scanning.android.viewmodel.HomeViewModel
import qrcode.scanning.android.views.HomeView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val viewModel = HomeViewModel()

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
    }

    private val takePhotoLauncher =
        registerForActivityResult(ScanQRCode(), ::handleResult)

    private fun handleResult(result: QRResult) {
        when (result) {
            is QRSuccess -> Log.i(this.toString(), result.content.rawValue)
            QRResult.QRUserCanceled -> Log.i(this.toString(), "User canceled")
            QRResult.QRMissingPermission -> Log.i(this.toString(), "Missing permission")
            is QRResult.QRError -> Log.i(
                this.toString(),
                "${result.exception.javaClass.simpleName}: ${result.exception.localizedMessage}"
            )
        }
        if (result is QRSuccess && result.content is QRContent.Url) {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse((result.content as QRContent.Url).url)
            startActivity(openURL)
        }
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
                        takePhotoLauncher.launch(null)
                    }
                }
            }
        }
    }
}