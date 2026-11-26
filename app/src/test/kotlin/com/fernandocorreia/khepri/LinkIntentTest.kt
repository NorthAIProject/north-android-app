package com.fernandocorreia.khepri

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkIntentTest {
    private fun view(link: String) = Intent(Intent.ACTION_VIEW, Uri.parse(link))

    @Test
    fun `a view intent carries its link, a replay from recents and a launch do not`() {
        assertEquals("khepri://more", LinkIntent.link(view("khepri://more")))
        assertNull(LinkIntent.link(view("khepri://more").addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)))
        assertNull(LinkIntent.link(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `a used intent has no link left`() {
        assertNull(LinkIntent.link(LinkIntent.consumed(view("khepri://more"))))
    }

    @Test
    fun `only an own web link goes to a browser, never back to the app`() {
        val browser = LinkIntent.browser(view("https://kheprios.com/app/unknown"))
        assertEquals("https", browser?.selector?.data?.scheme)
        assertNull(browser?.selector?.data?.host)
        assertEquals("https://kheprios.com/app/unknown", browser?.dataString)
        assertNull(LinkIntent.browser(view("khepri://nowhere")))
        assertNull(LinkIntent.browser(view("https://evil.example/app")))
    }

    @Test
    fun `a link is used once, by the navigator or a browser, and dropped either way`() {
        val opened = mutableListOf<String>()
        val started = mutableListOf<Intent>()
        fun consume(link: String, known: Boolean) =
            LinkIntent.consume(
                view(link),
                {
                    opened += it
                    known
                },
                { started += it },
            )

        assertNull(consume("khepri://more", known = true).data)
        assertEquals(listOf("khepri://more"), opened)
        assertEquals(emptyList<Intent>(), started)

        assertNull(consume("https://kheprios.com/i/short", known = false).data)
        assertEquals("https://kheprios.com/i/short", started.single().dataString)

        consume("https://evil.example/i/short", known = false)
        assertEquals(1, started.size)

        val launch = Intent(Intent.ACTION_MAIN)
        assertEquals(launch, LinkIntent.consume(launch, { error("no link") }, { error("no link") }))
    }
}
