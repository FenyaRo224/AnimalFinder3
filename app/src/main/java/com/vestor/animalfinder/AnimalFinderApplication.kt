package com.vestor.animalfinder

import android.app.Application
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth

class AnimalFinderApplication : Application() {

    lateinit var supabase: io.github.jan.supabase.SupabaseClient
    lateinit var authManager: AuthManager

    override fun onCreate() {
        super.onCreate()

        authManager = AuthManager(this)

        supabase = createSupabaseClient(
            supabaseUrl = "https://htusuxsjxxsudzxwjnvt.supabase.co",
            supabaseKey = "sb_publishable_xOG8hHT-MQPfiqZrNPeTXQ_ZKto_Isa"
        ) {
            install(Postgrest)
            install(Storage)
            install(Auth)
        }
    }
}