package pe.enrutatuagua.domain.usecase

import pe.enrutatuagua.domain.repositorio.DescargadorRegionMapa
import pe.enrutatuagua.domain.repositorio.RegionMapa

class DescargadorRegionMapaFake : DescargadorRegionMapa {
    val descargas = mutableListOf<Pair<String, RegionMapa>>()

    override fun descargar(clave: String, region: RegionMapa) {
        descargas += clave to region
    }
}
