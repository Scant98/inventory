package com.sombetech.inventory.di

import android.content.Context
import com.sombetech.inventory.data.http.InventoryApiClient
import com.sombetech.inventory.data.local.LocalCache
import com.sombetech.inventory.data.local.NetworkMonitor
import com.sombetech.inventory.data.local.OfflineQueue
import com.sombetech.inventory.data.repository.InventoryRepositoryImpl
import com.sombetech.inventory.domain.repository.InventoryRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val networkMonitor = NetworkMonitor(context)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    val apiClient: InventoryApiClient = InventoryApiClient(okHttpClient)

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepositoryImpl(
            api           = apiClient,
            okHttpClient  = okHttpClient,
            cache         = LocalCache(context),
            offlineQueue  = OfflineQueue(context),
            networkMonitor = networkMonitor,
        )
    }
}
