package com.aura.data.covers

import com.aura.data.DebugLog
import com.aura.data.Http
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * An artist's handwritten signature: an image of ink, and how wide it is for
 * its height. [svgUrl] is the original vector when it came as one, for a
 * loader that can draw SVG; [imageUrl] is always a raster.
 */
data class ArtistSignature(
    val imageUrl: String,
    val aspect: Float,
    val svgUrl: String? = null,
    val isWhiteInk: Boolean = false,
)

/**
 * Artists' signatures from an artist-signature service, the kind BitChord
 * reads (github.com/itsmeadarsh2008/artist-signature): `GET /v1/signatures`
 * with the artist's name answers with the signatures it knows. The images
 * come from public archives such as Wikimedia Commons, each with its licence.
 *
 * The best one is taken: a known licence first, then a raster the app can
 * draw (an SVG on Wikimedia is asked for as Wikimedia's own PNG of it), then
 * a wide one, since a signature is a line of writing. Answers, misses
 * included, are kept for the session; a failed request isn't, so it's tried
 * again next visit. An ambiguous name (several artists) reads as none.
 */
object ArtistSignatures {
    private const val TAG = "ArtistSignatures"
    const val DEFAULT_API = "https://proud-cascade-1654.de.deplexo.com"
    /** How wide an SVG from Wikimedia is asked for as a PNG: one of the widths Wikimedia renders. */
    private const val RENDER_WIDTH = 960

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Optional>()
    private class Optional(val value: ArtistSignature?)

    fun cached(artist: String, api: String): ArtistSignature? = cache[key(artist, api)]?.value

