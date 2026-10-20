# EQUISHARE

Plataforma móvil B2B para alquilar y compartir equipos subutilizados entre pymes de Trujillo (ODS 12: producción y consumo responsables). Proyecto del curso Desarrollo de Aplicaciones Móviles, Ingeniería de Sistemas Computacionales, UPN, 2026-2.

* `equishare-api/`: API REST en Node.js + Express (CRUD de equipos, reservas y reseñas, reglas de negocio RN-01 a RN-08, validación automática de fotos y verificación de tokens de Firebase).
* `equishare-android/`: app Android nativa en Kotlin con Jetpack Compose y arquitectura MVVM (Room, DataStore, Retrofit, Firebase Authentication, Cloud Firestore y Google Maps).

Instrucciones completas en [GUIA_CONFIGURACION.md](GUIA_CONFIGURACION.md).

## Estructura de la app (MVVM)

```
app/src/main/java/com/equishare/app/
├── domain/        Modelos, reglas de negocio, validaciones e interfaces de repositorio (Kotlin puro)
├── data/
│   ├── remote/    Retrofit: EquishareApi, DTO, manejo de errores HTTP
│   ├── local/     Room (caché offline) y DataStore (preferencias)
│   ├── auth/      Firebase Authentication y modo demostración
│   ├── firebase/  Cloud Firestore (favoritos y consentimiento)
│   ├── sistema/   Conectividad, ubicación (Fused Location) y lectura de imágenes
│   └── repositorio/ Implementaciones del patrón Repository
└── ui/            Pantallas Jetpack Compose + ViewModel con UI State inmutable
```
