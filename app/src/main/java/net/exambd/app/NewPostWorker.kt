package net.exambd.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Periodically checks ExamBD's RSS feed. If the newest post differs from the
 * last one we saw, a notification is shown to the user.
 */
class NewPostWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val latest = fetchLatestPost() ?: return@withContext Result.success()
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSeenLink = prefs.getString(KEY_LAST_LINK, null)

            if (lastSeenLink == null) {
                // First run: just remember current latest post, don't spam a notification.
                prefs.edit().putString(KEY_LAST_LINK, latest.link).apply()
            } else if (lastSeenLink != latest.link) {
                prefs.edit().putString(KEY_LAST_LINK, latest.link).apply()
                NotificationHelper.showNewPostNotification(applicationContext, latest.title, latest.link)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private data class Post(val title: String, val link: String)

    private fun fetchLatestPost(): Post? {
        val url = URL(FEED_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "ExamBD-Android-App")
        connection.inputStream.use { stream ->
            val parser: XmlPullParser = android.util.Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var inItem = false
            var title: String? = null
            var link: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> inItem = true
                            "title" -> if (inItem && title == null) title = parser.nextText()
                            "link" -> if (inItem && link == null) link = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            // First <item> in the RSS feed = latest post
                            if (title != null && link != null) {
                                return Post(title.trim(), link.trim())
                            }
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return null
    }

    companion object {
        private const val FEED_URL = "https://exambd.net/feed/"
        private const val PREFS_NAME = "exambd_prefs"
        private const val KEY_LAST_LINK = "last_post_link"
        private const val WORK_NAME = "exambd_new_post_check"

        /** Call once (e.g. from MainActivity.onCreate) to schedule periodic checks. */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // 15 minutes is the minimum interval Android's WorkManager allows
            // for periodic work; battery-friendly while still fairly prompt.
            val request = PeriodicWorkRequestBuilder<NewPostWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
