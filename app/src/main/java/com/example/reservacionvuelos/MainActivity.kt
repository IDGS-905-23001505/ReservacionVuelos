package com.example.reservacionvuelos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.ConnectingAirports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.text.NumberFormat
import java.util.*

//MODELO DE DATOS
data class Hotel(
    val id: Int? = null,
    val nombre: String,
    val ubicacion: String,
    val precioPorNoche: Double,
    val estrellas: Int,
    val fecha: String?,
    val titular: String?,
    val huespedes: Int?,
    val tipoHabitacion: String?
)

// CAPA DE RED (RETROFIT)
interface HotelApiService {
    @GET("hoteles")
    suspend fun getHoteles(): Response<List<Hotel>>

    @POST("hoteles")
    suspend fun createHotel(@Body hotel: Hotel): Response<Hotel>

    @PUT("hoteles/{id}")
    suspend fun updateHotel(@Path("id") id: Int, @Body hotel: Hotel): Response<Hotel>

    @DELETE("hoteles/{id}")
    suspend fun deleteHotel(@Path("id") id: Int): Response<Unit>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.16.4.54:5000/"

    val apiService: HotelApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HotelApiService::class.java)
    }
}

// ESTADOS DE LA PANTALLA
sealed class HotelUiState {
    object Loading : HotelUiState()
    data class Success(val lista: List<Hotel>) : HotelUiState()
    data class Error(val mensaje: String) : HotelUiState()
}


// VIEWMODEL
class HotelViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<HotelUiState>(HotelUiState.Loading)
    val uiState: StateFlow<HotelUiState> = _uiState

    init { fetchHoteles() }

    fun fetchHoteles() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = HotelUiState.Loading
            try {
                val response = RetrofitClient.apiService.getHoteles()
                if (response.isSuccessful) {
                    _uiState.value = HotelUiState.Success(response.body() ?: emptyList())
                } else {
                    _uiState.value = HotelUiState.Error("Error del servidor: ${response.code()}")
                }
            } catch (e: IOException) {
                _uiState.value = HotelUiState.Error("El servidor Flask no responde.")
            } catch (e: Exception) {
                _uiState.value = HotelUiState.Error("Error al procesar los datos.")
            }
        }
    }

    fun agregarHotel(hotel: Hotel, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (RetrofitClient.apiService.createHotel(hotel).isSuccessful) {
                    fetchHoteles()
                    launch(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) { }
        }
    }

    fun actualizarHotel(id: Int, hotel: Hotel, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (RetrofitClient.apiService.updateHotel(id, hotel).isSuccessful) {
                    fetchHoteles()
                    launch(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) { }
        }
    }

    fun eliminarHotel(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (RetrofitClient.apiService.deleteHotel(id).isSuccessful) {
                    fetchHoteles()
                }
            } catch (e: Exception) { }
        }
    }
}


// CONFIGURACIÓN DE DISEÑO Y COLORES
object AppDesign {
    val Primary = Color(0xFFE94B77)
    val PrimaryVariant = Color(0xFFC7325E)
    val Secondary = Color(0xFF2D2D3A)
    val Background = Color(0xFFFDF8F9)
    val CardBackground = Color.White
    val StarColor = Color(0xFFFFB000)
    val TextMain = Color(0xFF222222)
    val TextSub = Color(0xFF7E7A7C)

    val PrimaryGradient = Brush.verticalGradient(
        colors = listOf(Primary, PrimaryVariant)
    )

    val CardShape = RoundedCornerShape(20.dp)
    val ButtonShape = RoundedCornerShape(14.dp)
    val InputShape = RoundedCornerShape(12.dp)
}

@Composable
fun HotelAppTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = AppDesign.Primary,
        secondary = AppDesign.Secondary,
        background = AppDesign.Background,
        surface = AppDesign.CardBackground,
        onPrimary = Color.White,
        onBackground = AppDesign.TextMain,
        onSurface = AppDesign.TextMain
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 0.5.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp),
            bodyMedium = TextStyle(color = AppDesign.TextSub, fontSize = 14.sp)
        ),
        content = content
    )
}


