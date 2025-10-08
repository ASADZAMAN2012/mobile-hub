/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * This extension method will call the [com.vaxcare.core.viewmodel.KodeinViewModelFactory] and
 * inject dependencies into the view model. Requires the view model be registered in the graph
 * before this method can be used.
 *
 * @param VM The ViewModel type to be used
 * @param T The calling classes Type
 * @return by lazy, the view model that should be inflated
 */
inline fun <reified VM : ViewModel, T> T.viewModel(): Lazy<VM> where T : AppCompatActivity {
    return lazy { ViewModelProvider(this)[VM::class.java] }
}

/**
 * Hide the system UI (including the bottom bar and the nav bar). This is immersive so it will
 * not show the message to let the user know they can exit full screen mode.
 */
fun AppCompatActivity.hideSystemUi() {
    window.decorView.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LOW_PROFILE
    )
}
