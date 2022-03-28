package qrcode.scanning.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
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
    lateinit var currentPhotoPath: String

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data?.extras?.get("data") as Bitmap
            Log.i("MainActivity", intent.toString())
            setContent {
                Image(bitmap = intent.asImageBitmap(), contentDescription = "picture")
            }
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
        checkForPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE, "Read External Storage", READ_EXTERNAL_STORAGE_RQ)
        checkForPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, "Write External Storage", WRITE_EXTERNAL_STORAGE_RQ)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collectLatest { viewState: HomeViewState ->
                    if (viewState.isButtonClicked) {
                        checkForPermissions(android.Manifest.permission.CAMERA, "Camera", CAMERA_RQ)
                        openActivityForResult()
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
                Log.i(
                    this.toString(),
                    "I reached the permission asking stage"
                )
            }
        }
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

    private fun openActivityForResult() {
        startForResult.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }
}