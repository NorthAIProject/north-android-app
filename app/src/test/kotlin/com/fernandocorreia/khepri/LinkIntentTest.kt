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
    fun `an unknown web link goes to a browser, never back to the app, and a custom scheme goes nowhere`() {
        val browser = LinkIntent.browser(view("https://kheprios.com/app/unknown"))
        assertEquals("https", browser?.selector?.data?.scheme)
        assertNull(browser?.selector?.data?.host)
        assertEquals("https://kheprios.com/app/unknown", browser?.dataString)
        assertNull(LinkIntent.browser(view("khepri://nowhere")))
    }
}
