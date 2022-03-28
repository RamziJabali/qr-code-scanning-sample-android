package qrcode.scanning.android

import android.app.AlertDialog
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import qrcode.scanning.android.util.PermissionUtil
import qrcode.scanning.android.viewmodel.HomeViewModel
import qrcode.scanning.android.views.HomeView
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_RQ = 1
        private const val READ_EXTERNAL_STORAGE_RQ = 2
        private const val WRITE_EXTERNAL_STORAGE_RQ = 3
    }

    private val viewModel = HomeViewModel()
    private lateinit var currentPhotoPath: String

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccessful ->
            if (isSuccessful) {
                 Log.i(this.toString(), "Intent Success")

            }else{
                Log.i(this.toString(), "Intent Failure")
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeView(viewModel)
        }
        collectViewState()
        checkForPermissions(android.Manifest.permission.CAMERA, "Camera", CAMERA_RQ)
        checkForPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            "Read External Storage",
            READ_EXTERNAL_STORAGE_RQ
        )
        checkForPermissions(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "Write External Storage",
            WRITE_EXTERNAL_STORAGE_RQ
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collectLatest { viewState: HomeViewState ->
                    if (viewState.isButtonClicked) {
                        takePhoto()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkForPermissions(permission: String, name: String, requestCode: Int) {
        when {
            PermissionUtil.isGranted(this, permission) -> Log.i(
                this.toString(),
                "Permission $name Granted"
            )
            shouldShowRequestPermissionRationale(permission) -> showDialog(
                permission,
                name,
                requestCode
            )
            else -> {
                requestPermissions(arrayOf(permission), requestCode)
            }
        }
    }

    private fun showDialog(permission: String, name: String, requestCode: Int) {
        val dialog = AlertDialog.Builder(this)
            .setMessage("Permission to access your $name is required to use this app")
            .setTitle("Permission Required")
            .setPositiveButton("OK") { dialog, which ->
                PermissionUtil.request(this, arrayOf(permission), requestCode)
            }
            .create()
        dialog.show()
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

    private fun takePhoto() {
        val photoFile: File? = createImageFile()
        photoFile?.also {
            // You must set up file provider to expose the url to Camera app
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                it
            )
            takePhotoLauncher.launch(photoURI)
        }
    }
}