package com.example

import android.app.Application
import android.util.Log
import com.example.data.FirebaseConnectionManager
import com.google.firebase.FirebaseApp

class RcosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // Explicitly initialize Firebase API at application launch
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("RcosApplication", "FirebaseApp initialized in Application.onCreate()")
            }
            // Initialize central Firebase Connection Manager
            FirebaseConnectionManager.getInstance(this)
            Log.d("RcosApplication", "FirebaseConnectionManager initialized successfully.")
        } catch (e: Throwable) {
            Log.w("RcosApplication", "Firebase initialization fallback: ${e.message}")
        }
    }
}
