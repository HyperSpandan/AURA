package com.aura.data.addons

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/** Lossless and Hi-Res streams are enabled across all audio outputs (built-in speakers, Bluetooth, wired, USB DAC, HDMI). */
object AddonOutput {
    fun init(context: Context) {
        AddonStore.outputAllowed = { true }
    }
}
