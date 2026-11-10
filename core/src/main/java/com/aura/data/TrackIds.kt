package com.aura.data

import com.aura.data.local.LocalMusic
import com.aura.data.radio.Radio
import com.aura.data.subsonic.Subsonic

/**
 * Whether a song id is a YouTube video id, as opposed to a file on this
 * phone, a song on your own server or a radio station. Only YouTube songs can be resolved,
 * downloaded, upgraded, shared, followed with radio or measured for
 * loudness by YouTube.
 */
fun isYouTubeId(id: String): Boolean = !LocalMusic.isLocal(id) && !Subsonic.isSubsonic(id) && !Radio.isRadio(id)
