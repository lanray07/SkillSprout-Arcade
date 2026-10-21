package com.skillsprout.billing

/** Amazon sells subscription terms but returns their parent SKU on receipts. */
class AmazonProductCatalog private constructor(val purchases:Map<String,String>) {
    val purchaseSkus:Set<String> get()=purchases.keys
    val receiptSkus:Set<String> get()=purchases.values.toSet()
    val querySkus:Set<String> get()=purchaseSkus+receiptSkus
    fun mayPurchase(sku:String)=sku in purchaseSkus
    fun acceptsReceipt(sku:String)=sku in receiptSkus
    companion object {
        /** Comma-separated purchase=receipt pairs. A one-time entitlement maps to itself. */
        fun parse(config:String):AmazonProductCatalog {
            require(config.isNotBlank()) {"Explicit purchase-to-receipt SKU mappings are required"}
            val entries=config.split(',').map {entry->
                val parts=entry.split('=').map {it.trim()}
                require(parts.size==2 && parts.all {it.length in 1..200 && it.none {c->c.isWhitespace() || c.isISOControl() || c in ",="}}) {"Invalid SKU mapping"}
                parts[0] to parts[1]
            }
            require(entries.map {it.first}.distinct().size==entries.size) {"Duplicate purchase SKU"}
            val map=entries.toMap()
            // A subscription parent cannot also be offered as a buyable term.
            require(map.all {(purchase,receipt)->purchase==receipt || receipt !in map.keys}) {"Subscription parent cannot be purchased directly"}
            return AmazonProductCatalog(map)
        }
    }
}
