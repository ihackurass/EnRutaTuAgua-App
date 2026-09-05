# EnRutaTuAgua App

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Architecture](https://img.shields.io/badge/Arquitectura-Modular-00ACC1)](#arquitectura)
[![Status](https://img.shields.io/badge/Estado-Piloto-brightgreen)]()

## Visión general

**EnRutaTuAgua** es una aplicación Android nativa para coordinar y visualizar la distribución de agua mediante camiones cisterna. Ofrece experiencias específicas para aguateros, presidentes de asociaciones, vecinos y operadores, conectadas al backend real mediante REST, MQTT y WebSocket.

La aplicación está desarrollada con Kotlin y Jetpack Compose. Su diseño modular separa interfaz, reglas de negocio, acceso a datos y sistema visual para facilitar pruebas, mantenimiento y evolución independiente.

## Características principales

- **Acceso por roles:** Aguatero, Presidente, Vecino y Operador.
- **Gestión de pedidos:** creación multipunto, prioridades, ventanas y consulta de estado.
- **Planificación operativa:** selección de cisterna, aguatero, fecha y pedidos de una ruta.
- **Ruta del aguatero:** carga, navegación, entregas parciales, omisiones y cierre.
- **Operación offline:** cola local en Room con reintentos y confirmación idempotente.
- **Seguimiento GPS:** publicación por MQTT y representación del rumbo de la cisterna.
- **Mapa del vecino:** posición en vivo mediante WebSocket y polling REST de respaldo.
- **Cadena de custodia:** recepciones y conciliación del agua para el Presidente.
- **Material Design 3:** interfaz declarativa, accesible y adaptable con Jetpack Compose.

## Arquitectura

```mermaid
flowchart TD
    UI[app · Compose + ViewModels] --> D[domain · modelos y contratos]
    UI --> DS[designsystem · componentes y tema]
    DATA[data · implementaciones de repositorios] --> D
    DATA --> REST[Retrofit / REST]
    DATA --> DB[(Room)]
    DATA --> MQTT[MQTT]
    UI --> WS[WebSocket de tracking]
```

### Módulos

| Módulo | Responsabilidad |
| --- | --- |
| `app` | Pantallas Compose, navegación, ViewModels, GPS e inyección de dependencias |
| `domain` | Modelos y contratos de repositorios independientes de Android |
| `data` | Retrofit, Room, MQTT, serialización y repositorios concretos |
| `designsystem` | Tema Material 3 y componentes visuales reutilizables |

El flujo habitual es `UI → ViewModel → repositorio de dominio → implementación data`. Hilt ensambla las dependencias y las corrutinas exponen estado reactivo a Compose.

## Tecnologías utilizadas

<div align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material 3"/>
  <img src="https://img.shields.io/badge/Room-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Room"/>
  <img src="https://img.shields.io/badge/Retrofit-48B983?style=for-the-badge&logo=square&logoColor=white" alt="Retrofit"/>
  <img src="https://img.shields.io/badge/MapLibre-396CB2?style=for-the-badge&logo=openstreetmap&logoColor=white" alt="MapLibre"/>
  <img src="https://img.shields.io/badge/MQTT-660066?style=for-the-badge&logo=mqtt&logoColor=white" alt="MQTT"/>
</div>

## Guía de instalación

### Requisitos previos

- Android Studio con soporte para AGP 8.7.3.
- JDK 17.
- Android SDK 35.
- Dispositivo o emulador con Android 8.0 (API 26) o superior.
- Una instancia del backend EnRutaTuAgua.

### Configuración y ejecución

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/ihackurass/EnRutaTuAgua-App.git
   cd EnRutaTuAgua-App
   ```

2. Ejecutar en el emulador usando los valores predeterminados (`10.0.2.2`):

   ```bash
   ./gradlew installDebug
   ```

3. Para un dispositivo físico, indicar la IP LAN del equipo que ejecuta el backend:

   ```bash
   ./gradlew installDebug \
     -PENRUTA_BASE_URL=http://192.168.1.10:8080/ \
     -PENRUTA_MQTT_BROKER=tcp://192.168.1.10:1883
   ```

En Windows, usar `gradlew.bat`. No guardar contraseñas MQTT en archivos versionados; `ENRUTA_MQTT_PASSWORD` puede proporcionarse como propiedad Gradle durante la compilación.

### Pruebas

```bash
./gradlew test
```

La suite incluye pruebas unitarias, ViewModels, repositorios, Compose con Robolectric y verificaciones visuales con Roborazzi.

## Capturas de pantalla

<div align="center">
  <img src="docs/images/presidente-pedidos.png" alt="Pedidos del Presidente" width="23%"/>
  <img src="docs/images/operador-planificacion.png" alt="Planificación del Operador" width="23%"/>
  <img src="docs/images/aguatero-tracking-gps.png" alt="Ruta y GPS del Aguatero" width="23%"/>
  <img src="docs/images/vecino-tracking-en-vivo.png" alt="Tracking en vivo del Vecino" width="23%"/>
</div>

## Comunicación con el backend

| Canal | Uso |
| --- | --- |
| REST + JWT | Sesiones, pedidos, rutas, entregas y custodia |
| MQTT | Publicación de posiciones GPS del aguatero |
| WebSocket | Actualización en vivo de la cisterna para el vecino |
| Room | Persistencia local y cola de acciones offline |

## Repositorio relacionado

- [Backend EnRutaTuAgua](https://github.com/ihackurass/EnRutaTuAgua-Backend)

## Contacto

Si tienes preguntas o sugerencias, puedes abrir un issue o contactar a [@ihackurass](https://github.com/ihackurass).

---

<div align="center">
  <sub>Hecho con ❤️ para el Perú. Tecnología para acercar el agua a quienes más la necesitan.</sub>
</div>
