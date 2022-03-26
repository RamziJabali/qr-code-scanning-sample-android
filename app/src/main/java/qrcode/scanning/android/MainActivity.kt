package qrcode.scanning.android

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import qrcode.scanning.android.util.PermissionUtil
import qrcode.scanning.android.viewmodel.HomeViewModel
import qrcode.scanning.android.views.HomeView
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_RQ = 1
        private const val IMAGE_CAPTURE_RQ = 2
    }

    private val viewModel = HomeViewModel()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeView(viewModel)
        }
        collectViewState()
        checkForPermissions(android.Manifest.permission.CAMERA, "Camera", CAMERA_RQ)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collectLatest { viewState: HomeViewState ->
                    if (viewState.isButtonClicked) {
                        checkForPermissions(android.Manifest.permission.CAMERA, "Camera", CAMERA_RQ)
                        dispatchTakePictureIntent()
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

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, IMAGE_CAPTURE_RQ)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }
}