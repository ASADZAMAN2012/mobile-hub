/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vaxcare.vaxhub.ui.PermissionsActivity

class UpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.MY_PACKAGE_REPLACED") {
            val launchApp = Intent(context, PermissionsActivity::class.java)
            launchApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchApp)
        }
    }
}
