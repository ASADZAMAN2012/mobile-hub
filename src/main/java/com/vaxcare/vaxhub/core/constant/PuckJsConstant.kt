/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.constant

import java.util.UUID

object PuckJsConstant {
    /**
     * The descriptor configuration for Notify
     */
    val DESCRIPTOR_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     * The Puck.js service that is used to write the javascript and listen for the console
     */
    val PUCKJS_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

    /**
     * Transfer characteristic that is used solely for writing to the interpreter. This can cause
     * FIFO_FULL errors if the buffer is printing simultaneously.
     */
    val TX_CHARACTERISTIC = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    /**
     * Read characteristic used only for the notify characteristic on changed. You cannot read this
     * using the gatt onReadCharacteristic method since it does not allow read commands.
     */
    val RX_CHARACTERISTIC = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    /**
     * The manufacturer data that the Puck.js will send. This is the 3rd integer in the payload we
     * are advertising in the script. If that changes, so too will this.
     */
    const val MANUFACTURER_ID = 76

    /**
     * The current version checksum we are basing against. If the Puck.js has a checksum that does
     * not agree with what we have, consider it bad and we'll try to reflash.
     */
    const val CHECKSUM_DIGIT = 10

    /**
     * The following are key variables that need to be replaced within the puck.js code by the
     * kotlin code.
     */
    val KEYMAP: MutableMap<String, String> = mutableMapOf(
        makeKey("THRESHOLD_KEY") to "2500",
        makeKey("UPDATE_ADVERTISING_INTERVAL") to "500",
        makeKey("ADVERTISING_BROADCAST_INTERVAL") to "150",
        makeKey("MAG_SETTING") to "1.25",
        makeKey("SHIFT_INTERVAL") to "15000"
    )

    /**
     * The range of the data that the puck is collecting to monitor door events
     */
    const val QUEUE_START = 1
    const val QUEUE_END = 17

    /**
     * Simple function to generate the key for finding in the puck.js code.  Quotes are included
     * so that javascript recognizes the ints as ints and we don't want to call parseInt in the
     * javascript.
     *
     * @param key the key that will be replaced in the puckjs file
     * @return the full key, including quotations
     */
    private fun makeKey(key: String): String {
        return "\"@$@$key@$@\""
    }
}
