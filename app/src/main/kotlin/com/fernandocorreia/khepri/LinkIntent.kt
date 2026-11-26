package com.fernandocorreia.khepri

import android.content.Intent
import android.net.Uri

/** Which intents carry a link, and what is left of one once it is used. */
internal object LinkIntent {
    /** The link of a VIEW intent, unless Android replays it from the recents list. */
    fun link(intent: Intent): String? {
        val replayed = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        return intent.dataString.takeIf { intent.action == Intent.ACTION_VIEW && !replayed }
    }

    /**
     * Uses the link in [intent] once: [open] takes it, or a browser gets an own web link [open] does not know
     * ([start]). Returns the intent to keep, without the link.
     */
    fun consume(intent: Intent, open: (String) -> Boolean, start: (Intent) -> Unit): Intent {
        val link = link(intent) ?: return intent
        if (!open(link)) browser(intent)?.let(start)
        return consumed(intent)
    }

    /** The intent without its link, so neither `getIntent()` nor a later recreation sees it again. */
    fun consumed(intent: Intent): Intent = Intent(intent).setAction(Intent.ACTION_MAIN).setData(null)

    /**
     * A web link the app does not handle, aimed at a browser: a plain VIEW intent would come back to this activity once
     * the `kheprios.com` filter is verified. The selector, which Android resolves in place of the intent, is a web link
     * with no host: browsers take any https link, while this app's filter names its host. Only this app's own web links
     * are forwarded: the activity is exported, and another app must not use it to open arbitrary sites. Custom-scheme
     * links have nowhere else to go.
     */
    fun browser(intent: Intent): Intent? {
        val uri = intent.data?.takeIf { it.scheme == "https" && it.host == WEB_HOST } ?: return null
        return Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).apply {
            selector =
                Intent(Intent.ACTION_VIEW, Uri.Builder().scheme("https").build()).addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    private const val WEB_HOST = "kheprios.com"
}
