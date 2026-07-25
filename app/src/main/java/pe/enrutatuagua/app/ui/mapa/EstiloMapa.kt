package pe.enrutatuagua.app.ui.mapa

import android.content.Context
import java.io.File

/**
 * Estilo Protomaps offline: PMTiles empaquetado en assets (Lima Sur, plan 07 §1)
 * copiado una sola vez a filesDir, y plantillas JSON claro/oscuro con los tokens
 * de mapa del design system. Sin red: tiles y glifos viven en el APK.
 */
object EstiloMapa {
    private const val MARCADOR = "__PMTILES__"
    private const val ASSET_PMTILES = "mapa/lima-sur.pmtiles"

    fun prepararPmtiles(contexto: Context): File {
        val destino = File(contexto.filesDir, "mapa/lima-sur.pmtiles")
        if (!destino.exists()) {
            destino.parentFile?.mkdirs()
            contexto.assets.open(ASSET_PMTILES).use { entrada ->
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            }
        }
        return destino
    }

    fun json(contexto: Context, oscuro: Boolean): String {
        val plantilla = contexto.assets
            .open(if (oscuro) "mapa/estilo-oscuro.json" else "mapa/estilo-claro.json")
            .bufferedReader().use { it.readText() }
        val pmtiles = prepararPmtiles(contexto)
        return plantilla.replace(MARCADOR, "pmtiles://file://${pmtiles.absolutePath}")
    }
}
