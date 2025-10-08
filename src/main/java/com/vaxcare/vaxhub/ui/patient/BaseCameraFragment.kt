/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.viewbinding.ViewBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.makeLongToast
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.view.VaxToolbar
import com.vaxcare.vaxhub.service.ScannerManager
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class BaseCameraFragment<VB : ViewBinding> : BaseCaptureFlowFragment<VB>() {
    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var cameraExecutor: ExecutorService

    private val handler = Handler(Looper.getMainLooper())

    abstract val scannerManager: ScannerManager
    abstract val captureView: View?
    abstract val previewView: PreviewView?
    abstract val captureButton: View?
    abstract val topBar: VaxToolbar?
    abstract val verifyPhoto: Boolean

    open fun onStartTakePhoto() {
        Timber.d("Start take photo")
    }

    open fun onTakePhoto(url: String, verify: Boolean) {
        Timber.d("Photo taken: $url, Need verify $verify")
    }

    open fun onEndVerifyPhoto(
        url: String,
        success: Boolean,
        captureText: String?
    ) {
        Timber.d("End verify photo $success")
    }

    open fun onTakePhotoFailed() {
        Timber.d("Take photo failed!")
    }

    override fun onDestroyView() {
        preview = null
        cameraExecutor.shutdown()
        super.onDestroyView()
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        scannerManager.resetLibrary()
        cameraExecutor = Executors.newSingleThreadExecutor()
        previewView?.let { prev ->
            prev.post {
                displayId = prev.display.displayId
                updateCameraUi()
                setUpCamera(prev)
            }
        }
    }

    private fun setImageAutoCapture(count: Int = 0) {
        val autoFocusPoint = SurfaceOrientedMeteringPointFactory(1f, 1f)
            .createPoint(.5f, .5f)
        val action = FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
            .build()

        val future = camera?.cameraControl?.startFocusAndMetering(action)
        future?.addListener({
            val result = future.get()
            if (result.isFocusSuccessful) {
                takePicture()
            } else {
                if (count < 2) {
                    setImageAutoCapture(count + 1)
                } else {
                    onFailed()
                }
            }
        }, cameraExecutor)
    }

    private fun takePicture() {
        imageCapture?.takePicture(
            cameraExecutor,
            object :
                ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Timber.e(exc, "Photo capture failed: ${exc.message}")
                    onFailed()
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    if (!isAdded || isRemoving) return
                    try {
                        safeLet(captureView, topBar, previewView) { cv, tb, pv ->
                            val bitmap = imageProxyToBitmap(image)
                            image.close()

                            Timber.d(
                                "Bitmap width: ${bitmap.width}, height: ${bitmap.height}"
                            )

                            // FIXME Only fit your own device size, not test on other devices
                            val scale = bitmap.width / pv.width.toFloat()
                            val x = cv.left * scale
                            val y =
                                max(
                                    ((cv.top - tb.height - (pv.height - bitmap.height / scale) / 2) * scale),
                                    0f
                                )

                            val width = cv.width * scale
                            val height = cv.height * scale

                            Timber.d(
                                "Capture size x: $x, y: $y, width: $width, " +
                                    "height: $height from bitmap width: ${bitmap.width}, " +
                                    "height: ${bitmap.height}"
                            )
                            val imageBitmap = Bitmap.createBitmap(
                                bitmap,
                                x.toInt(),
                                y.toInt(),
                                width.toInt(),
                                height.toInt(),
                                Matrix(),
                                true
                            )

                            val url = saveBitmap(imageBitmap)
                            bitmap.recycle()

                            if (url != null) {
                                handler.post { onTakePhoto(url, verifyPhoto) }

                                if (verifyPhoto) {
                                    val inputImage =
                                        InputImage.fromFilePath(requireContext(), Uri.parse(url))
                                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                        .process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val text = visionText.text
                                            if (text.isNotBlank() && text.length > 60) {
                                                handler.post { onEndVerifyPhoto(url, true, text) }
                                            } else {
                                                handler.post { onEndVerifyPhoto(url, false, text) }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            e.printStackTrace()
                                            handler.post { onEndVerifyPhoto(url, false, null) }
                                        }
                                }
                            } else {
                                onFailed()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Photo capture failed")
                        onFailed()
                    }
                    super.onCaptureSuccess(image)
                }
            }
        )
    }

    private fun onFailed() {
        handler.post {
            onTakePhotoFailed()
            context?.makeLongToast(R.string.patient_add_failed_to_take_photo)
        }
    }

    private fun updateCameraUi() {
        captureButton?.setOnSingleClickListener {
            onStartTakePhoto()
            setImageAutoCapture()
        }
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Rotate bitmap
        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun saveBitmap(bitmap: Bitmap): String? {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        val sd: String = sdf.format(Date(timeStamp))
        val fileName = "$sd.jpg"

        val file = File(requireContext().cacheDir, fileName)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            bitmap.recycle()
            return file.toUri().toString()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun setUpCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Build and bind the camera use cases
            bindCameraUseCases(previewView)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases(previewView: PreviewView) {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = previewView.display.rotation
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner, cameraSelector, preview, imageCapture
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Timber.e(exc, "Use case binding failed")
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
