package com.skillsprout.arcade

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable fun FamilyPurchases(state:PurchaseState,access:FamilyAccess,gateway:PurchaseGateway) {
    Heading(stringResource(R.string.family_premium),true)
    Text(stringResource(if(access.development) R.string.family_development else if(access.premium) R.string.family_verified else R.string.family_free))
    Text(stringResource(R.string.family_features))
    Text(stringResource(when(state.status) {
        PurchaseStatus.UNAVAILABLE->R.string.purchase_unavailable
        PurchaseStatus.LOADING->R.string.purchase_loading
        PurchaseStatus.READY->R.string.purchase_ready
        PurchaseStatus.VERIFIED->if(access.premium) R.string.purchase_verified else R.string.purchase_pending
        PurchaseStatus.PENDING->R.string.purchase_pending
    }))
    if(state.configured) {
        Text(stringResource(R.string.purchase_privacy))
        if(!access.premium) state.offers.forEach {offer->
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(offer.title)
                if(offer.subscriptionPeriod.isNotBlank()) Text(stringResource(R.string.purchase_period,offer.subscriptionPeriod))
                if(offer.description.isNotBlank()) Text(offer.description)
                Action(stringResource(R.string.purchase_offer,offer.price),enabled=!state.busy) {gateway.purchase(offer.sku)}
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Action(stringResource(R.string.purchase_restore),enabled=!state.busy) {gateway.restore()}
            Action(stringResource(R.string.purchase_refresh)) {gateway.resume()}
        }
    }
    Text(stringResource(R.string.family_expiry))
    Text(stringResource(R.string.purchase_terms))
}
