package com.vestor.animalfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vestor.animalfinder.domain.model.PetListing
import com.vestor.animalfinder.ui.theme.AnimalFinderTheme
import io.github.jan.supabase.postgrest.from

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AnimalFinderApplication

        setContent {
            AnimalFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PetListScreen(app.supabase)
                }
            }
        }
    }
}

@Composable
fun PetListScreen(supabase: io.github.jan.supabase.SupabaseClient) {
    var petList by remember { mutableStateOf<List<PetListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val result: List<PetListing> = supabase
                .from("pet_listings")
                .select()
                .decodeList()

            petList = result
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Animal Finder",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text("Ошибка: $error", color = MaterialTheme.colorScheme.error)
            }
            petList.isEmpty() -> {
                Text("Пока нет объявлений о пропавших/найденных животных")
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(petList) { pet ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(pet.petName, style = MaterialTheme.typography.titleLarge)
                                Text("${pet.species} • ${pet.listingType}")
                                pet.breed?.let { Text("Порода: $it") }
                                pet.location?.let { Text("Место: $it") }
                                pet.description?.let {
                                    if (it.isNotBlank()) Text(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}