package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.data.db.VaultDatabase
import com.example.data.repository.VaultRepository
import com.example.data.storage.VaultStorageManager
import com.example.playback.PlayerManager
import com.example.security.SecurityManager

class MediaVaultApplication : Application(), Application.ActivityLifecycleCallbacks {

    lateinit var database: VaultDatabase
        private set
    lateinit var storageManager: VaultStorageManager
        private set
    lateinit var repository: VaultRepository
        private set
    lateinit var securityManager: SecurityManager
        private set
    lateinit var playerManager: PlayerManager
        private set

    private var activeActivities = 0

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = VaultDatabase.getInstance(this)
        storageManager = VaultStorageManager(this)
        repository = VaultRepository(this, database.mediaDao(), storageManager)
        securityManager = SecurityManager(this)
        playerManager = PlayerManager.getInstance(this)

        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activeActivities == 0) {
            securityManager.onAppForegrounded()
        }
        activeActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        activeActivities--
        if (activeActivities <= 0) {
            activeActivities = 0
            securityManager.onAppBackgrounded()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        lateinit var instance: MediaVaultApplication
            private set
    }
}
