package pe.enrutatuagua.app.ui.ruta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pe.enrutatuagua.app.ui.mapa.MapaRuta
import pe.enrutatuagua.domain.modelo.EstadoEntrega

@Composable
fun RutaScreen(onParadaClick: (Long) -> Unit, vm: RutaViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi ruta", style = MaterialTheme.typography.headlineSmall)
        val rc = estado.rutaConTanque
        when {
            rc != null -> {
                Text("Camión ${rc.ruta.vehiculo.placa} — Tanque: ${rc.litrosRestantes} / ${rc.ruta.vehiculo.capacidadTanqueLitros} L")
                MapaRuta(rc.ruta, Modifier.fillMaxWidth().height(240.dp))
                LazyColumn {
                    items(rc.ruta.paradas, key = { it.id }) { p ->
                        ListItem(
                            modifier = Modifier.clickable(enabled = p.estadoEntrega == EstadoEntrega.PENDIENTE) { onParadaClick(p.id) },
                            headlineContent = { Text("${p.orden}. ${p.punto.nombre}") },
                            supportingContent = { Text("Esperado: ${p.capacidadEsperadaLitros} L") },
                            trailingContent = {
                                Text(when (p.estadoEntrega) {
                                    EstadoEntrega.ENTREGADO -> "✔"
                                    EstadoEntrega.OMITIDO -> "✖ ${p.motivoOmision?.etiqueta ?: ""}"
                                    EstadoEntrega.PENDIENTE -> "•"
                                })
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
            estado.cargando -> CircularProgressIndicator()
            else -> Text("Sin ruta abierta")
        }
    }
}