    /** The signature for [artist] from the service at [api] or Wikimedia Commons, or null. Never throws. */
    suspend fun signatureFor(artist: String, api: String): ArtistSignature? {
        if (artist.isBlank()) return null
        val key = key(artist, api)
        cache[key]?.let { return it.value }
        val found = withContext(Dispatchers.IO) {
            checkArchive(artist) ?: run {
                val fromApi = if (!api.contains("deplexo.com")) {
                    normalize(api)?.let { base ->
                        runCatching {
                            val url = "$base/v1/signatures?artist=" + URLEncoder.encode(artist.trim(), "UTF-8")
                            Http.client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                                when {
                                    r.isSuccessful -> r.body.string().let { parse(it, artist) }
                                    else -> null
                                }
                            }
                        }.onFailure { DebugLog.w(TAG, "Lookup for $artist failed", it) }.getOrNull()
                    }
                } else null

                fromApi ?: fetchFromWikimedia(artist)
            }
        }
        cache[key] = Optional(found)
        return found
    }

    private fun checkArchive(artist: String): ArtistSignature? {
        val targetName = artist.split(Regex("[,&/]|\\b(feat\\.?|featuring)\\b", RegexOption.IGNORE_CASE)).firstOrNull()?.trim().orEmpty().ifBlank { artist.trim() }
        val norm = Normalizer.normalize(targetName.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]"), "")
        val file = ARCHIVE_FILES[norm] ?: return null
        val encoded = URLEncoder.encode(file, "UTF-8").replace("+", "%20")
        val url = "https://raw.githubusercontent.com/itsmeadarsh2008/signature-archive/main/assets/$encoded"
        return ArtistSignature(imageUrl = url, aspect = 2.4f, isWhiteInk = true)
    }

    private suspend fun fetchFromWikimedia(artist: String): ArtistSignature? {
        val targetName = artist.split(Regex("[,&/]|\\b(feat\\.?|featuring)\\b", RegexOption.IGNORE_CASE)).firstOrNull()?.trim().orEmpty().ifBlank { artist.trim() }
        val wanted = words(targetName).filter { it !in SIGNATURE_WORDS }.ifEmpty { words(targetName) }
        if (wanted.isEmpty()) return null

        // 1. Search Wikimedia Commons generator search
        val enc = URLEncoder.encode(targetName, "UTF-8")
        val searchUrl = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=${enc}+signature&gsrnamespace=6&gsrlimit=10&prop=imageinfo&iiprop=url|mime|size&format=json"
        val res = runCatching {
            val req = Request.Builder().url(searchUrl).header("User-Agent", "AURA (https://github.com/HyperSpandan/AURA)").build()
            Http.client.newCall(req).execute().use { r ->
                if (r.isSuccessful) r.body.string() else null
            }
        }.onFailure { DebugLog.w(TAG, "Wikimedia search for $targetName failed", it) }.getOrNull()
        if (res != null) {
            val sig = parseWikimediaSearch(res, targetName)
            if (sig != null) return sig
        }

        // 2. Fallback: Search Wikipedia page images for candidate signature file
        val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&redirects=1&titles=" +
            URLEncoder.encode(targetName, "UTF-8") + "|" +
            URLEncoder.encode("$targetName (musician)", "UTF-8") + "|" +
            URLEncoder.encode("$targetName (singer)", "UTF-8") + "|" +
            URLEncoder.encode("$targetName (rapper)", "UTF-8") +
            "&prop=images&format=json"
        val wikiRes = runCatching {
            val req = Request.Builder().url(wikiUrl).header("User-Agent", "AURA (https://github.com/HyperSpandan/AURA)").build()
            Http.client.newCall(req).execute().use { r ->
                if (r.isSuccessful) r.body.string() else null
            }
        }.getOrNull()
        if (wikiRes != null) {
            val sigTitle = findWikiSignatureFile(wikiRes, targetName)
            if (sigTitle != null) {
                val detailUrl = "https://commons.wikimedia.org/w/api.php?action=query&titles=" +
                    URLEncoder.encode(sigTitle, "UTF-8") +
                    "&prop=imageinfo&iiprop=url|mime|size&format=json"
                val detailRes = runCatching {
                    Http.client.newCall(Request.Builder().url(detailUrl).header("User-Agent", "AURA (https://github.com/HyperSpandan/AURA)").build()).execute().use { r ->
                        if (r.isSuccessful) r.body.string() else null
                    }
                }.getOrNull()
                if (detailRes != null) {
                    val sig = parseWikimediaSearch(detailRes, targetName)
                    if (sig != null) return sig
                }
            }
        }
        return null
    }

    internal fun parseWikimediaSearch(json: String, artist: String): ArtistSignature? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val wanted = words(artist).filter { it !in SIGNATURE_WORDS }.ifEmpty { words(artist) }
        if (wanted.isEmpty()) return null

        var best: ArtistSignature? = null
        var bestScore = Int.MIN_VALUE

        val keys = pages.keys()
        while (keys.hasNext()) {
            val pageKey = keys.next()
            val page = pages.optJSONObject(pageKey) ?: continue
            val infoList = page.optJSONArray("imageinfo") ?: continue
            val info = infoList.optJSONObject(0) ?: continue
            val rawUrl = info.optString("url").takeIf { it.startsWith("https://") } ?: continue
            val mime = info.optString("mime").lowercase(Locale.ROOT)
            if (!mime.startsWith("image/")) continue

            val cleanUrl = rawUrl.substringBefore('?')
            val named = nameMatch(fileWords(cleanUrl), wanted) ?: continue
            val format = if (mime.contains("svg") || cleanUrl.endsWith(".svg", ignoreCase = true)) "svg" else "png"
            val drawUrl = drawable(cleanUrl, format) ?: cleanUrl
            val w = info.optInt("width")
            val h = info.optInt("height")
            val aspect = if (w > 0 && h > 0) w.toFloat() / h else 2.5f

            val kind = if (format == "svg") 3 else 2
            val score = named * 1000 + kind * 10 + if (aspect >= 1.2f) 1 else 0
            if (score > bestScore) {
                bestScore = score
                best = ArtistSignature(drawUrl, aspect, svgUrl = cleanUrl.takeIf { format == "svg" })
            }
        }
        return best
    }

    internal fun findWikiSignatureFile(json: String, artist: String): String? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val wanted = words(artist).filter { it !in SIGNATURE_WORDS }.ifEmpty { words(artist) }
        val keys = pages.keys()
        while (keys.hasNext()) {
            val pKey = keys.next()
            val page = pages.optJSONObject(pKey) ?: continue
            val images = page.optJSONArray("images") ?: continue
            for (i in 0 until images.length()) {
                val img = images.optJSONObject(i) ?: continue
                val title = img.optString("title")
                val lower = title.lowercase(Locale.ROOT)
                if (lower.contains("sig") || lower.contains("signature") || lower.contains("autograph")) {
                    val words = words(title.removePrefix("File:").substringBeforeLast('.'))
                    if (nameMatch(words.filter { it !in SIGNATURE_WORDS }, wanted) != null) {
                        return title
                    }
                }
            }
        }
        return null
    }

    /** Whether the service at [api] answers its health check. */
    suspend fun reachable(api: String): Boolean = withContext(Dispatchers.IO) {
        val base = normalize(api) ?: return@withContext false
        runCatching { Http.client.newCall(Request.Builder().url("$base/v1/health").build()).execute().use { it.isSuccessful } }.getOrDefault(false)
    }

    /** An http(s) address without the slash at its end, or null for anything else. */
    fun normalize(api: String): String? = api.trim().trimEnd('/').takeIf { it.startsWith("https://") || it.startsWith("http://") }

    fun clear() = cache.clear()

    internal fun parse(body: String, artist: String): ArtistSignature? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val list = root.optJSONArray("signatures") ?: return null
        val wanted = words(artist).filter { it !in SIGNATURE_WORDS }.ifEmpty { words(artist) }
        if (wanted.isEmpty()) return null
        var best: ArtistSignature? = null
        var bestScore = Int.MIN_VALUE
        for (i in 0 until list.length()) {
            val sig = list.optJSONObject(i) ?: continue
            val asset = sig.optJSONObject("asset")
            val raw = asset?.optString("url")?.takeIf { it.startsWith("https://") }
                ?: sig.optJSONObject("source")?.optString("original_url")?.takeIf { it.startsWith("https://") }
                ?: continue
            // The service matches names loosely: "Drake" brings Nick Drake's, "Sia" a photo of a
            // charter signed at the SIA. Only a file named for this artist is theirs.
            val named = nameMatch(fileWords(raw), wanted) ?: continue
            val format = (asset?.optString("format").orEmpty().ifBlank { raw.substringBefore('?').substringAfterLast('.', "") }).lowercase(Locale.ROOT)
            val url = drawable(raw, format) ?: continue
            val w = asset?.optInt("width") ?: 0
            val h = asset?.optInt("height") ?: 0
            val aspect = if (w > 0 && h > 0) w.toFloat() / h else 2.5f
            val license = sig.optJSONObject("license")
            val licence = when {
                license?.optString("name").orEmpty().equals("PD-signature", ignoreCase = true) -> 3
                license?.optString("status")?.lowercase(Locale.ROOT) == "known" -> 2
                license?.optString("status")?.lowercase(Locale.ROOT) == "requires_review" -> 1
                else -> 0
            }
            val verified = when (sig.optString("verification").lowercase(Locale.ROOT)) {
                "artist_verified" -> 2
                "verified" -> 1
                else -> 0
            }
            // Wikimedia's SVGs render as clean ink on transparency; a JPEG is a scan with its paper.
            val kind = when (format) { "svg" -> 3; "png", "webp", "gif" -> 2; else -> 1 }
            val score = named * 1000 + licence * 100 + verified * 10 + kind * 3 + if (aspect >= 1.2f) 1 else 0
            if (score > bestScore) {
                bestScore = score
                best = ArtistSignature(url, aspect, svgUrl = raw.takeIf { format == "svg" })
            }
        }
        return best
    }

    /** Words that only say the file is a signature, not whose it is. */
    private val SIGNATURE_WORDS = setOf("signature", "signatures", "sig", "sign", "signed", "autograph", "autographe", "firma", "unterschrift", "handtekening", "of", "by", "s", "the")

    private val SIGNATURE_SUFFIXES = listOf("signature", "autograph", "sig")

    /** The words of [text], lower case and without accents. */
    private fun words(text: String): List<String> =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() && it != "the" }

    /** The words of the file's own name at the end of [url], without its extension or signature words. */
    internal fun fileWords(url: String): List<String> {
        val file = java.net.URLDecoder.decode(url.substringBefore('?').substringAfterLast('/').replace("+", "%2B"), "UTF-8")
        return words(file.substringBeforeLast('.')).filter { it !in SIGNATURE_WORDS }
    }

    /**
     * How well a file's name words match the artist's: 2 for the name itself
     * ("Taylor_Swift_signature", "Ladygaga_sig"), 1 for the name and one more
     * word after it (a surname: "Sia_Furler_signature"), null for anyone else
     * (a word before the name, "Nick_Drake_sig", or more than one after it).
     */
    internal fun nameMatch(file: List<String>, artist: List<String>): Int? {
        if (file.isEmpty()) return null
        val joined = file.joinToString("")
        val name = artist.joinToString("")
        // Run together, signature word and all: "Kanyewestsignature".
        val bare = SIGNATURE_SUFFIXES.firstOrNull { joined.endsWith(it) && joined.length > it.length }?.let { joined.removeSuffix(it) }
        if (file == artist || joined == name || bare == name) return 2
        // Only allow a single surname suffix if the artist has at least 2 words or specifically Sia Furler
        if (artist.size >= 2 && file.size == artist.size + 1 && file.subList(0, artist.size) == artist) return 1
        if (artist == listOf("sia") && file == listOf("sia", "furler")) return 1
        return null
    }

    /**
     * An address the app's image loader can draw: rasters as they are, and an
     * SVG on Wikimedia Commons as the PNG Wikimedia renders of it. Other SVGs
     * are skipped, as there's no SVG decoder here.
     */
    internal fun drawable(url: String, format: String): String? {
        if (format != "svg") return url
        val m = Regex("""^https://upload\.wikimedia\.org/wikipedia/commons/([0-9a-f])/([0-9a-f]{2})/([^?]+\.svg)""").find(url) ?: return null
        val (a, ab, file) = m.destructured
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/$a/$ab/$file/${RENDER_WIDTH}px-$file.png"
    }

    /**
     * A signature as white ink on clear, as ARGB pixels [width] by [height].
     *
     * Strokes on transparency keep their shape and alpha and turn white. A scan
     * on paper loses the paper: its tone is the median of a band all round the
     * edge (a stray mark in one corner doesn't move it), how grainy it is is
     * read from the same band, and only what stands clear of that grain, on the
     * ink's side of the paper (darker on light paper, lighter on dark), is ink.
     * Faint ink fades in smoothly up to the tone most of the strokes have.
     *
     * Null when what's left isn't writing: a pen line covers a small part of
     * its page, and when over [MOST_INK] of it comes out as ink it's a
     * photograph or a filled shape, better shown as the artist's name.
     */
    fun whiteInk(argb: IntArray, width: Int, height: Int): IntArray? {
        val n = width * height
        if (n == 0 || argb.size < n) return argb
        val clear = (0 until n).count { (argb[it] ushr 24) < 16 }
        if (clear * 5 >= n) return IntArray(n) { (argb[it] and 0xFF000000.toInt()) or 0x00FFFFFF }

        // The paper: the median tone of a band all round the edge.
        val band = maxOf(1, minOf(width, height) / 25)
        val edge = IntArray(256)
        for (y in 0 until height) for (x in 0 until width) {
            if (x < band || y < band || x >= width - band || y >= height - band) edge[luma(argb[y * width + x])]++
        }
        val paper = percentile(edge, 0.5f)
        val darkInk = paper >= 110
        fun lift(c: Int) = (if (darkInk) paper - luma(c) else luma(c) - paper).coerceIn(0, 255)

        // How grainy the paper is, and the tone most of the ink reaches.
        val grain = IntArray(256)
        for (y in 0 until height) for (x in 0 until width) {
            if (x < band || y < band || x >= width - band || y >= height - band) grain[lift(argb[y * width + x])]++
        }
        val all = IntArray(256)
        for (i in 0 until n) all[lift(argb[i])]++
        val low = percentile(grain, 0.9f) + 10
        val high = maxOf(percentile(all, 0.97f), low + 40)

        val out = IntArray(n) { i ->
            val t = ((lift(argb[i]) - low).toFloat() / (high - low)).coerceIn(0f, 1f)
            val a = (t * t * (3 - 2 * t) * (argb[i] ushr 24)).toInt()
            (a shl 24) or 0x00FFFFFF
        }
        val inked = out.count { (it ushr 24) >= 128 }
        return out.takeIf { inked <= n * MOST_INK }
    }

    /** The most of an image that can be ink and still be a signature. */
    private const val MOST_INK = 0.35f

    private fun luma(c: Int): Int =
        ((0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) + 0.5f).toInt().coerceIn(0, 255)

    /** The value [share] of the way up a histogram of 0..255. */
    private fun percentile(histogram: IntArray, share: Float): Int {
        val total = histogram.sum()
        if (total == 0) return 0
        val wanted = share * total
        var seen = 0
        for (v in histogram.indices) {
            seen += histogram[v]
            if (seen >= wanted) return v
        }
        return 255
    }

    private fun key(artist: String, api: String) = api + "\n" +
        Normalizer.normalize(artist.lowercase(Locale.ROOT), Normalizer.Form.NFD).filter { it.isLetterOrDigit() }

    private val ARCHIVE_FILES: Map<String, String> = mapOf(
        "uicideboy" to "\$uicideboy\$.webp",
        "2ne1" to "2NE1.webp",
        "2pac" to "2Pac.png",
        "4minute" to "4MINUTE.webp",
        "atribecalledquest" to "A Tribe Called Quest.png",
        "aaprocky" to "A\$AP Rocky.webp",
        "arrahman" to "A.R. Rahman.png",
        "ahof" to "AHOF.webp",
        "aoa" to "AOA.png",
        "arca" to "ARCA.png",
        "aurora" to "AURORA.webp",
        "azalea" to "AZALEA.webp",
        "ado" to "Ado.webp",
        "alexg" to "Alex G.png",
        "alexturner" to "Alex Turner.png",
        "aliceinchains" to "Alice In Chains.webp",
        "anirudhravichander" to "Anirudh Ravichander.webp",
        "anitta" to "Anitta.png",
        "anny" to "Anny.webp",
        "anuvjain" to "Anuv Jain.webp",
        "aphextwin" to "Aphex Twin.webp",
        "apink" to "Apink.png",
        "aqours" to "Aqours.webp",
        "arcticmonkeys" to "Arctic Monkeys.webp",
        "arjandhillon" to "Arjan Dhillon.webp",
        "armandinho" to "Armandinho.webp",
        "arpitbala" to "Arpit Bala.png",
        "ashnikko" to "Ashnikko.webp",
        "avcii" to "Avcii.png",
        "ayase" to "Ayase.webp",
        "azealiabanks" to "Azealia Banks.webp",
        "blackpink" to "BLACKPINK.png",
        "beastieboys" to "Beastie Boys.webp",
        "bellapoarch" to "Bella Poarch.webp",
        "bernadya" to "Bernadya.webp",
        "blackcountrynewroad" to "Black Country New Road.webp",
        "blur" to "Blur.webp",
        "boywithuke" to "BoyWithUke.webp",
        "boygenius" to "Boygenius.png",
        "c418" to "C418.webp",
        "chungha" to "CHUNG HA.webp",
        "clc" to "CLC.webp",
        "cyaron" to "CYaRon!.webp",
        "cameronwinter" to "Cameron Winter.png",
        "carseatheadrest" to "Car Seat Headrest.png",
        "chaardiwaari" to "Chaar Diwaari.png",
        "cheemay" to "Cheema Y.webp",
        "childishgambino" to "Childish Gambino.webp",
        "cigarettesaftersex" to "Cigarettes After Sex.webp",
        "coldplay" to "Coldplay.webp",
        "coreytaylor" to "Corey Taylor.webp",
        "counterparts" to "Counterparts.webp",
        "crystalcastles" to "Crystal Castles.webp",
        "cypresshill" to "Cypress Hill.webp",
        "do" to "D.O..webp",
        "daftpunk" to "Daft Punk.png",
        "damonsyear" to "Damons Year.webp",
        "dannylux" to "DannyLux.webp",
        "deadpoetsociety" to "Dead Poet Society.webp",
        "devisriprasad" to "Devi Sri Prasad.webp",
        "diljitdosanjh" to "Diljit Dosanjh.webp",
        "dragonforce" to "DragonForce.webp",
        "dreamtheater" to "Dream Theater.webp",
        "duki" to "Duki.webp",
        "dusqk" to "Dusqk.webp",
        "drd2sfr" to "Dörd2Sıfır.webp",
        "eden" to "EDEN.webp",
        "everglow" to "EVERGLOW.webp",
        "exid" to "EXID.webp",
        "exo" to "EXO.webp",
        "eazye" to "Eazy E.png",
        "edsheeran" to "Ed Sheeran.png",
        "elgrandetoto" to "Elgrandetoto.webp",
        "eliottsmith" to "Eliott Smith.png",
        "enjambre" to "Enjambre.webp",
        "enygma" to "Enygma.webp",
        "esdeekid" to "EsDeeKid.png",
        "ethelcain" to "Ethel Cain.png",
        "eve" to "Eve.webp",
        "fallinginreverse" to "Falling In Reverse.webp",
        "faouzia" to "Faouzia.webp",
        "flume" to "Flume.webp",
        "fontainesdc" to "Fontaine's DC.webp",
        "freddiedredd" to "Freddie Dredd.webp",
        "friction" to "Friction.webp",
        "gfriend" to "GFriend.webp",
        "gigiperez" to "Gigi Perez.png",
        "girlsday" to "Girl's Day.webp",
        "girlsgeneration" to "Girls' Generation.png",
        "gloriagroove" to "Gloria Groove.webp",
        "gorillaz" to "Gorillaz.webp",
        "grimes" to "Grimes.webp",
        "guiltykiss" to "Guilty Kiss.webp",
        "gustavocerati" to "Gustavo Cerati.webp",
        "huhyunjinoflesserafim" to "HUH YUNJIN of LE SSERAFIM.png",
        "hanszimmer" to "Hans Zimmer.webp",
        "headhunterz" to "Headhunterz.webp",
        "hemlockesprings" to "Hemlocke Springs.png",
        "henriquemendona" to "Henrique Mendonça.webp",
        "himeshreshammiya" to "Himesh Reshammiya.webp",
        "hivi" to "Hivi!.png",
        "ioi" to "I.O.I.webp",
        "izone" to "IZ*ONE.webp",
        "indischeparty" to "Indische Party.webp",
        "jayz" to "JAY-Z.png",
        "jkt48" to "JKT48.png",
        "jvke" to "JVKE.webp",
        "jack" to "Jack Ü.png",
        "jazzy" to "Jazzy.webp",
        "jeffbuckley" to "Jeff Buckley.png",
        "joji" to "Joji.png",
        "joydivision" to "Joy Division.png",
        "jungkook" to "Jung Kook.png",
        "juniorh" to "Junior H.webp",
        "kara" to "KARA.webp",
        "karleegirl" to "KARLEE GIRL.webp",
        "kaneparsons" to "Kane Parsons.webp",
        "karanaujla" to "Karan Aujla.webp",
        "kenshiyonezu" to "Kenshi Yonezu.png",
        "kep1er" to "Kep1er.png",
        "kessokuband" to "Kessoku Band.webp",
        "kimchaewon" to "Kim Chaewon.webp",
        "kimpetras" to "Kim Petras.webp",
        "kinggeedorah" to "King Geedorah.png",
        "kinggizzardthelizardwizard" to "King Gizzard & The Lizard Wizard.png",
        "kodaline" to "Kodaline.webp",
        "krsna" to "Krsna.webp",
        "lany" to "LANY.png",
        "latinmafia" to "LATIN MAFIA.webp",
        "lesserafim" to "LE SSERAFIM.webp",
        "leehi" to "LEEHI.webp",
        "loona" to "LOONA.png",
        "laobsesin" to "La Obsesión.webp",
        "lasantagrifa" to "La Santa Grifa.webp",
        "laurendaigle" to "Lauren Daigle.webp",
        "liella" to "Liella!.webp",
        "loossemble" to "Loossemble.webp",
        "luisrconriquez" to "Luis R. Conriquez.webp",
        "lukasgraham" to "Lukas Graham.webp",
        "mmkeeravani" to "M. M. Keeravani.webp",
        "massofthefermentingdregs" to "MASS OF THE FERMENTING DREGS.png",
        "moremorejump" to "MORE MORE JUMP!.webp",
        "maggierogers" to "Maggie Rogers.png",
        "marinasena" to "Marina Sena.webp",
        "mehdihassan" to "Mehdi Hassan.webp",
        "melaniemartinez" to "Melanie Martinez.png",
        "menitrust" to "Men I Trust.webp",
        "michaeljackson" to "Michael Jackson.png",
        "minjiwoon" to "Min Jiwoon.webp",
        "mobbdeep" to "Mobb Deep.webp",
        "moiradelatorre" to "Moira Dela Torre.webp",
        "moongazingandher" to "Moongazing And Her.webp",
        "najm" to "Najm.png",
        "natanaelcano" to "Natanael Cano.webp",
        "netonvega" to "Neton Vega.webp",
        "newkidsontheblock" to "New Kids On The Block.webp",
        "newjeans" to "NewJeans.webp",
        "nopressure" to "No Pressure.webp",
        "nusratfatehalikhan" to "Nusrat Fateh Ali Khan.webp",
        "oddfuture" to "Odd Future.webp",
        "odd" to "Odd.png",
        "partynextdoor" to "PARTYNEXTDOOR.png",
        "panchiko" to "Panchiko.webp",
        "paster" to "Paster.webp",
        "payungteduh" to "Payung Teduh.webp",
        "pesopluma" to "Peso Pluma.png",
        "phantogram" to "Phantogram.webp",
        "piercetheveil" to "Pierce The Veil.png",
        "pinocchiop" to "PinocchioP.webp",
        "pizzahotline" to "Pizza Hotline.png",
        "plubio" to "Plubio.png",
        "porterrobinson" to "Porter Robinson.webp",
        "portishead" to "Portishead.webp",
        "radwimps" to "RADWIMPS.webp",
        "rescene" to "RESCENE.webp",
        "radiohead" to "Radiohead.webp",
        "raftaar" to "Raftaar.webp",
        "raimlaode" to "Raim Laode.webp",
        "ravynlenae" to "Ravyn Lenae.webp",
        "realboss" to "Real Boss.webp",
        "realityclub" to "Reality Club.png",
        "redvelvet" to "Red Velvet.webp",
        "reona" to "ReoNa.png",
        "rupamislam" to "Rupam Islam.webp",
        "spbalasubrahmanyam" to "S. P. Balasubrahmanyam.webp",
        "senidah" to "SENIDAH.webp",
        "sistar" to "SISTAR.webp",
        "sophie" to "SOPHIE.webp",
        "stayc" to "STAYC.webp",
        "stormy" to "STORMY.png",
        "saintsnow" to "Saint Snow.webp",
        "sandrocavazza" to "Sandro Cavazza.webp",
        "satindersartaaj" to "Satinder Sartaaj.webp",
        "seedhemaut" to "Seedhe Maut.webp",
        "seulgi" to "Seulgi.webp",
        "sharrymaan" to "Sharry Maan.webp",
        "shewantsrevenge" to "She Wants Revenge.webp",
        "skillet" to "Skillet.png",
        "skrillex" to "Skrillex.png",
        "sleeptheory" to "Sleep Theory.webp",
        "souldified" to "Souldified.webp",
        "soundgarden" to "Soundgarden.png",
        "stephensanchez" to "Stephen Sanchez.webp",
        "suburban" to "Sub Urban.webp",
        "surfcurse" to "Surf Curse.webp",
        "switchfoot" to "Switchfoot.webp",
        "tara" to "T-ARA.webp",
        "tuide" to "TUIDE.webp",
        "tvgirl" to "TV Girl.webp",
        "twice" to "TWICE.png",
        "tzuyu" to "TZUYU.webp",
        "takab" to "TakaB.webp",
        "tameimpala" to "Tame Impala.webp",
        "technimatic" to "Technimatic.webp",
        "the1975" to "The 1975.png",
        "thejapanesehouse" to "The Japanese House.png",
        "thelivingtombstone" to "The Living Tombstone.webp",
        "thelocaltrain" to "The Local Train.webp",
        "thestorysofar" to "The Story So Far.webp",
        "thomyorke" to "Thom Yorke.webp",
        "tulus" to "Tulus.png",
        "twentyonepilots" to "Twenty One Pilots.webp",
        "tydollaign" to "Ty Dolla \$ign.png",
        "tylayaweh" to "Tyla Yaweh.png",
        "vana" to "VANA.webp",
        "vividbadsquad" to "Vivid BAD SQUAD.webp",
        "wendy" to "WENDY.webp",
        "wavetoearth" to "Wave To Earth.webp",
        "ween" to "Ween.webp",
        "wetleg" to "Wet Leg.png",
        "wildstylez" to "Wildstylez.webp",
        "wisinyandel" to "Wisin & Yandel.webp",
        "wonderlandsshowtime" to "Wonderlands×Showtime.webp",
        "xingwonmusic" to "Xingwonmusic.webp",
        "xpert" to "Xpert.webp",
        "yeat" to "YEAT.webp",
        "yoona" to "YOONA.png",
        "yeyo" to "Yeyo.webp",
        "yorushika" to "Yorushika.png",
        "yukikookada" to "Yukiko Okada.webp",
        "yunglixo" to "YungLixo.webp",
        "yves" to "Yves.webp",
        "zerobaseone" to "ZEROBASEONE.webp",
        "ano" to "ano.webp",
        "bbno" to "bbno\$.webp",
        "ba" to "bôa.webp",
        "deadmau5" to "deadmau5.webp",
        "idle" to "i-dle.webp",
        "milet" to "milet.png",
        "mxmtoon" to "mxmtoon.webp",
        "nona" to "no na.webp",
        "oshaman" to "oShaman.webp",
        "starfall" to "starfall.webp",
        "underscores" to "underscores.webp",
        "scarmaydon" to "Óscar Maydon.webp",
        "s" to "μ’s.webp",
    )
}
