/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vaxcare.core.report.Reporting
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.makeLongToast
import com.vaxcare.vaxhub.viewmodel.PermissionViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PermissionsActivity : AppCompatActivity() {
    companion object {
        private const val MOBILE_BRIDGE_APP_PACKAGE = "com.vaxcare.mobilebridge"

        private const val COARSE_LOCATION_REQUEST = 1001

        private const val URL_FMT = "content://%s/datapoints"
        private const val DEVICE_INFO_ID = "_id"
        private const val DEVICE_INFO_SERIAL = "serial"
        private const val DEVICE_INFO_IMEI = "imei"
        private const val DEVICE_INFO_ICCID = "iccid"

        private const val MODEL_TO_IGNORE = "G50 Plus"

        /**
         * Permissions required to make the app work!
         */
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val modelPackageName by lazy {
        val modelSuffix = Build.MODEL.let { modelName ->
            if (modelName == MODEL_TO_IGNORE) {
                ""
            } else {
                ".${modelName.lowercase()}"
            }
        }
        "$MOBILE_BRIDGE_APP_PACKAGE$modelSuffix"
    }

    private val deviceInfoContentUri by lazy {
        Uri.parse(URL_FMT.format(modelPackageName))
    }

    private val permissionViewModel: PermissionViewModel by viewModels()

    @Inject
    lateinit var reporting: Reporting

    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val resultCode = it.resultCode
            val data = it.data
            Timber.d("Result: $resultCode, $data")
            navigate()
        }
        reporting.start()
    }

    override fun onResume() {
        super.onResume()

        checkHasPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            COARSE_LOCATION_REQUEST -> {
                if (allPermissionsGranted()) {
                    navigate()
                } else {
                    makeLongToast(R.string.permission_must_be_accepted)
                    finish()
                }
            }
        }
    }

    private fun navigate() {
        if (!Settings.canDrawOverlays(this@PermissionsActivity)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            launcher.launch(intent)
        } else if (!packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
            launcher.launch(intent)
        } else if (!Settings.System.canWrite(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            launcher.launch(intent)
        } else if (isRunningOnEmulator()) {
            saveDeviceInfoAndStartMain(DeviceInfo("EMULATOR", "EMULATOR", "EMULATOR"))
        } else if (!isPackageInstalled(modelPackageName)) {
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.bridge_required_title)
                .setMessage(R.string.bridge_required_message)
                .setPositiveButton(R.string.install) { dialog, _ ->
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$modelPackageName")
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$modelPackageName")
                            )
                        )
                    }

                    dialog.dismiss()
                    finish()
                }
                .setNegativeButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .create()
                .show()
        } else {
            val deviceInfo = getInfoFromContentProvider()
            if (deviceInfo != null) {
                saveDeviceInfoAndStartMain(deviceInfo)
            } else {
                openApp()
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        val listOfInstalledPackages: List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
                    .map { it.packageName ?: "" }
            } else {
                packageManager.getInstalledPackages(0).map { it.packageName ?: "" }
            }

        return listOfInstalledPackages.any { it == packageName }
    }

    /**
     * We need to check that our user has allowed us to use the bluetooth and location permissions.
     * This is needed for the Bluetooth door sensor. We can have this run first and keep the user
     * from using the application if they have denied any of the permissions.
     */
    private fun checkHasPermissions() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this@PermissionsActivity,
                REQUIRED_PERMISSIONS,
                COARSE_LOCATION_REQUEST
            )
        } else {
            navigate()
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun getInfoFromContentProvider(): DeviceInfo? {
        var cursor: Cursor? = null

        var result: DeviceInfo? = null

        try {
            cursor = contentResolver.query(deviceInfoContentUri, null, null, null, DEVICE_INFO_ID)

            if (cursor != null && cursor.moveToFirst()) {
                var serial: String
                var imei: String
                var iccid: String

                do {
                    serial =
                        cursor.getString(cursor.getColumnIndex(DEVICE_INFO_SERIAL).coerceAtLeast(0))
                    imei =
                        cursor.getString(cursor.getColumnIndex(DEVICE_INFO_IMEI).coerceAtLeast(0))
                    iccid =
                        cursor.getString(cursor.getColumnIndex(DEVICE_INFO_ICCID).coerceAtLeast(0))
                } while (cursor.moveToNext())

                result = DeviceInfo(serial, imei, iccid)
            } else {
                Timber.e("Bridge app failed to return data in cursor.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting the cursor")
        } finally {
            cursor?.close()
        }

        return result
    }

    private fun openApp(): Boolean =
        try {
            packageManager.getLaunchIntentForPackage(modelPackageName)?.let { intent ->
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                launcher.launch(intent)
            }
            true
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Error launching bridge app")
            false
        }

    private fun isRunningOnEmulator(): Boolean {
        return Build.PRODUCT.startsWith("sdk")
    }

    private fun saveDeviceInfoAndStartMain(deviceInfo: DeviceInfo) {
        permissionViewModel.saveSecret(deviceInfo.serialNumber, deviceInfo.imei, deviceInfo.iccid)

        reporting.configure(deviceInfo.serialNumber)

        // navigate to main activity
        startActivity(Intent(this, Main::class.java))
        finish()
    }

    inner class DeviceInfo(val serialNumber: String, val imei: String, val iccid: String)
}
