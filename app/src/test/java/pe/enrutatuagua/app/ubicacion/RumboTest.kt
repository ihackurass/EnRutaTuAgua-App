package pe.enrutatuagua.app.ubicacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RumboTest {

    @Test
    fun `hacia el norte es 0 grados`() {
        assertEquals(0f, rumboEntre(-12.16, -76.95, -12.15, -76.95), 0.5f)
    }

    @Test
    fun `hacia el este es 90 grados`() {
        assertEquals(90f, rumboEntre(0.0, 0.0, 0.0, 0.01), 0.5f)
    }

    @Test
    fun `hacia el sur es 180 grados`() {
        assertEquals(180f, rumboEntre(-12.15, -76.95, -12.16, -76.95), 0.5f)
    }

    @Test
    fun `hacia el oeste es 270 grados`() {
        assertEquals(270f, rumboEntre(0.0, 0.01, 0.0, 0.0), 0.5f)
    }

    @Test
    fun `diagonal noreste en el ecuador es 45 grados`() {
        assertEquals(45f, rumboEntre(0.0, 0.0, 0.01, 0.01), 0.5f)
    }

    @Test
    fun `siempre devuelve un valor en el rango 0 a 360`() {
        val puntos = listOf(
            -12.1571 to -76.9442,
            -12.1613 to -76.9488,
            -12.1652 to -76.9521,
            -12.1699 to -76.9563,
        )
        for ((a, b) in puntos.zipWithNext()) {
            val rumbo = rumboEntre(a.first, a.second, b.first, b.second)
            assertTrue("rumbo $rumbo fuera de [0, 360)", rumbo >= 0f && rumbo < 360f)
        }
    }

    @Test
    fun `el rumbo de vuelta es el de ida mas 180`() {
        val ida = rumboEntre(-12.1571, -76.9442, -12.1613, -76.9488)
        val vuelta = rumboEntre(-12.1613, -76.9488, -12.1571, -76.9442)
        assertEquals((ida + 180f) % 360f, vuelta, 0.5f)
    }
}
