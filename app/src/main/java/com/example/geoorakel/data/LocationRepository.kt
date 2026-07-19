package com.example.geoorakel.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {

    suspend fun getAddressName(lat: Double, lon: Double): String =
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())
            val defaultName = "Koordinaten: $lat, $lon"

            fun buildName(address: Address): String {
                val thoroughfare = address.thoroughfare
                val subThoroughfare = address.subThoroughfare
                val subLocality = address.subLocality
                val locality = address.locality

                return when {
                    thoroughfare != null && subThoroughfare != null ->
                        "$thoroughfare $subThoroughfare, ${subLocality ?: ""}, ${locality ?: ""}".trimEnd(',', ' ')
                    thoroughfare != null ->
                        "$thoroughfare, ${subLocality ?: ""}, ${locality ?: ""}".trimEnd(',', ' ')
                    subLocality != null && locality != null ->
                        "$subLocality, $locality"
                    else ->
                        locality ?: subLocality ?: address.featureName ?: defaultName
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    val name = addresses.firstOrNull()?.let { buildName(it) } ?: defaultName
                    continuation.resume(name)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val name = addresses?.firstOrNull()?.let { buildName(it) } ?: defaultName
                continuation.resume(name)
            }
        }

    suspend fun getNearbyWikiContext(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 8000,
        limit: Int = 5
    ): String = withContext(Dispatchers.IO) {
        try {
            val language = wikipediaLanguage()
            val pageIds = fetchNearbyPageIds(language, lat, lon, radiusMeters, limit)
            if (pageIds.isEmpty()) return@withContext ""

            val extracts = fetchExtracts(language, pageIds)
            extracts.entries.joinToString(separator = "\n\n") { (title, extract) ->
                "„$title“: $extract"
            }
        } catch (e: Exception) {
            android.util.Log.w("LocationRepository", "Wikipedia-Anfrage fehlgeschlagen", e)
            ""
        }
    }

    private fun wikipediaLanguage(): String {
        val lang = Locale.getDefault().language
        return lang.ifBlank { "en" }
    }

    private fun fetchNearbyPageIds(
        language: String,
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        limit: Int
    ): List<Int> {
        val coord = "$lat|$lon"
        val urlString = "https://$language.wikipedia.org/w/api.php" +
                "?action=query&list=geosearch" +
                "&gscoord=${URLEncoder.encode(coord, "UTF-8")}" +
                "&gsradius=$radiusMeters" +
                "&gslimit=$limit" +
                "&format=json"

        val json = httpGetJson(urlString)
        val results = json.optJSONObject("query")?.optJSONArray("geosearch") ?: return emptyList()

        val ids = mutableListOf<Int>()
        for (i in 0 until results.length()) {
            ids.add(results.getJSONObject(i).getInt("pageid"))
        }
        return ids
    }

    private fun fetchExtracts(language: String, pageIds: List<Int>): Map<String, String> {
        val idsParam = pageIds.joinToString("|")
        val urlString = "https://$language.wikipedia.org/w/api.php" +
                "?action=query&prop=extracts" +
                "&exintro&explaintext" +
                "&exsentences=2" +
                "&pageids=$idsParam" +
                "&format=json"

        val json = httpGetJson(urlString)
        val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return emptyMap()

        val result = LinkedHashMap<String, String>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.getJSONObject(keys.next())
            val title = page.optString("title")
            val extract = page.optString("extract")
            if (title.isNotBlank() && extract.isNotBlank()) {
                result[title] = extract
            }
        }
        return result
    }

    private fun httpGetJson(urlString: String): JSONObject {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "GeoOrakel-App/1.0")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        connection.inputStream.bufferedReader().use { reader ->
            val text = reader.readText()
            connection.disconnect()
            return JSONObject(text)
        }
    }
}