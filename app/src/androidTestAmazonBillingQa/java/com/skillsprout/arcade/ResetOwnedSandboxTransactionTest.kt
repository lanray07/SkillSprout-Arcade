package com.skillsprout.arcade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Deletes only this disposable QA package's App Tester subscription, never global transactions. */
@RunWith(AndroidJUnit4::class)
class ResetOwnedSandboxTransactionTest {
    @Test fun resetOnlyOwnedSubscription() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val owned="com.skillsprout.arcade.billingqa"
        val tester="com.amazon.sdktestclient"
        check(BuildConfig.DEBUG && instrument.targetContext.packageName==owned)
        val device=UiDevice.getInstance(instrument)
        val idle=Configurator.getInstance().waitForIdleTimeout
        Configurator.getInstance().waitForIdleTimeout=0
        try {
            device.wakeUp()
            device.executeShellCommand("am start -W -n $tester/.MainActivity")
            device.wait(Until.hasObject(By.pkg(tester)),10000)
            device.findObject(By.text("Appstore SDK API's"))?.click()
            val manage=By.text("8. Manage Transactions")
            assertTrue(device.wait(Until.hasObject(manage),10000))
            device.findObject(manage).click()
            device.swipe(device.displayWidth*9/10,device.displayHeight*4/5,device.displayWidth*9/10,device.displayHeight/4,30)
            Thread.sleep(500)
            val owner=By.res("$tester:id/package_name").text(owned)
            assertTrue("Owned sandbox transaction must be visible",device.wait(Until.hasObject(owner),10000))
            assertEquals(1,device.findObjects(owner).size)
            var row=device.findObject(owner).parent
            while(row.findObjects(By.res("$tester:id/delete_button")).isEmpty()) row=row.parent
            assertEquals(listOf("com.skillsprout.arcade.family"),row.findObjects(By.res("$tester:id/sku")).map {it.text})
            val delete=row.findObjects(By.res("$tester:id/delete_button")).single()
            assertEquals(tester,device.currentPackageName)
            delete.click()
            assertTrue("Only the owned row must disappear",device.wait(Until.gone(owner),10000))
        } finally {Configurator.getInstance().waitForIdleTimeout=idle}
    }
}
