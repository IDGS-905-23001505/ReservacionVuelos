package com.example.reservacionvuelos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.reservacionvuelos.ui.theme.*


data class Vuelo(
    val id: Int = 0, val aerolinea: String, val origen: String,
    val destino: String, val fecha: String, val nombrePasajero: String,
    val telefono: String, val clase: String, val precio: Double
)

interface VueloApiService {
    @GET("/hoteles") suspend fun getVuelos(): List<Vuelo>
    @POST("/hoteles") suspend fun agregar(@Body v: Vuelo): Vuelo
    @PUT("/hoteles/{id}") suspend fun actualizar(@Path("id") id: Int, @Body v: Vuelo): Vuelo
    @DELETE("vuelos/{id}")
    suspend fun eliminar(@Path("id") id: Int): retrofit2.Response<Unit>
}

object RetrofitClient {
    val instance: VueloApiService = Retrofit.Builder()
        .baseUrl("http://192.168.100.102:5000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(VueloApiService::class.java)
}

class VueloViewModel : ViewModel() {
    var listaVuelos = mutableStateListOf<Vuelo>()
    var vueloEnEdicion by mutableStateOf<Vuelo?>(null)
    init { refresh() }
    fun refresh() = viewModelScope.launch { listaVuelos.clear(); listaVuelos.addAll(RetrofitClient.instance.getVuelos()) }
    fun guardar(v: Vuelo) = viewModelScope.launch {
        if (v.id == 0) RetrofitClient.instance.agregar(v) else RetrofitClient.instance.actualizar(v.id, v)
        refresh()
    }
    fun eliminar(v: Vuelo) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.eliminar(v.id)

                if (response.isSuccessful) {
                    refresh()
                } else {
                    android.util.Log.e("API_ERROR", "Error al eliminar: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Excepción: ${e.message}")
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val nav = rememberNavController()
            val vm: VueloViewModel = viewModel()
            NavHost(nav, "lista") {
                composable("lista") { PantallaLista(vm, nav) }
                composable("formulario") { PantallaFormulario(vm, nav) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLista(vm: VueloViewModel, nav: androidx.navigation.NavHostController) {
    var mostrarAlerta by remember { mutableStateOf(false) }
    var vueloAEliminar by remember { mutableStateOf<Vuelo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservaciones de vuelos", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HotelPrimary)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Button(
                onClick = { vm.vueloEnEdicion = null; nav.navigate("formulario") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HotelSecondary)
            ) {
                Text("Reservar Vuelo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(items = vm.listaVuelos, key = { it.id }) { v ->
                    Card(
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HotelCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(v.aerolinea, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = HotelTextMain)
                                IconButton(onClick = {
                                    vueloAEliminar = v
                                    mostrarAlerta = true
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = HotelErrorRed)
                                }
                            }
                            Row { Icon(Icons.Default.FlightLand, null, Modifier.size(16.dp), tint = HotelTextSub); Text(" Destino: ${v.destino}", color = HotelTextSub) }
                            Row { Icon(Icons.Default.DateRange, null, Modifier.size(16.dp), tint = HotelTextSub); Text(" Salida: ${v.fecha}", color = HotelTextSub) }
                            Text("Pasajero: ${v.nombrePasajero} (${v.telefono}) • ${v.clase}", color = HotelPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("$${String.format("%,.2f", v.precio)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HotelPrimary)
                                FloatingActionButton(
                                    onClick = { vm.vueloEnEdicion = v; nav.navigate("formulario") },
                                    containerColor = HotelSecondary,
                                    modifier = Modifier.size(48.dp)
                                ) { Icon(Icons.Default.Edit, null, tint = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarAlerta) {
        AlertDialog(
            onDismissRequest = { mostrarAlerta = false },
            title = { Text("Eliminar Reservación") },
            text = { Text("¿Está seguro de que desea eliminar esta reservación?") },
            confirmButton = {
                TextButton(onClick = {
                    val v = vueloAEliminar
                    mostrarAlerta = false
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (v != null) vm.eliminar(v)
                    }, 150)
                }) {
                    Text("Eliminar", color = HotelErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlerta = false }) { Text("Cancelar") }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(vm: VueloViewModel, nav: androidx.navigation.NavHostController) {
    val edit = vm.vueloEnEdicion

    var aerolinea by remember { mutableStateOf(edit?.aerolinea ?: "Aeroméxico") }
    var nombrePasajero by remember { mutableStateOf(edit?.nombrePasajero ?: "") }
    var origen by remember { mutableStateOf(edit?.origen ?: "") }
    var destino by remember { mutableStateOf(edit?.destino ?: "") }
    var telefono by remember { mutableStateOf(edit?.telefono ?: "") }
    var fecha by remember { mutableStateOf(edit?.fecha ?: "") }
    var precio by remember { mutableStateOf(edit?.precio?.toString() ?: "") }
    var clase by remember { mutableStateOf(edit?.clase ?: "Económica") }

    var expAerolinea by remember { mutableStateOf(false) }
    var expClase by remember { mutableStateOf(false) }

    val listaAerolineas = listOf("Aeroméxico", "Volaris", "VivaAerobus", "American Airlines", "Delta", "United")
    val listaClases = listOf("Económica", "Turista Premium", "Business", "Primera Clase")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Vuelo", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HotelPrimary)
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Box(Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                OutlinedTextField(
                    value = aerolinea,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Aerolínea") },
                    leadingIcon = { Icon(Icons.Default.Flight, null) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { expAerolinea = true })
                DropdownMenu(expanded = expAerolinea, onDismissRequest = { expAerolinea = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    listaAerolineas.forEach { op ->
                        DropdownMenuItem(text = { Text(op) }, onClick = { aerolinea = op; expAerolinea = false })
                    }
                }
            }

            OutlinedTextField(nombrePasajero, { if (it.all { c -> c.isLetter() || c.isWhitespace() }) nombrePasajero = it }, label = { Text("Nombre del Pasajero") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            OutlinedTextField(origen, { if (it.all { c -> c.isLetter() || c.isWhitespace() }) origen = it }, label = { Text("Origen") }, leadingIcon = { Icon(Icons.Default.FlightTakeoff, null) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(destino, { if (it.all { c -> c.isLetter() || c.isWhitespace() }) destino = it }, label = { Text("Destino") }, leadingIcon = { Icon(Icons.Default.FlightLand, null) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(telefono, { if (it.all { c -> c.isDigit() }) telefono = it }, label = { Text("Número Telefónico") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha Vuelo") }, leadingIcon = { Icon(Icons.Default.DateRange, null) }, modifier = Modifier.fillMaxWidth())

            Box(Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                OutlinedTextField(
                    value = clase,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Clase de Asiento") },
                    leadingIcon = { Icon(Icons.Default.Chair, null) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { expClase = true })
                DropdownMenu(expanded = expClase, onDismissRequest = { expClase = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    listaClases.forEach { op ->
                        DropdownMenuItem(text = { Text(op) }, onClick = { clase = op; expClase = false })
                    }
                }
            }

            OutlinedTextField(precio, { if (it.all { c -> c.isDigit() || c == '.' }) precio = it }, label = { Text("Precio del Boleto") }, leadingIcon = { Icon(Icons.Default.AttachMoney, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    vm.guardar(Vuelo(id = edit?.id ?: 0, aerolinea = aerolinea, nombrePasajero = nombrePasajero, origen = origen, destino = destino, fecha = fecha, telefono = telefono, clase = clase, precio = precio.toDoubleOrNull() ?: 0.0))
                    nav.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HotelSecondary)
            ) {
                Text("Confirmar Vuelo", color = Color.White)
            }
        }
    }
}