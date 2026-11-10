package com.aura.ui.browse

import com.aura.data.covers.ArtistSignatures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class ArtistSignatureTest {
    @Test
    fun testSignatureArchiveAndWikimedia() = runBlocking {
        val diljit = ArtistSignatures.signatureFor("Diljit Dosanjh", ArtistSignatures.DEFAULT_API)
        assertNotNull(diljit)
        assertTrue(diljit!!.isWhiteInk)
        assertTrue(diljit.imageUrl.contains("Diljit%20Dosanjh.webp"))

        val karan = ArtistSignatures.signatureFor("Karan Aujla", ArtistSignatures.DEFAULT_API)
        assertNotNull(karan)
        assertTrue(karan!!.isWhiteInk)

        val kanye = ArtistSignatures.signatureFor("Kanye West", ArtistSignatures.DEFAULT_API)
        assertNotNull(kanye)
        assertTrue(kanye!!.imageUrl.contains("Kanyewestsignature.svg"))
    }
}
