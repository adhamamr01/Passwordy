package com.adhamamr.passwordy

import android.app.Application
import com.adhamamr.passwordy.data.network.RetrofitInstance

/** Initialises the networking layer (which needs a context for the token store) once at startup. */
class PasswordyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.init(this)
    }
}
