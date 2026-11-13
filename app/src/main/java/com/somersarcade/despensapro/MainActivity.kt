package com.somersarcade.despensapro

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

data class PantryProduct(val name: String, val amount: String, val unit: String, val price: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PantryApp(this) } }
    }
}

@Composable
fun PantryApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("despensa_local", Context.MODE_PRIVATE) }
    var products by remember { mutableStateOf(loadProducts(prefs)) }
    var budget by remember { mutableStateOf(prefs.getString("budget", "1500") ?: "1500") }
    var showAdd by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val friday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
    val nextFriday = friday.plusDays(7)

    fun save() {
        val arr = JSONArray()
        products.forEach { p -> arr.put(JSONObject().put("name", p.name).put("amount", p.amount).put("unit", p.unit).put("price", p.price)) }
        prefs.edit().putString("products", arr.toString()).apply()
    }

    val total = products.sumOf { it.price }
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Mi Despensa Pro", style = MaterialTheme.typography.headlineMedium)
                Text("Semana: $friday al $nextFriday")
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Presupuesto semanal", style = MaterialTheme.typography.titleMedium)
                        Text("$${"%.2f".format(budget.toDoubleOrNull() ?: 0.0)}")
                        Text("Valor registrado: $${"%.2f".format(total)}")
                        Text("Disponible: $${"%.2f".format((budget.toDoubleOrNull() ?: 0.0) - total)}")
                        OutlinedButton(onClick = { showBudget = true }) { Text("Cambiar presupuesto") }
                    }
                }
            }
            item { Text("Productos en tu despensa", style = MaterialTheme.typography.titleLarge) }
            if (products.isEmpty()) item { Text("Todavía no agregas productos. Pulsa + para comenzar.") }
            items(products) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text("${p.amount} ${p.unit} · $${"%.2f".format(p.price)}")
                        }
                        TextButton(onClick = { products = products.filterNot { it == p }; save() }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("1") }
        var unit by remember { mutableStateOf("pieza(s)") }
        var price by remember { mutableStateOf("0") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Agregar producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Producto") }, singleLine = true)
                    OutlinedTextField(amount, { amount = it }, label = { Text("Cantidad") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("pieza(s)", "g", "kg").forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                        }
                    }
                    OutlinedTextField(price, { price = it }, label = { Text("Valor estimado ($)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        products = products + PantryProduct(name.trim(), amount, unit, price.toDoubleOrNull() ?: 0.0)
                        save()
                        showAdd = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancelar") } }
        )
    }
    if (showBudget) {
        var value by remember { mutableStateOf(budget) }
        AlertDialog(
            onDismissRequest = { showBudget = false },
            title = { Text("Presupuesto de viernes a viernes") },
            text = { OutlinedTextField(value, { value = it }, label = { Text("Presupuesto en pesos") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                budget = value
                prefs.edit().putString("budget", budget).apply()
                showBudget = false
            }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { showBudget = false }) { Text("Cancelar") } }
        )
    }
}

fun loadProducts(prefs: android.content.SharedPreferences): List<PantryProduct> {
    return try {
        val arr = JSONArray(prefs.getString("products", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PantryProduct(o.getString("name"), o.getString("amount"), o.getString("unit"), o.getDouble("price"))
        }
    } catch (_: Exception) { emptyList() }
}
