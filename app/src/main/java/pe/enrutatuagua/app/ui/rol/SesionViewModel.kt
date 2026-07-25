package pe.enrutatuagua.app.ui.rol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pe.enrutatuagua.domain.modelo.Rol
import pe.enrutatuagua.domain.usecase.SeleccionarRolUseCase
import javax.inject.Inject

@HiltViewModel
class SesionViewModel @Inject constructor(
    private val seleccionarRolUseCase: SeleccionarRolUseCase,
) : ViewModel() {
    fun seleccionarRol(rol: Rol) { viewModelScope.launch { seleccionarRolUseCase(rol) } }
}
