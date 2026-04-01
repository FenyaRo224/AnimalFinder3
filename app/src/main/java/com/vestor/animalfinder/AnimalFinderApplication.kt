package com.vestor.animalfinder

import android.app.Application
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

class AnimalFinderApplication : Application() {

    lateinit var supabase: io.github.jan.supabase.SupabaseClient

    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = "https://htusuxsjxxsudzxwjnvt.supabase.co",
            supabaseKey = "sb_publishable_xOG8hHT-MQPfiqZrNPeTXQ_ZKto_Isa"
        ) {
            install(Postgrest)
            // Auth пока убираем, чтобы заработало
        }
    }
}