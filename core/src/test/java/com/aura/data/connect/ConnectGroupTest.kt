package com.aura.data.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ConnectGroupTest {

    @Test
    fun matchesTestVectors() {
        val secretB64 = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE"
        val expectedTag = "8a70946b4b85c417d9d8de598995cf71"
        val expectedPlain = "hello connect"
        val sealedVector = "BwcHBwcHBwcHBwcHB2BINKpvS/fjROeStiRMXgyxY9suTPf5EiqmZr0="

        val group = ConnectGroup.fromText(secretB64)
        assertNotNull(group)
        assertEquals(expectedTag, group!!.tag)

        // Decrypt vector sealed by Web
        val decrypted = group.open(sealedVector)
        assertEquals(expectedPlain, decrypted)

        // Roundtrip seal & open
        val sealed = group.seal("test message sync")
        val opened = group.open(sealed)
        assertEquals("test message sync", opened)
    }

    @Test
    fun parseVariousFormats() {
        val group = ConnectGroup.generate()
        val encoded = group.encoded

        assertEquals(encoded, ConnectGroup.parse(group.appLink))
        assertEquals(encoded, ConnectGroup.parse(group.webLink))
        assertEquals(encoded, ConnectGroup.parse("  $encoded  "))
        assertNull(ConnectGroup.parse("invalid://something"))
    }

    @Test
    fun decodeWebPlayerStateAndPresence() {
        val presenceJson = """{"type":"presence","from":"web_123","name":"Chrome","form":"web","seq":1732400000000,"at":1732400000000,"active":true}"""
        val presence = ConnectMsg.decode(presenceJson)
        assertNotNull(presence)
        assertTrue(presence is ConnectMsg.Presence)
        val p = presence as ConnectMsg.Presence
        assertEquals("web_123", p.from)
        assertEquals("Chrome", p.name)
        assertEquals("web", p.form)

        val stateJson = """{"type":"state","from":"web_123","seq":1732400000001,"at":1732400000001,"track":{"id":"dQw4w9WgXcQ","title":"Never Gonna Give You Up","artist":"Rick Astley","thumb":"https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg","length":"3:32","album":null},"private":false,"playing":true,"pos":45000,"queue":[],"volume":1,"shuffle":false,"repeat":"off","since":1732400000000,"quality":"Lossless"}"""
        val state = ConnectMsg.decode(stateJson)
        assertNotNull(state)
        assertTrue(state is ConnectMsg.State)
        val s = state as ConnectMsg.State
        assertEquals(true, s.playing)
        assertEquals(45000L, s.pos)
        assertEquals("Never Gonna Give You Up", s.track?.title)

        val nullTrackJson = """{"type":"state","from":"web_123","seq":1732400000002,"at":1732400000002,"track":null,"private":true,"playing":false,"pos":0,"queue":[],"volume":0.8,"shuffle":false,"repeat":"off"}"""
        val stateNullTrack = ConnectMsg.decode(nullTrackJson)
        assertNotNull(stateNullTrack)
        assertTrue(stateNullTrack is ConnectMsg.State)
        assertNull((stateNullTrack as ConnectMsg.State).track)

        val floatPosJson = """{"type":"state","from":"web_123","seq":1732400000003,"at":1732400000003,"track":null,"playing":true,"pos":1234.5}"""
        val stateFloatPos = ConnectMsg.decode(floatPosJson)
        assertNotNull(stateFloatPos)
        assertTrue(stateFloatPos is ConnectMsg.State)
        assertEquals(1234L, (stateFloatPos as ConnectMsg.State).pos)
    }
}
