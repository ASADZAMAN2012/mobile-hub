/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub

import java.io.File

/**
 * Interface to access globally the current app info independently of the submodule
 */
interface AppInfo {
    /**
     * Get the application package with suffix
     *
     * @return the application package with suffix ex: com.vaxcare.vaxhub2.dev
     */
    val applicationId: String

    /**
     * Get the application Version Code
     *
     * @return the application version code
     */
    val versionCode: Int

    /**
     * Get the application Version Name
     *
     * @return the application version name ex: 1.0.0
     */
    val versionName: String

    /**
     * Get device Serial Number
     *
     * @return device serial number
     */
    val deviceSerialNumber: String

    /**
     * App Center Key
     *
     * @return the app center key for the environment
     */
    val appCenterKey: String

    /**
     * Build Variant
     *
     * @return the build variant name (could be debug, qa, staging, release)
     */
    val buildVariant: String

    /**
     * Package Name
     *
     * @return the package name of the hub
     */
    val packageName: String

    /**
     * Files Directory Pat
     *
     * @return the path to the files directory on the hub
     */
    val filesDirectoryPath: String

    /**
     * Mixpanel Token
     *
     * @return the Mixpanel token for the environment
     */
    val mixpanelKey: String

    val fileDirectory: File?
}

data class AppInfoImpl(
    override val applicationId: String,
    override val versionCode: Int,
    override val versionName: String,
    override val deviceSerialNumber: String,
    override val appCenterKey: String,
    override val buildVariant: String,
    override val packageName: String,
    override val filesDirectoryPath: String,
    override val mixpanelKey: String,
    override val fileDirectory: File?
) : AppInfo
