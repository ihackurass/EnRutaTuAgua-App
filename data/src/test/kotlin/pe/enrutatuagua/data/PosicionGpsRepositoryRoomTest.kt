package pe.enrutatuagua.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pe.enrutatuagua.data.local.EnRutaTuAguaDb
import pe.enrutatuagua.domain.repositorio.PosicionGps

@RunWith(RobolectricTestRunner::class)
class PosicionGpsRepositoryRoomTest {
    private lateinit var repo: PosicionGpsRepositoryRoom

    @Before fun setUp() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), EnRutaTuAguaDb::class.java
        ).allowMainThreadQueries().build()
        repo = PosicionGpsRepositoryRoom(db)
    }

    private fun posicion(registradaEnMillis: Long) =
        PosicionGps(rutaId = 42, lat = -12.16, lng = -76.94, registradaEnMillis = registradaEnMillis)

    @Test fun `guardar deja el punto disponible en pendientes`() = runTest {
        repo.guardar(posicion(1_000L))
        repo.guardar(posicion(2_000L))

        val pendientes = repo.pendientes(maxItems = 10)

        assertEquals(2, pendientes.size)
        assertEquals(listOf(1_000L, 2_000L), pendientes.map { it.registradaEnMillis })
    }

    @Test fun `marcarPublicadas quita del buffer sólo los ids confirmados`() = runTest {
        val a = repo.guardar(posicion(1_000L))
        val b = repo.guardar(posicion(2_000L))

        repo.marcarPublicadas(listOf(a.id))

        val pendientes = repo.pendientes(maxItems = 10)
        assertEquals(listOf(b.id), pendientes.map { it.id })
    }

    @Test fun `purgarAntiguas elimina lo anterior al limite de tiempo`() = runTest {
        repo.guardar(posicion(1_000L))
        repo.guardar(posicion(9_000L))

        repo.purgarAntiguas(limiteMillis = 5_000L, maxFilas = 100)

        val pendientes = repo.pendientes(maxItems = 10)
        assertEquals(listOf(9_000L), pendientes.map { it.registradaEnMillis })
    }

    @Test fun `purgarAntiguas recorta al tope de filas quedandose con lo mas reciente`() = runTest {
        repeat(5) { i -> repo.guardar(posicion(i.toLong())) }

        repo.purgarAntiguas(limiteMillis = 0L, maxFilas = 2)

        val pendientes = repo.pendientes(maxItems = 10)
        assertEquals(2, pendientes.size)
        assertTrue(pendientes.all { it.registradaEnMillis >= 3L })
    }
}
