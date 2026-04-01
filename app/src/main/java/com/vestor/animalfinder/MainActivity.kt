package com.vestor.animalfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
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
                    AnimalFinderNavHost(app)
                }
            }
        }
    }
}

@Composable
fun AnimalFinderNavHost(app: AnimalFinderApplication) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list"
    ) {
        composable("list") {
            PetListScreen(
                supabase = app.supabase,
                onPetClick = { pet ->
                    navController.navigate("detail/${pet.id}")
                },
                onAddClick = {
                    navController.navigate("add")
                }
            )
        }

        composable(
            route = "detail/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            PetDetailScreen(
                petId = petId,
                supabase = app.supabase,
                onBack = { navController.popBackStack() }
            )
        }

        composable("add") {
            AddPetScreen(
                supabase = app.supabase,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PetListScreen(
    supabase: io.github.jan.supabase.SupabaseClient,
    onPetClick: (PetListing) -> Unit,
    onAddClick: () -> Unit
) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Animal Finder",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(
                onClick = onAddClick
            ) {
                Text("➕")
            }
        }

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
                        PetCard(
                            pet = pet,
                            onClick = { onPetClick(pet) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PetCard(pet: PetListing, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!pet.photoUrl.isNullOrBlank()) {
                val painter = rememberAsyncImagePainter(
                    model = pet.photoUrl,
                    error = painterResource(id = android.R.drawable.ic_menu_gallery)
                )
                Image(
                    painter = painter,
                    contentDescription = pet.petName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

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