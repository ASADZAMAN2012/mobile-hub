/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.core.span.FontSpan
import com.vaxcare.vaxhub.model.Operation
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.model.patient.PayerField
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.roundToInt

fun TextView.removeLast() {
    if (this.length() > 0) {
        this.text = StringBuilder(this.text.toString()).deleteAt(this.text.length - 1)
    }
}

fun TextView.setUnderlinedText(text: String) {
    val spannableString = SpannableString(text)
    spannableString.setSpan(UnderlineSpan(), 0, text.length, 0)
    this.text = spannableString
}

fun TextView.strikeThroughLined() {
    paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
    paint.isAntiAlias = true
}

fun TextView.removeLined() {
    paint.flags = 0
}

fun TextView.setEmpty() {
    this.text = ""
}

fun EditText.removeLast() {
    if (this.length() > 0) {
        this.editableText.delete(this.length() - 1, this.length())
    }
}

fun EditText.clear() {
    if (this.length() > 0) {
        this.editableText.delete(0, this.length())
    }
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun Int.toMoney(): String {
    val i = this.toFloat() / 100f
    return NumberFormat.getCurrencyInstance().format(i)
}

fun Int.toDp(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Context.formatString(
    @StringRes resource: Int,
    vararg string: Any
): String {
    return String.format(
        Locale.getDefault(),
        this.getString(resource),
        *string
    )
}

fun Context.formatHtml(
    @StringRes resource: Int,
    vararg string: Any
): Spanned {
    return Html.fromHtml(
        formatString(resource, *string),
        Html.FROM_HTML_MODE_LEGACY
    )
}

fun Int.toColorState(): ColorStateList {
    return ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled)
        ),
        intArrayOf(this, this)
    )
}

inline fun <reified T : Enum<T>> intToEnum(type: Int, unknown: T): T {
    val map = enumValues<T>().associateBy { t: T ->
        when (t) {
            is InventorySource -> t.id
            else -> t.ordinal
        }
    }
    return map[type] ?: unknown
}

inline fun <reified T : Enum<T>> stringToEnum(value: String, unknown: T): T {
    val map = enumValues<T>().associateBy { t: T ->
        when (t) {
            is Operation -> t.value
            else -> t.name
        }
    }

    return map[value] ?: unknown
}

fun Int.toFloorOf(value: Int): Int {
    val small = (this / value) * value
    val big = this + value
    return if (this - small > big - this) big else small
}

fun Int.getSuffix(): String =
    if (this in listOf(11, 12, 13)) {
        "th"
    } else {
        when (this % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

fun <T : Number> T.toPercentValue(scale: T): String {
    return "${(this.toFloat() * 100 / scale.toFloat()).roundToInt()}%"
}

fun TextView.setTextIfExist(text: CharSequence?) {
    if (text.isNullOrEmpty()) {
        this.hide()
    } else {
        this.show()
        this.text = text
    }
}

fun TextView.setSpannableIfExist(spannable: Spannable?) {
    if (spannable.isNullOrEmpty()) {
        this.hide()
    } else {
        this.show()
        this.text = spannable
    }
}

fun EditText.setFontHint(text: CharSequence, typeface: Typeface) {
    val spannableString = SpannableString(text)
    spannableString.setSpan(
        FontSpan(typeface),
        0,
        spannableString.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
    )
    hint = spannableString
}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(
            s: CharSequence,
            start: Int,
            count: Int,
            after: Int
        ) {}

        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {}
    })
}

private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Timber.e(throwable, "Error in coroutine")
}

fun CoroutineScope.safeLaunch(customContext: CoroutineContext? = null, work: suspend CoroutineScope.() -> Unit): Job {
    return if (customContext == null) {
        launch(
            context = coroutineExceptionHandler,
            block = work
        )
    } else {
        launch(context = coroutineExceptionHandler) {
            launch(context = customContext, block = work)
        }
    }
}

suspend fun <T> safeContext(customContext: CoroutineContext? = null, work: suspend CoroutineScope.() -> T): T {
    return if (customContext == null) {
        withContext(
            context = coroutineExceptionHandler,
            block = work
        )
    } else {
        withContext(context = coroutineExceptionHandler) {
            withContext(context = customContext, block = work)
        }
    }
}

fun Context.dpToPx(dp: Int): Int {
    val metrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(metrics)
    return ceil(dp * metrics.density.toDouble()).toInt()
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerBroadcastReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(receiver, intentFilter)
    }
}

/**
 * Allow checking if the input values are not null. If values are not null then the function block
 * is going to be executed
 *
 * @param T1 first value type
 * @param T2 second value type
 * @param R return value type
 * @param p1 first value
 * @param p2 second value
 * @param block function block to be executed if input values are not null
 * @return the value to be returned if any
 */
inline fun <T1 : Any, T2 : Any, R : Any> safeLet(
    p1: T1?,
    p2: T2?,
    block: (T1, T2) -> R?
): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}

/**
 * Allow checking if the input values are not null. If values are not null then the function block
 * is going to be executed
 *
 * @param T1 first value type
 * @param T2 second value type
 * @param R return value type
 * @param p1 first value
 * @param p2 second value
 * @param block function block to be executed if input values are not null
 * @return the value to be returned if any
 */
inline fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(
    p1: T1?,
    p2: T2?,
    p3: T3?,
    block: (T1, T2, T3) -> R?
): R? {
    return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
}

/**
 * Get an enum passed into bundle. There must be a valid ordinal value for T
 *
 * @param T type of enum
 * @param key key in bundle
 * @param default default when value is < 0
 */
inline fun <reified T : Enum<T>> Bundle.getEnum(key: String, default: T) =
    getInt(key, -1)
        .let { ordinal ->
            if (ordinal < 0) {
                default
            } else {
                enumValues<T>()[ordinal]
            }
        }

infix fun <A, B, C> Pair<A, B>.to(third: C) = Triple(first, second, third)

/**
 * With statement safe for nulls
 *
 * @param T type of caller
 * @param param caller
 * @param block block of work using param as the caller
 */
inline fun <T : Any> safeWith(param: T?, block: T.() -> Unit) = param?.block()

/**
 * Get and remove the first element of a set
 */
fun <T> MutableSet<T>.pop() = first().also { remove(it) }

/**
 * Gets a nullable parcelable from a bundle.
 */
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.retrieveParcelable(key: String): T? =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
        else -> getParcelable(key) as? T
    }

fun List<InfoField>.isMissingPayerFields(): Boolean =
    this.filterIsInstance<PayerField>()
        .filter { it !is PayerField.PortalMappingId }
        .filter { it !is PayerField.PlanId }
        .any { it.currentValue.isNullOrEmpty() }
