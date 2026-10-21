package com.skillsprout.billing
import org.junit.Assert.*
import org.junit.Test

class AmazonProductCatalogTest {
    @Test fun termsAreBuyableAndParentIsTheReceiptIdentity() {
        val catalog=AmazonProductCatalog.parse("family.monthly=family,family.annual=family")
        assertEquals(setOf("family.monthly","family.annual","family"),catalog.querySkus)
        assertTrue(catalog.mayPurchase("family.monthly"));assertFalse(catalog.mayPurchase("family"))
        assertTrue(catalog.acceptsReceipt("family"));assertFalse(catalog.acceptsReceipt("family.monthly"))
    }
    @Test fun oneTimeEntitlementUsesTheSameIdentity() {
        val catalog=AmazonProductCatalog.parse("lifetime=lifetime")
        assertTrue(catalog.mayPurchase("lifetime"));assertTrue(catalog.acceptsReceipt("lifetime"))
    }
    @Test fun invalidAndAmbiguousConfigurationsFailClosed() {
        listOf("","monthly","monthly=parent,monthly=other","monthly=parent,parent=parent","monthly= ","monthly=parent=extra","monthly=bad sku","monthly=parent,").forEach {value->
            assertTrue("Accepted invalid mapping: $value",runCatching {AmazonProductCatalog.parse(value)}.isFailure)
        }
    }
    @Test fun receiptOnlyParentCanNeverBecomeAnOffer() {
        val catalog=AmazonProductCatalog.parse(" monthly = family , annual = family ")
        val responseSkus=listOf("family","monthly","annual","unrelated")
        assertEquals(listOf("monthly","annual"),responseSkus.filter(catalog::mayPurchase))
        assertEquals(listOf("family"),responseSkus.filter(catalog::acceptsReceipt))
    }
}