// FLUJO DE NAVEGACIÓN Y MAIN
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HotelAppTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val hotelViewModel: HotelViewModel = viewModel()
    var hotelAEditar by remember { mutableStateOf<Hotel?>(null) }

    NavHost(
        navController = navController,
        startDestination = "lista"
    ) {
        composable("lista") {
            PantallaLista(
                viewModel = hotelViewModel,
                onNavigateToAlta = {
                    hotelAEditar = null
                    navController.navigate("formulario")
                },
                onNavigateToEdicion = { hotel ->
                    hotelAEditar = hotel
                    navController.navigate("formulario")
                }
            )
        }
        composable("formulario") {
            PantallaFormulario(
                hotelEditando = hotelAEditar,
                onBack = {
                    hotelAEditar = null
                    navController.popBackStack()
                },
                onSave = { hotel ->
                    if (hotel.id == null) {
                        hotelViewModel.agregarHotel(hotel) { navController.popBackStack() }
                    } else {
                        hotelViewModel.actualizarHotel(hotel.id, hotel) {
                            hotelAEditar = null
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}


// COMPONENTES Y VISTAS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledTopBar(title: String, actions: @Composable (RowScope.() -> Unit)? = null, navigationIcon: (@Composable () -> Unit)? = null) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(color = Color.White)) },
        navigationIcon = { navigationIcon?.invoke() },
        actions = { actions?.invoke(this) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier
            .background(AppDesign.PrimaryGradient)
            .statusBarsPadding()
    )
}

@Composable
fun PantallaLista(viewModel: HotelViewModel, onNavigateToAlta: () -> Unit, onNavigateToEdicion: (Hotel) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var hotelAEliminar by remember { mutableStateOf<Hotel?>(null) }
    var hotelACalificar by remember { mutableStateOf<Hotel?>(null) }
    val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    Scaffold(
        topBar = { StyledTopBar(title = "Vuelos SkyPremium") },
        containerColor = AppDesign.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Button(
                onClick = onNavigateToAlta,
                colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Secondary),
                shape = AppDesign.ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Reservar Vuelo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.06f))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val result = state) {
                    is HotelUiState.Loading -> {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppDesign.Primary)
                        }
                    }
                    is HotelUiState.Error -> {
                        ErrorComponent(mensaje = result.mensaje) { viewModel.fetchHoteles() }
                    }
                    is HotelUiState.Success -> {
                        if (result.lista.isEmpty()) {
                            EmptyComponent(onNavigateToAlta)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(result.lista, key = { it.id!! }) { hotel ->
                                    FlightCard(
                                        flight = hotel,
                                        currencyFormat = format,
                                        onModificar = { onNavigateToEdicion(hotel) },
                                        onDelete = { hotelAEliminar = hotel },
                                        onCalificar = { hotelACalificar = hotel }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    hotelACalificar?.let { flight ->
        var estrellasSeleccionadas by remember { mutableStateOf(flight.estrellas) }
        AlertDialog(
            onDismissRequest = { hotelACalificar = null },
            shape = AppDesign.CardShape,
            containerColor = AppDesign.CardBackground,
            title = { Text("Calificar Servicio", fontWeight = FontWeight.Bold, color = AppDesign.TextMain) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("¿Qué puntuación le das al servicio de '${flight.nombre}'?", color = AppDesign.TextSub)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= estrellasSeleccionadas) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null,
                                tint = AppDesign.StarColor,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { estrellasSeleccionadas = i }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.actualizarHotel(flight.id!!, flight.copy(estrellas = estrellasSeleccionadas)) {}
                        hotelACalificar = null
                    },
                    shape = AppDesign.ButtonShape
                ) { Text("Guardar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { hotelACalificar = null }, shape = AppDesign.ButtonShape) { Text("Cancelar") }
            }
        )
    }

    hotelAEliminar?.let { flight ->
        CustomAlertDialog(
            title = "Cancelar Vuelo",
            body = "¿Estás seguro de quitar el itinerario a '${flight.ubicacion}'?",
            confirmText = "Eliminar",
            onConfirm = {
                flight.id?.let { viewModel.eliminarHotel(it) }
                hotelAEliminar = null
            },
            onDismiss = { hotelAEliminar = null }
        )
    }
}

@Composable
fun FlightCard(flight: Hotel, currencyFormat: NumberFormat, onModificar: () -> Unit, onDelete: () -> Unit, onCalificar: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppDesign.CardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = AppDesign.CardBackground)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(flight.nombre, style = MaterialTheme.typography.titleMedium, color = AppDesign.TextMain)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FlightTakeoff, null, tint = AppDesign.Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Destino: ${flight.ubicacion}", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = AppDesign.TextSub, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salida: ${flight.fecha ?: "Por confirmar"}", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    val pasajero = flight.titular ?: "Desconocido"
                    val numPasajeros = flight.huespedes ?: 1
                    val clase = flight.tipoHabitacion ?: "Económica"

                    Text(
                        text = "Pasajero: $pasajero • $numPasajeros Tkt. ($clase)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = AppDesign.PrimaryVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Borrar", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = AppDesign.StarColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${flight.estrellas}.0", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppDesign.TextMain)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(currencyFormat.format(flight.precioPorNoche), style = MaterialTheme.typography.titleMedium, color = AppDesign.PrimaryVariant, fontSize = 18.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedIconButton(
                        onClick = onCalificar,
                        shape = AppDesign.ButtonShape,
                        border = BorderStroke(1.dp, AppDesign.StarColor),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Rounded.Star, "Calificar", tint = AppDesign.StarColor, modifier = Modifier.size(18.dp))
                    }

                    FilledIconButton(
                        onClick = onModificar,
                        shape = AppDesign.ButtonShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = AppDesign.Primary),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Edit, "Modificar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(hotelEditando: Hotel?, onBack: () -> Unit, onSave: (Hotel) -> Unit) {
    var aerolinea by remember { mutableStateOf(hotelEditando?.nombre ?: "") }
    var destino by remember { mutableStateOf(hotelEditando?.ubicacion ?: "") }
    var fecha by remember { mutableStateOf(hotelEditando?.fecha ?: "") }
    var precioBoleto by remember { mutableStateOf(hotelEditando?.precioPorNoche?.toString() ?: "") }

    var pasajeroPrincipal by remember { mutableStateOf(hotelEditando?.titular ?: "") }
    var asientos by remember { mutableStateOf(hotelEditando?.huespedes?.toString() ?: "1") }
    var claseAsiento by remember { mutableStateOf(hotelEditando?.tipoHabitacion ?: "Económica") }

    val estrellas = hotelEditando?.estrellas ?: 5
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppDesign.Background,
        topBar = {
            StyledTopBar(
                title = if (hotelEditando == null) "Nuevo Vuelo" else "Editar Itinerario",
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(if (hotelEditando == null) "Registrar Boleto" else "Modificar Ruta", style = MaterialTheme.typography.titleLarge, color = AppDesign.TextMain)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                item { StyledTextField(value = aerolinea, onValueChange = { aerolinea = it }, label = "Aerolínea (Ej: Aeroméxico)", icon = Icons.Default.AirplanemodeActive) }
                item { StyledTextField(value = destino, onValueChange = { destino = it }, label = "Destino (Ciudad, País o Estado)", icon = Icons.Rounded.ConnectingAirports) }
                item { StyledTextField(value = pasajeroPrincipal, onValueChange = { pasajeroPrincipal = it }, label = "Nombre del Pasajero", icon = Icons.Default.Person) }

                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1.5f)) { StyledTextField(value = fecha, onValueChange = { fecha = it }, label = "Fecha Vuelo (Día/Mes)", icon = Icons.Rounded.CalendarMonth) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) { StyledTextField(value = asientos, onValueChange = { asientos = it }, label = "Pasajeros", icon = Icons.Default.ConfirmationNumber, keyboardType = KeyboardType.Number) }
                    }
                }

                item { StyledTextField(value = claseAsiento, onValueChange = { claseAsiento = it }, label = "Clase (Económica, Business, Primera)", icon = Icons.Default.EventSeat) }
                item { StyledTextField(value = precioBoleto, onValueChange = { precioBoleto = it }, label = "Precio del Boleto", icon = Icons.Default.AttachMoney, keyboardType = KeyboardType.Number) }
            }

            AnimatedVisibility(visible = errorMsg != null) {
                Row(modifier = Modifier.fillMaxWidth().clip(AppDesign.InputShape).background(Color(0xFFFDECEA)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(errorMsg ?: "", color = Color.Red, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val pDouble = precioBoleto.toDoubleOrNull()
                    val aInt = asientos.toIntOrNull()

                    if (aerolinea.isBlank() || destino.isBlank() || fecha.isBlank() || pasajeroPrincipal.isBlank() || claseAsiento.isBlank()) {
                        errorMsg = "Completa la información del vuelo."
                    } else if (pDouble == null || pDouble <= 0) {
                        errorMsg = "Por favor, introduce un precio de boleto válido."
                    } else if (aInt == null || aInt <= 0) {
                        errorMsg = "Número de pasajeros inválido."
                    } else {
                        errorMsg = null
                        onSave(Hotel(
                            id = hotelEditando?.id,
                            nombre = aerolinea,
                            ubicacion = destino,
                            precioPorNoche = pDouble,
                            estrellas = estrellas,
                            fecha = fecha,
                            titular = pasajeroPrincipal,
                            huespedes = aInt,
                            tipoHabitacion = claseAsiento
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = AppDesign.ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppDesign.Primary)
            ) {
                Text("Confirmar Reservación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = AppDesign.Primary, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = AppDesign.InputShape,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = AppDesign.CardBackground,
            focusedContainerColor = AppDesign.CardBackground,
            unfocusedBorderColor = Color(0xFFE5E5E5),
            focusedBorderColor = AppDesign.Primary,
            unfocusedLabelColor = AppDesign.TextSub,
            focusedLabelColor = AppDesign.Primary
        )
    )
}

@Composable
fun ErrorComponent(mensaje: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Warning, null, tint = AppDesign.Primary, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(mensaje, color = AppDesign.TextMain, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, shape = AppDesign.ButtonShape) { Text("Reintentar") }
    }
}

@Composable
fun EmptyComponent(onAlta: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp).clickable { onAlta() }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AirplanemodeActive, null, tint = Color(0xFFEFEFEF), modifier = Modifier.size(90.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No hay itinerarios de vuelo agregados", fontWeight = FontWeight.Bold, color = AppDesign.TextMain)
    }
}

@Composable
fun CustomAlertDialog(title: String, body: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDesign.CardShape,
        containerColor = AppDesign.CardBackground,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, color = AppDesign.TextMain) },
        text = { Text(body, color = AppDesign.TextSub) },
        confirmButton = { Button(onClick = onConfirm, shape = AppDesign.ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text(confirmText) } },
        dismissButton = { OutlinedButton(onClick = onDismiss, shape = AppDesign.ButtonShape, border = BorderStroke(1.dp, Color(0xFFE5E5E5))) { Text("Cancelar", color = AppDesign.TextMain) } }
    )
}