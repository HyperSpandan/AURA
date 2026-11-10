package com.aura.data.lossless

import java.text.Normalizer
import java.util.Locale

/** Conservative cross-catalog identity: matches exact or cleaned YouTube titles while rejecting covers and remixes. */
internal data class CatalogIdentity(val title: String, val artist: String, val album: String?, val durationMs: Long) {
    private val cleaned = com.aura.data.lyrics.TrackNameCleaner.clean(title, artist)

    fun matches(title: String, artists: List<String>, album: String?, durationMs: Long): Boolean {
        if (key(this.title).isEmpty() || key(this.artist).isEmpty()) return false
        val candidateKey = key(title)
        val ownKey = key(this.title)
        val cleanKey = key(cleaned.title)
        val candidateCleanKey = key(com.aura.data.lyrics.TrackNameCleaner.clean(title, artists.firstOrNull().orEmpty()).title)
        val titleMatch = ownKey == candidateKey ||
            (cleanKey.isNotEmpty() && cleanKey == candidateKey) ||
            (candidateCleanKey.isNotEmpty() && (ownKey == candidateCleanKey || (cleanKey.isNotEmpty() && cleanKey == candidateCleanKey)))
        if (!titleMatch) return false

        val artistMatch = artistsMatch(this.artist, artists)
        if (!artistMatch) return false

        if (!this.album.isNullOrBlank() && !album.isNullOrBlank() && key(this.album) != key(album)) return false

        if (!LosslessSource.durationMatches(this.durationMs, durationMs)) return false

        if (otherVersion(title, album)) return false

        return true
    }

    /**
     * An instrumental, karaoke or other version filed under the song's own title, given
     * away only by its album ("Tum Hi Ho" on "Aashiqui 2 (Instrumental)"): it played the
     * music without the vocals while the lyrics ran on. Words the wanted song has itself
     * don't count against it.
     */
    private fun otherVersion(title: String, album: String?): Boolean {
        val own = versionsIn("${this.title} ${this.album.orEmpty()}")
        return (versionsIn(title) + versionsIn(album.orEmpty())).any { it !in own }
    }

    companion object {
        private val VERSION = Regex(
            "\\b(instrumentals?|karaoke|backing track|minus one|without vocals|no vocals|music only|" +
                "acapella|a cappella|lofi|lo fi|slowed|reverb|sped up|8d|nightcore|remix|unplugged|" +
                "live|tribute|cover|acoustic|mashup|edit|mix)\\b|伴奏",
            RegexOption.IGNORE_CASE,
        )

        private fun versionsIn(text: String): Set<String> =
            VERSION.findAll(key(text)).mapTo(HashSet()) { it.value.replace(" ", "").removeSuffix("s") }

        // Queue bylines join performers, while catalog APIs return an array.
        // Keep exact band names first (e.g. Simon & Garfunkel), then require
        // either all named performers or the primary performer.
        private fun artistsMatch(credit: String, artists: List<String>): Boolean {
            if (artists.any { key(it) == key(credit) }) return true
            val separators = Regex("\\s*(?:,|&|;|\\u2022)\\s*|\\s+(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)
            val wanted = credit.split(separators).map(::key).filter(String::isNotEmpty)
            val available = artists.flatMap { listOf(key(it)) + it.split(separators).map(::key) }.toSet()
            if (wanted.isEmpty() || available.isEmpty()) return false
            return (wanted.size > 1 && wanted.all { it in available }) ||
                available.all { it in wanted }
        }

        fun key(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ").trim()
    }
}
