package pe.enrutatuagua.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.domain.modelo.*

@RunWith(RobolectricTestRunner::class)
class RutaRepositoryRoomTest {
    private lateinit var repo: RutaRepositoryRoom

    @Before fun setUp() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), EnRutaTuAguaDb::class.java
        ).allowMainThreadQueries().build()
        repo = RutaRepositoryRoom(db)
    }

    @Test fun `siembra ruta ABIERTA con vehiculo y 4 paradas ordenadas`() = runTest {
        repo.sembrarSiVacio()
        val ruta = repo.observarRutaAbierta().first()!!
        assertEquals(EstadoRuta.ABIERTA, ruta.estado)
        assertEquals(12000, ruta.vehiculo.capacidadTanqueLitros)
        assertEquals(listOf(1, 2, 3, 4), ruta.paradas.map { it.orden })
    }

    @Test fun `registrar entrega persiste y omitir guarda motivo`() = runTest {
        repo.sembrarSiVacio()
        val ruta = repo.observarRutaAbierta().first()!!
        val parada = ruta.paradas.first()

        repo.registrarEntrega(Entrega("uuid-1", parada.id, TipoEntrega.ENTREGA, null, 3500, null, null, 0L))
        assertEquals(3500, repo.observarEntregas(ruta.id).first().single().litros)

        repo.omitirParada(ruta.paradas[1].id, MotivoOmision.NADIE_PRESENTE, null)
        val recargada = repo.observarRutaAbierta().first()!!
        assertEquals(EstadoEntrega.OMITIDO, recargada.paradas[1].estadoEntrega)
        assertEquals(MotivoOmision.NADIE_PRESENTE, recargada.paradas[1].motivoOmision)
    }
}
