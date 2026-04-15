package com.vestor.animalfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
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
    var allPetList by remember { mutableStateOf<List<PetListing>>(emptyList()) }
    var filteredPetList by remember { mutableStateOf<List<PetListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Состояния фильтров
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<String?>(null) } // lost, found, или null
    var selectedGender by remember { mutableStateOf<String?>(null) } // male, female, или null
    var selectedSize by remember { mutableStateOf<String?>(null) } // small, medium, large, или null

    // Загрузка данных
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val result: List<PetListing> = supabase
                .from("pet_listings")
                .select()
                .decodeList()

            allPetList = result
            filteredPetList = result
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Фильтрация при изменении поиска или фильтров
    LaunchedEffect(searchQuery, selectedType, selectedGender, selectedSize, allPetList) {
        var filtered = allPetList

        // Фильтр по поиску (кличка)
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.petName.contains(searchQuery, ignoreCase = true) ||
                        (it.breed?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // Фильтр по типу (lost/found)
        selectedType?.let { type ->
            filtered = filtered.filter { it.listingType == type }
        }

        // Фильтр по полу
        selectedGender?.let { gender ->
            filtered = filtered.filter { it.gender == gender }
        }

        // Фильтр по размеру
        selectedSize?.let { size ->
            filtered = filtered.filter { it.size == size }
        }

        filteredPetList = filtered
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Верхняя панель с заголовком и кнопкой добавления
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Animal Finder",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = onAddClick) {
                Text("➕")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Строка поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск по кличке или породе...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("✕")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопка фильтров и активные фильтры
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showFilters,
                onClick = { showFilters = !showFilters },
                label = { Text("🔽 Фильтры") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            // Счётчик результатов
            Text(
                text = "Найдено: ${filteredPetList.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Панель фильтров (показывается только если showFilters = true)
        if (showFilters) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Тип объявления
                    Text("Тип объявления:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == "lost",
                            onClick = { selectedType = if (selectedType == "lost") null else "lost" },
                            label = { Text("🐾 Пропал") }
                        )
                        FilterChip(
                            selected = selectedType == "found",
                            onClick = { selectedType = if (selectedType == "found") null else "found" },
                            label = { Text("🐕 Найден") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Пол
                    Text("Пол:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedGender == "male",
                            onClick = { selectedGender = if (selectedGender == "male") null else "male" },
                            label = { Text("♂ Мальчик") }
                        )
                        FilterChip(
                            selected = selectedGender == "female",
                            onClick = { selectedGender = if (selectedGender == "female") null else "female" },
                            label = { Text("♀ Девочка") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Размер
                    Text("Размер:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSize == "small",
                            onClick = { selectedSize = if (selectedSize == "small") null else "small" },
                            label = { Text("Маленький") }
                        )
                        FilterChip(
                            selected = selectedSize == "medium",
                            onClick = { selectedSize = if (selectedSize == "medium") null else "medium" },
                            label = { Text("Средний") }
                        )
                        FilterChip(
                            selected = selectedSize == "large",
                            onClick = { selectedSize = if (selectedSize == "large") null else "large" },
                            label = { Text("Большой") }
                        )
                    }

                    // Кнопка сброса фильтров
                    TextButton(
                        onClick = {
                            selectedType = null
                            selectedGender = null
                            selectedSize = null
                            searchQuery = ""
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Сбросить все фильтры")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Список объявлений
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
            filteredPetList.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😢 Ничего не найдено")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            selectedType = null
                            selectedGender = null
                            selectedSize = null
                            searchQuery = ""
                        }) {
                            Text("Сбросить фильтры")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredPetList) { pet ->
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
            Text("${pet.species} • ${if (pet.listingType == "lost") "Пропал" else "Найден"}")
            pet.breed?.let { Text("Порода: $it") }
            pet.location?.let { Text("Место: $it") }
            pet.description?.let {
                if (it.isNotBlank()) Text(it)
            }
        }
    }
}