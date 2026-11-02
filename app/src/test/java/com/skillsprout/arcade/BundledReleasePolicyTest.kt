package com.skillsprout.arcade

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class BundledReleasePolicyTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun exactBundledPackLoadsWithoutInventingApproval() {
        val bytes=context.assets.open("packs/starter.json").use {it.readBytes()}
        assertEquals(ContentPacks.BUNDLED_PUBLISHER_WAIVER_SHA256,ContentPacks.sha(bytes))
        val activities=ContentPacks(context).starter()
        assertEquals(444,activities.size)
        assertTrue(activities.all {it.reviewStatus=="Awaiting Teacher Review"})
    }
    @Test fun releasePublicParserAndUnsignedImportDoNotInheritBundledException() {
        org.junit.Assume.assumeFalse(BuildConfig.DEVELOPMENT_CONTENT)
        val bytes=context.assets.open("packs/starter.json").use {it.readBytes()}
        val packs=ContentPacks(context)
        assertThrows(IllegalArgumentException::class.java) {packs.parse(bytes)}
        assertThrows(IllegalArgumentException::class.java) {packs.import(bytes,ContentPacks.sha(bytes))}
        val root=org.json.JSONObject(bytes.toString(Charsets.UTF_8))
        val first=root.getJSONArray("activities").getJSONObject(0).put("reviewStatus","Approved")
        root.put("activities",org.json.JSONArray().put(first))
        assertEquals(1,packs.parse(root.toString().toByteArray()).size)
        first.put("reviewStatus","Rejected")
        assertThrows(IllegalArgumentException::class.java) {packs.parse(root.toString().toByteArray())}
    }
}
