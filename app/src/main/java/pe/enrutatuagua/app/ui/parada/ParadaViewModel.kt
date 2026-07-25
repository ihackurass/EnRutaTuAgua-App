package pe.enrutatuagua.app.ui.parada

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.ErrorDominio
import pe.enrutatuagua.domain.modelo.MotivoOmision
import pe.enrutatuagua.domain.usecase.ObtenerRutaAbiertaUseCase
import pe.enrutatuagua.domain.usecase.OmitirParadaUseCase
import pe.enrutatuagua.domain.usecase.RegistrarEntregaUseCase
import javax.inject.Inject

/** Lo que la pantalla necesita de la parada (plan 07 T7). */
data class ParadaUi(val titulo: String, val pedidoLitros: Int, val litrosRestantes: Int)

@HiltViewModel
class ParadaViewModel @Inject constructor(
    private val registrarEntrega: RegistrarEntregaUseCase,
    private val omitirParada: OmitirParadaUseCase,
    obtenerRutaAbierta: ObtenerRutaAbiertaUseCase,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val paradaId: Long = checkNotNull(savedState["paradaId"])
    val iniciaEnOmision: Boolean = savedState["omitir"] ?: false

    private val _hecho = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _parada = MutableStateFlow<ParadaUi?>(null)
    val hecho: StateFlow<Boolean> = _hecho
    val error: StateFlow<String?> = _error
    val parada: StateFlow<ParadaUi?> = _parada

    init {
        viewModelScope.launch {
            obtenerRutaAbierta().collect { rc ->
                _parada.value = rc?.ruta?.paradas
                    ?.firstOrNull { it.id == paradaId }
                    ?.let { p -> ParadaUi(p.punto.nombre, p.capacidadEsperadaLitros, rc.litrosRestantes) }
            }
        }
    }

    fun confirmarEntrega(litros: Int) = ejecutar { registrarEntrega(paradaId, litros) }
    fun omitir(motivo: MotivoOmision, comentario: String?) = ejecutar { omitirParada(paradaId, motivo, comentario) }

    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            try { _error.value = null; accion(); _hecho.value = true }
            catch (e: ErrorDominio) { _error.value = e.message }
        }
    }
}
