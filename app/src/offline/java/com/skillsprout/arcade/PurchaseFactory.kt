package com.skillsprout.arcade
import android.content.Context
fun createPurchases(context:Context,parentSessionValid:()->Boolean):PurchaseGateway=UnavailablePurchases()
