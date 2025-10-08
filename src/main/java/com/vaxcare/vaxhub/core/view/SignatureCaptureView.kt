/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.Window
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.dpToPx
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.round

class SignatureCaptureView : View {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    companion object {
        const val DISABLED_OPACITY = 0.5f
        const val ENABLED_OPACITY = 1.0f
    }

    /**
     * Callback for touch events on the SignatureCaptureView
     */
    var touchCallback: (MotionEvent?) -> Unit = { }

    /**
     * Bitmap for the background
     */
    var backgroundBmp: Bitmap = blankBitmap()
        set(value) {
            field = value
            bgPaddedTop = (abs((measuredHeight / 2) - (value.height / 2))).toFloat()
            bgBmp = Bitmap.createBitmap(value)
        }

    /**
     * Bitmap for the underline
     */
    var lineBmp: Bitmap = createLine()
        set(value) {
            field = value
            lnPaddedTop = (abs(measuredHeight - value.height)).toFloat()
            lnBmp = Bitmap.createBitmap(value)
        }

    private var bgBmp: Bitmap = Bitmap.createBitmap(backgroundBmp)
    private var lnBmp: Bitmap = Bitmap.createBitmap(lineBmp)

    var signatureColor: Int = Color.BLACK
        set(value) {
            field = value
            p.color = value
        }

    var lineColor: Int = Color.BLACK
        set(value) {
            field = value
            lineBmp = createLine()
        }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        adjustAlphaColor(enabled)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        lineBmp = createLine()
        lnPaddedTop = (abs(measuredHeight - lnBmp.height) - resources.getDimension(R.dimen.dp_60))
        bgPaddedTop = (abs((measuredHeight / 2) - (backgroundBmp.height / 2))).toFloat()
    }

    private fun adjustAlphaColor(opaque: Boolean) {
        val a = round(
            Color.alpha(signatureColor) * if (opaque) {
                ENABLED_OPACITY
            } else {
                DISABLED_OPACITY
            }
        ).toInt()
        val r = Color.red(signatureColor)
        val g = Color.green(signatureColor)
        val b = Color.blue(signatureColor)
        signatureColor = Color.argb(a, r, g, b)
    }

    private val p = Paint().apply {
        isAntiAlias = true
        color = signatureColor
        style = Paint.Style.STROKE
        strokeWidth = 8.0f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val sigPath = Path()
    private var bgPaddedTop: Float = 0f
    private var lnPaddedTop: Float = 0f
    private var readOnlyImage: Bitmap? = null

    fun applyReadOnlySignature(rawBytes: ByteArray) {
        val image = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        toggleSignatureLock(image != null)
        image?.let { img ->
            readOnlyImage = img
        }
    }

    /**
     * This will return a raw byte array of
     * the drawing cache via PixelCopy api (this is the entire view! If we
     * insert a background drawable, that will also be captured a long with
     * views being drawn on top of this)
     *
     * @param window - from Activity.getWindow().
     * @param callback - this is async so this callback will contain the result as parameter.
     */
    fun getSignatureBytes(
        window: Window,
        trimImage: Boolean = true,
        vertical: Boolean = true,
        callback: (ByteArray) -> Unit
    ) {
        adjustAlphaColor(true)
        val bitmap: Bitmap
        val location = IntArray(2)
        getLocationInWindow(location)

        val rect = if (vertical) {
            bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
            Rect(
                location[0],
                location[1],
                (location[0] + measuredWidth),
                (location[1] + measuredHeight)
            )
        } else {
            bitmap = Bitmap.createBitmap(measuredHeight, measuredWidth, Bitmap.Config.ARGB_8888)
            Rect(
                location[0],
                location[1] - measuredWidth,
                (location[0] + measuredHeight),
                (location[1])
            )
        }

        try {
            PixelCopy.request(
                window,
                rect,
                bitmap,
                { result ->
                    adjustAlphaColor(isEnabled)
                    if (result == PixelCopy.SUCCESS) {
                        val rotateBitmap = if (vertical) {
                            bitmap
                        } else {
                            rotate(bitmap)
                        }

                        val image =
                            if (trimImage) {
                                trim(rotateBitmap)
                            } else {
                                rotateBitmap
                            }

                        callback(convertBitmapToBytes(image))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotate(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)

        val resizedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        if (resizedBitmap != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return resizedBitmap
    }

    fun toggleSignatureLock(enable: Boolean = !isEnabled) {
        isEnabled = enable
    }

    fun resetCanvas() {
        sigPath.reset()
        readOnlyImage?.recycle()
        readOnlyImage = null
        bgBmp = Bitmap.createBitmap(backgroundBmp)
        postInvalidate()
    }

    private fun convertBitmapToBytes(bmp: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val res = stream.toByteArray()
        bmp.recycle()
        return res
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isEnabled) {
            bgBmp = blankBitmap()
            touchCallback(event)
            event?.let {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        sigPath.moveTo(it.x, it.y)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        sigPath.lineTo(it.x, it.y)
                    }

                    else -> Unit
                }
                postInvalidate()
            }
            return true
        }
        return false
    }

    private fun blankBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    private fun createLine(): Bitmap {
        val res = Bitmap.createBitmap(
            (measuredWidth - resources.getDimension(R.dimen.dp_120)).toInt().coerceAtLeast(1),
            context.dpToPx(6),
            Bitmap.Config.ARGB_8888
        )
        val allPixels = IntArray(res.width * res.height)
        res.setPixels(
            allPixels.map { lineColor }.toIntArray(),
            0,
            res.width,
            0,
            0,
            res.width,
            res.height
        )
        return res
    }

    private fun IntArray.mapAsArray(block: (Int) -> Int) {
        indices.forEach { this[it] = block(this[it]) }
    }

    fun trim(source: Bitmap): Bitmap {
        var firstX = 0
        var firstY = 0
        var lastX = source.width
        var lastY = source.height
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        pixels.mapAsArray {
            if (it != signatureColor) {
                0
            } else {
                it
            }
        }
        loop@ for (x in 0 until source.width) {
            for (y in 0 until source.height) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    firstX = x
                    break@loop
                }
            }
        }
        loop@ for (y in 0 until source.height) {
            for (x in firstX until source.width) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    firstY = y
                    break@loop
                }
            }
        }
        loop@ for (x in source.width - 1 downTo firstX) {
            for (y in source.height - 1 downTo firstY) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    lastX = x
                    break@loop
                }
            }
        }
        loop@ for (y in source.height - 1 downTo firstY) {
            for (x in source.width - 1 downTo firstX) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    lastY = y
                    break@loop
                }
            }
        }

        source.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        return Bitmap.createBitmap(source, firstX, firstY, lastX - firstX, lastY - firstY)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bgBmp, 0f, bgPaddedTop, p)
        canvas.drawBitmap(lnBmp, resources.getDimension(R.dimen.dp_60), lnPaddedTop, p)
        readOnlyImage?.let { canvas.drawBitmap(it, 0f, 0f, p) }
            ?: run { canvas.drawPath(sigPath, p) }
    }
}
