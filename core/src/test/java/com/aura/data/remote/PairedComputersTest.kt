package com.aura.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairedComputersTest {

    @Test
    fun generatesValidSecret() {
        val secret = PairedComputers.generateSecret()
        assertEquals(43, secret.length)
        assertEquals(secret, PairedComputers.parse(secret))
    }

    @Test
    fun parsesVariousFormats() {
        val secret = PairedComputers.generateSecret()

        // 1. App link from QR code
        assertEquals(secret, PairedComputers.parse("aura://connect/#$secret"))

        // 2. Web player join URL
        assertEquals(secret, PairedComputers.parse("https://hyperspandan.github.io/AURA/player/#/settings/devices?join=$secret"))
        assertEquals(secret, PairedComputers.parse("https://hyperspandan.github.io/AURA/player/?join=$secret"))

        // 3. Raw 43-char string with surrounding whitespace
        assertEquals(secret, PairedComputers.parse("  $secret  "))

        // 4. Invalid strings and other aura links should return null
        assertNull(PairedComputers.parse("aura://pair/#1|192.168.1.1|4000"))
        assertNull(PairedComputers.parse("aura://remote/#something"))
        assertNull(PairedComputers.parse("https://example.com/other"))
        assertNull(PairedComputers.parse("short"))
        assertNull(PairedComputers.parse(null))
    }

    @Test
    fun addsAndRemovesComputers() {
        val storage = MemoryStorage()
        val manager = PairedComputers(storage)
        assertTrue(manager.computers.value.isEmpty())

        val secret = PairedComputers.generateSecret()
        val pc = PairedComputer(id = "pc1", name = "MacBook Pro", secret = secret)
        manager.add(pc)

        assertEquals(1, manager.computers.value.size)
        assertEquals("MacBook Pro", manager.computers.value.first().name)

        // Adding with same id/secret replaces/updates
        val pcUpdated = PairedComputer(id = "pc1", name = "MacBook Pro (Renamed)", secret = secret)
        manager.add(pcUpdated)
        assertEquals(1, manager.computers.value.size)
        assertEquals("MacBook Pro (Renamed)", manager.computers.value.first().name)

        manager.remove("pc1")
        assertTrue(manager.computers.value.isEmpty())
    }
}
