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
}
