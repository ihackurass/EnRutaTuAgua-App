# Simulador de GPS para el emulador (plan 09): recorre la ruta del
# aguatero enviando fixes con `adb emu geo fix`. La app calcula el rumbo
# entre fixes (MotorPosicionConRumbo), asi que no hace falta bearing.
#
# ANDROID-PLAN-02 (T8): SeedInicial.kt ya no existe (Room dado de baja,
# la ruta viene del backend real) -- estos waypoints quedaron
# coincidiendo con los del seed real (R__seed_desarrollo.sql), pero si
# la ruta abierta trae otras paradas, hay que actualizar este array a
# mano con `GET /api/v1/rutas/abierta` + `GET /api/v1/puntos/{id}`.
#
# Uso tipico (emulador Pixel_7a_API_35 corriendo):
#   powershell -ExecutionPolicy Bypass -File scripts\simular-gps.ps1
#   powershell ... -VelocidadKmh 40 -IntervaloMs 500 -Bucle
#   powershell ... -SoloImprimir           # dry-run sin adb
#   powershell ... -Serial emulator-5554   # si hay varios dispositivos
param(
    [double]$VelocidadKmh = 20,
    [int]$IntervaloMs = 1000,
    [switch]$Bucle,
    [switch]$SoloImprimir,
    [string]$Adb = "adb",
    [string]$Serial = ""
)

$culturaInvariante = [System.Globalization.CultureInfo]::InvariantCulture

# Waypoints = las 4 paradas de SeedInicial.kt (data/src/main/.../SeedInicial.kt)
$paradas = @(
    @{ Nombre = "Comite 12 - Loma Alta";             Lat = -12.1571; Lng = -76.9442 },
    @{ Nombre = "AAHH Buena Vista, sector B";        Lat = -12.1613; Lng = -76.9488 },
    @{ Nombre = "Comite 7 - Jose C. Mariategui";     Lat = -12.1652; Lng = -76.9521 },
    @{ Nombre = "Paradero Piedra Grande";            Lat = -12.1699; Lng = -76.9563 }
)

function Get-DistanciaMetros([double]$latA, [double]$lngA, [double]$latB, [double]$lngB) {
    # Haversine
    $radioTierra = 6371000.0
    $dLat = ($latB - $latA) * [Math]::PI / 180
    $dLng = ($lngB - $lngA) * [Math]::PI / 180
    $a = [Math]::Sin($dLat / 2) * [Math]::Sin($dLat / 2) +
         [Math]::Cos($latA * [Math]::PI / 180) * [Math]::Cos($latB * [Math]::PI / 180) *
         [Math]::Sin($dLng / 2) * [Math]::Sin($dLng / 2)
    return $radioTierra * 2 * [Math]::Atan2([Math]::Sqrt($a), [Math]::Sqrt(1 - $a))
}

function Send-Fix([double]$lat, [double]$lng, [string]$etiqueta) {
    # ORDEN CRITICO: geo fix <LONGITUD> <LATITUD>.
    # InvariantCulture: en locale es-PE los decimales salen con COMA y geo fix se rompe.
    $lngTxt = $lng.ToString("0.000000", $culturaInvariante)
    $latTxt = $lat.ToString("0.000000", $culturaInvariante)
    Write-Host "$etiqueta  geo fix $lngTxt $latTxt"
    if (-not $SoloImprimir) {
        $argumentos = @()
        if ($Serial -ne "") { $argumentos += @("-s", $Serial) }
        $argumentos += @("emu", "geo", "fix", $lngTxt, $latTxt)
        & $Adb @argumentos | Out-Null
        Start-Sleep -Milliseconds $IntervaloMs
    }
}

$metrosPorTick = ($VelocidadKmh / 3.6) * ($IntervaloMs / 1000.0)
Write-Host "Simulando a $VelocidadKmh km/h, un fix cada $IntervaloMs ms (~$([Math]::Round($metrosPorTick, 1)) m por fix)"

do {
    for ($i = 0; $i -lt $paradas.Count - 1; $i++) {
        $a = $paradas[$i]
        $b = $paradas[$i + 1]
        $distancia = Get-DistanciaMetros $a.Lat $a.Lng $b.Lat $b.Lng
        $pasos = [Math]::Max(1, [Math]::Ceiling($distancia / $metrosPorTick))
        Write-Host "-- $($a.Nombre) -> $($b.Nombre): $([Math]::Round($distancia)) m en $pasos fixes"
        for ($t = 0; $t -le $pasos; $t++) {
            $fraccion = $t / [double]$pasos
            $lat = $a.Lat + ($b.Lat - $a.Lat) * $fraccion
            $lng = $a.Lng + ($b.Lng - $a.Lng) * $fraccion
            Send-Fix $lat $lng "[parada $($i + 1)->$($i + 2)] paso $t/$pasos"
        }
    }
    if ($Bucle) {
        [array]::Reverse($paradas)
        Write-Host "== Ruta invertida (bucle; Ctrl+C para cortar) =="
    }
} while ($Bucle)
