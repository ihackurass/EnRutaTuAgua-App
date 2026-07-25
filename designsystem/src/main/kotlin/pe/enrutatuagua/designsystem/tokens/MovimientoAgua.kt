package pe.enrutatuagua.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing

// ANDROID-PLAN-07 §2.5 — movimiento. Una sola curva firma para toda la app.
object MovimientoAgua {
    val EasingSalida = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    const val Hoja = 420
    const val Perilla = 300
    const val Tubo = 700
    const val Aviso = 400
    const val AvisoVisible = 2600L
    const val Latido = 2400
    const val Press = 120
}
