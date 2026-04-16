package com.vestor.animalfinder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.vestor.animalfinder.domain.model.PetListing
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: String,
    supabase: io.github.jan.supabase.SupabaseClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as AnimalFinderApplication

    var pet by remember { mutableStateOf<PetListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Текущий пользователь
    var currentUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        currentUserId = app.authManager.getUserId().firstOrNull()
    }

    LaunchedEffect(petId) {
        try {
            isLoading = true
            val result: List<PetListing> = supabase
                .from("pet_listings")
                .select {
                    filter {
                        eq("id", petId)
                    }
                }
                .decodeList()

            pet = result.firstOrNull()
        } catch (e: Exception) {
            error = e.message ?: "Ошибка загрузки"
        } finally {
            isLoading = false
        }
    }

    // Проверка, может ли пользователь удалить это объявление
    val canDelete = pet?.userId != null && pet?.userId == currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали объявления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (canDelete && !isDeleting) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ошибка: $error", color = MaterialTheme.colorScheme.error)
                }
            }
            pet == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Объявление не найдено")
                }
            }
            else -> {
                PetDetailContent(pet!!, context)
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить объявление") },
            text = { Text("Вы уверены, что хотите удалить объявление о ${pet?.petName}? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            try {
                                supabase.from("pet_listings").delete {
                                    filter {
                                        eq("id", petId)
                                    }
                                }
                                onBack()
                            } catch (e: Exception) {
                                error = "Ошибка удаления: ${e.message}"
                            } finally {
                                isDeleting = false
                                showDeleteDialog = false
                            }
                        }
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PetDetailContent(pet: PetListing, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (pet.listingType == "lost")
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = if (pet.listingType == "lost") "🐾 ПРОПАЛ(А)" else "🐕 НАЙДЕН(А)",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(pet.petName, style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "${pet.species}${pet.breed?.let { " • $it" } ?: ""}${pet.age?.let { " • $it лет" } ?: ""}",
            style = MaterialTheme.typography.bodyLarge
        )

        Divider()

        if (!pet.color.isNullOrBlank() || !pet.gender.isNullOrBlank() || !pet.temperament.isNullOrBlank() || !pet.size.isNullOrBlank()) {
            Text("Характеристики:", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pet.color?.let { InfoRow("🎨 Окрас:", it) }
                pet.gender?.let { InfoRow("⚥ Пол:", if (it == "male") "Мальчик" else "Девочка") }
                pet.temperament?.let { InfoRow("😊 Характер:", it) }
                pet.size?.let { sizeValue ->
                    val sizeText = when(sizeValue) {
                        "small" -> "Маленький"
                        "medium" -> "Средний"
                        "large" -> "Большой"
                        else -> sizeValue
                    }
                    InfoRow("📏 Размер:", sizeText)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        pet.location?.let {
            InfoRow("📍 Место:", it)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!pet.createdAt.isNullOrBlank()) {
            val formattedDate = pet.createdAt.substringBefore("T").replace("-", ".")
            InfoRow("📅 Дата создания:", formattedDate)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!pet.description.isNullOrBlank()) {
            Text("Описание:", style = MaterialTheme.typography.titleMedium)
            Text(pet.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!pet.contact.isNullOrBlank() || !pet.contactPhone.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📞 КОНТАКТЫ ДЛЯ СВЯЗИ", style = MaterialTheme.typography.titleMedium)

                    pet.contactPhone?.let { phone ->
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$phone")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null)
                                Text("Позвонить: $phone")
                            }
                        }
                    }

                    pet.contact?.let { contact ->
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/${contact.replace("@", "")}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Text("Написать: $contact")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}