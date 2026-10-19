# GeoRegistro — Sistema de Registro de Incidencias en Terreno

Aplicación móvil nativa para Android desarrollada en **Java** para la asignatura **Taller de Nuevas Tecnologías (PA26-04724)** del Instituto Profesional Virginio Gómez. Permite registrar incidencias georreferenciadas en terreno, consultar su historial persistente desde Firebase Cloud Firestore, ubicar marcadores en Google Maps y monitorear el sensor acelerómetro del dispositivo.

## Información del Estudiante
* Estudiante:Nicolás Ignacio San Martín Godoy
* Docente: Pablo Calderón Cornejo


---

## Entorno de Desarrollo y Versiones
* IDE: Android Studio Ladybug Feature Drop (2024.2+)
* Lenguaje: Java 8 / JVM 17
* Compile SDK: 36
* Min SDK: 27
* Target SDK: 36
* Build System: Gradle (Kotlin DSL - build.gradle.kts)

---

## Requisitos Funcionales Implementados
* **RF01:** Pantalla principal con acceso y navegación funcional hacia Registro, Historial y Mapa[cite: 1].
* **RF02:** Formulario completo con validación obligatoria de datos (título, descripción, categoría, fecha/hora y coordenadas)[cite: 1].
* **RF03:** Persistencia en la nube mediante **Firebase Cloud Firestore**, controlando fallos de conectividad sin cierres forzosos de la aplicación[cite: 1].
* **RF04:** Solicitud de permisos de ubicación (`ACCESS_FINE_LOCATION`) en tiempo de ejecución, captura de coordenadas reales mediante `FusedLocationProviderClient` y soporte para ingreso manual en caso de permiso denegado[cite: 1].
* **RF05:** Integración de **Google Maps SDK** con despliegue de marcadores de las incidencias registradas e inyección segura de clave API[cite: 1].
* **RF06:** Detección de movimiento mediante el sensor **Acelerómetro** (`TYPE_ACCELEROMETER`), verificando disponibilidad de hardware y permitiendo limpiar el formulario agitando el teléfono (*shake*)[cite: 1].
* **RF08:** Seguridad: no inclusión de archivos sensibles (`google-services.json` y `local.properties`) en el repositorio y reglas restringidas en Firestore[cite: 1].

---

## Instrucciones de Instalación, Configuración y Ejecución (Sin Secretos)

Para cumplir con la política de seguridad requerida (**RF08**), este repositorio público no contiene credenciales privadas ni claves secretas[cite: 1, 34]. Para compilar y ejecutar este proyecto:

### 1. Clonar el repositorio
```bash
git clone [https://github.com/Nicooo-bot/ProyectoEvaluacion-N-2-Nicolas-San-Martin.git](https://github.com/Nicooo-bot/ProyectoEvaluacion-N-2-Nicolas-San-Martin.git)