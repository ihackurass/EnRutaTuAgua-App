package pe.enrutatuagua.data

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.enrutatuagua.domain.modelo.EstadoEntrega
import pe.enrutatuagua.domain.modelo.MotivoOmision

class MappersTest {
    @Test
    fun `codigos identicos al catalogo_valor del DDL`() {
        assertEquals(3, EstadoEntrega.OMITIDO.codigo)
        assertEquals(EstadoEntrega.OMITIDO, estadoEntregaDesdeCodigo(3))
        assertEquals(4, MotivoOmision.OTRO.codigo)
        assertEquals(MotivoOmision.OTRO, motivoDesdeCodigo(4))
        assertEquals(null, motivoDesdeCodigo(null))
    }
}
