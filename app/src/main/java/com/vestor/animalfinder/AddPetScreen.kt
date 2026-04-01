package com.vestor.animalfinder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

@Serializable
data class NewPetListing(
    val id: String,
    @SerialName("listing_type")
    val listingType: String,
    @SerialName("pet_name")
    val petName: String,
    val species: String,
    val breed: String? = null,
    val color: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val location: String? = null,
    val description: String? = null,
    val temperament: String? = null,
    val contact: String? = null,
    @SerialName("contact_phone")
    val contactPhone: String? = null,
    @SerialName("user_id")
    val userId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    supabase: io.github.jan.supabase.SupabaseClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var listingType by remember { mutableStateOf("lost") }
    var petName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var temperament by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // TODO: сохранить bitmap в файл и загрузить в Supabase Storage
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать объявление") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Новое объявление",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 24.sp
            )

            Text("Тип объявления:", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = listingType == "lost",
                    onClick = { listingType = "lost" },
                    label = { Text("🐾 Пропал(а)") }
                )
                FilterChip(
                    selected = listingType == "found",
                    onClick = { listingType = "found" },
                    label = { Text("🐕 Найден(а)") }
                )
            }

            Text("Фото животного:", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Галерея")
                    }
                }
                OutlinedButton(onClick = { cameraLauncher.launch(null) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Камера")
                    }
                }
            }

            if (selectedImageUri != null) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(selectedImageUri)
                        .crossfade(true)
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = "Выбранное фото",
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Divider()

            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it },
                label = { Text("Кличка *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Вид (собака/кошка/другое) *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                label = { Text("Порода") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Окрас") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Возраст (лет)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = if (gender == "male") "Мальчик" else if (gender == "female") "Девочка" else "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Пол") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.weight(1f).menuAnchor()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Мальчик") }, onClick = { gender = "male"; expanded = false })
                        DropdownMenuItem(text = { Text("Девочка") }, onClick = { gender = "female"; expanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Место пропажи/нахождения") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = temperament,
                onValueChange = { temperament = it },
                label = { Text("Характер") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Контактные данные:", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Telegram / WhatsApp") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("@username или 123456789") }
            )

            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Телефон для звонка") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("+7 (999) 123-45-67") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (petName.isBlank() || species.isBlank()) {
                        errorMessage = "Заполните кличку и вид"
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        try {
                            val newPet = NewPetListing(
                                id = UUID.randomUUID().toString(),
                                listingType = listingType,
                                petName = petName,
                                species = species,
                                breed = breed.ifBlank { null },
                                color = color.ifBlank { null },
                                age = age.toIntOrNull(),
                                gender = gender.ifBlank { null },
                                location = location.ifBlank { null },
                                description = description.ifBlank { null },
                                temperament = temperament.ifBlank { null },
                                contact = contact.ifBlank { null },
                                contactPhone = contactPhone.ifBlank { null },
                                userId = null
                            )

                            supabase.from("pet_listings").insert(newPet)

                            onBack()
                        } catch (e: Exception) {
                            errorMessage = "Ошибка: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Сохранение..." else "✅ Опубликовать объявление")
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}