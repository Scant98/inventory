package com.sombetech.inventory

import android.app.Application
import com.sombetech.inventory.di.AppContainer

class InventoryApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
