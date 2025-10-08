/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout

/**
 * A sliding drawer class that will push the content over. This assumes a basic structure where the
 * drawerLayout has one child that is the content (so the entire screen moves with the drawer).
 *
 * @property content the view that will be sliding. The assumption is this view is the first child
 *  on the drawerLayout
 * @constructor Creates the basic sliding drawer layout with the ActionBarDrawerToggle params and
 *  the content view that needs to slide over.
 * @param activity
 * @param drawer
 * @param open
 * @param close
 */
class SlidingDrawer(
    activity: AppCompatActivity,
    drawer: DrawerLayout,
    @StringRes open: Int,
    @StringRes close: Int,
    private val content: View
) : ActionBarDrawerToggle(activity, drawer, open, close) {
    /**
     * Move the content with the width of the drawerView and the slideOffset
     *
     * @param drawerView
     * @param slideOffset
     */
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        super.onDrawerSlide(drawerView, slideOffset)
        content.apply {
            translationX = drawerView.width * slideOffset
        }
    }
}
