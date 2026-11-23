package com.aura.data.connect

import com.aura.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectManagerTest {

    @Test
    fun testSongAndCTrackConversion() {
        val song = Song(
            videoId = "test_123",
            title = "Test Track",
            artist = "Test Artist",
            thumbnailUrl = "https://example.com/art.jpg",
            durationText = "3:45",
            albumName = "Test Album",
        )

        val track = CTrack(
            id = song.videoId,
            title = song.title,
            artist = song.artist,
            thumb = song.thumbnailUrl,
            length = song.durationText,
            album = song.albumName,
        )

        assertEquals("test_123", track.id)
        assertEquals("Test Track", track.title)
        assertEquals("Test Artist", track.artist)
        assertEquals("https://example.com/art.jpg", track.thumb)
        assertEquals("3:45", track.length)
        assertEquals("Test Album", track.album)
    }

    @Test
    fun testConnectSessionViewUpdates() {
        val group = ConnectGroup.generate()
        val session = ConnectSession(group, "phone_123", "AURA Android")
        assertNotNull(session.view.value)
        assertEquals("phone_123", session.view.value.myId)
        assertEquals("AURA Android", session.view.value.myName)
        assertTrue(session.view.value.devices.isEmpty())
    }
}
