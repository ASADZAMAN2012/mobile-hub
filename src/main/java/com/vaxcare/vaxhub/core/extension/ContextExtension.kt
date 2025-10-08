/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.StringRes

/**
 * Show a long toast message on the UI with a given string placeholder
 *
 * This method is deprecated and should not be used except for in testing. Please use the
 * [makeLongToast] with a resource id instead.
 *
 * @param string the message to be displayed
 */
@Deprecated(
    message = "This is a testing only method. Do not use in production, " +
        "instead use string resources for production ready code.",
    replaceWith = ReplaceWith(
        "makeLongToast(R.string.YOUR_RESOURCE_HERE)",
        "import com.vaxcare.common.extension.makeLongToast"
    )
)
fun Context.makeLongToast(string: String) {
    Toast.makeText(this, string, Toast.LENGTH_LONG).show()
}

/**
 * Show a long toast message on the UI with a given string resource id
 *
 * @param string the resource id to be displayed
 */
fun Context.makeLongToast(
    @StringRes string: Int
) {
    Toast.makeText(this, string, Toast.LENGTH_LONG).show()
}

/**
 * Show a short toast message on the UI with a given string placeholder
 *
 * This method is deprecated and should not be used except for in testing. Please use the
 * [makeShortToast] with a resource id instead.
 *
 * @param string the message to be displayed
 */
@Deprecated(
    message = "This is a testing only method. Do not use in production, " +
        "instead use string resources for production ready code.",
    replaceWith = ReplaceWith(
        "makeShortToast(R.string.YOUR_RESOURCE_HERE)",
        "import com.vaxcare.common.extension.makeShortToast"
    )
)
fun Context.makeShortToast(string: String) {
    Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
}

/**
 * Show a short toast message on the UI with a given string resource id
 *
 * @param string the resource id to be displayed
 */
fun Context.makeShortToast(
    @StringRes string: Int
) {
    Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
}

/**
 * A simple extension method to return the layout inflater from the context object.
 *
 * @return the system Layout Inflater
 */
fun Context.getLayoutInflater(): LayoutInflater {
    return this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
}

/**
 * TODO
 *
 * @return
 */
fun Context.getWifiManager(): WifiManager {
    return this.getSystemService(Context.WIFI_SERVICE) as WifiManager
}

/**
 * Given a dimension attribute reference, returns the dimension as the closest integer
 *
 * @param attr
 * @param display
 * @return dimension
 */
fun Context.getAttributeDimension(
    @AttrRes attr: Int,
    display: DisplayMetrics
): Int {
    val typedValue = TypedValue()
    this.theme.resolveAttribute(attr, typedValue, true)
    return typedValue.getDimension(display).toInt()
}

/**
 * Given an integer attribute reference, returns the integer
 *
 * @param attr
 * @return the integer attribute
 */
fun Context.getAttributeInt(
    @AttrRes attr: Int
): Int {
    val typedValue = TypedValue()
    this.theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

/**
 * Given a color attribute reference, returns the color as an integer.
 *
 * @see getAttributeInt
 * @param attr
 * @return the color int
 */
fun Context.getAttributeColor(
    @AttrRes attr: Int
): Int = getAttributeInt(attr)

/**
 * Given a context, fetch the main looper so that we can post messages on the UI thread
 *
 * @param function anonymous function to run in the Handler
 */
fun Context.getMainThread(function: () -> Unit) {
    Handler(this.mainLooper).post(function)
}
