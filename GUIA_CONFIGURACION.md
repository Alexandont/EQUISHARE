# EQUISHARE: guía de configuración y ejecución

Este repositorio tiene dos proyectos:

| Carpeta | Qué es | Tecnología |
|---|---|---|
| `equishare-api/` | Servidor API REST | Node.js 20.19+ y Express 5 |
| `equishare-android/` | Aplicación móvil | Android Studio, Kotlin, Jetpack Compose, MVVM, Room, DataStore, Retrofit, Firebase y Google Maps |

La app funciona en **dos modos**:

* **Modo demostración**: no necesita Firebase ni Google Maps. Sirve para probar todo el flujo de inmediato con las empresas de ejemplo (`constructora@demo.pe`, `eventos@demo.pe`, `taller@demo.pe` y cualquier contraseña de 8 caracteres con letras y números).
* **Modo completo**: Firebase Authentication y Cloud Firestore reales, y mapa de Google Maps. Se activa solo con colocar los archivos de configuración de los pasos 3 y 4.

---

## 1. Requisitos

1. **Android Studio** (versión estable actual) con el **SDK Platform 35 (Android 15)**. Si al abrir el proyecto aparece "Install missing SDK", acéptalo.
2. **Node.js 20.19 o superior** (`node -v` para verificarlo). Descarga en https://nodejs.org
3. **Git** y una cuenta de **GitHub** (para el repositorio que pide el informe).
4. Un emulador de Android con **Google Play** (imagen "Google APIs"/"Google Play") o un celular Android con depuración USB.

## 2. Ejecutar el servidor (API REST)

```bash
cd equishare-api
npm install
npm test          # 21 pruebas automáticas del servidor
npm start         # inicia en http://localhost:3000
```

Al iniciar, la consola muestra las direcciones para la app:

```
EQUISHARE API escuchando en el puerto 3000
  Emulador Android:   http://10.0.2.2:3000/
  Celular en la red:  http://192.168.1.50:3000/
```

* La primera vez se cargan **10 equipos de demostración** en Trujillo. Para reiniciar los datos: `npm run seed`.
* Los datos se guardan en `equishare-api/data/db.json` y las fotos en `equishare-api/uploads/`.
* El contrato de la API (endpoints y JSON) está en `equishare-api/docs/openapi.yaml`. Puedes abrirlo en https://editor.swagger.io (File > Import file).

## 3. Crear el proyecto de Firebase (Authentication + Cloud Firestore)

Todo en el **plan gratuito Spark**; no se necesita tarjeta.

1. Entra a https://console.firebase.google.com con tu cuenta de Google y pulsa **Crear un proyecto**. Nombre sugerido: `equishare-upn`. Puedes desactivar Google Analytics.
2. **Agregar la app Android**: en la página del proyecto pulsa el ícono de Android.
   * Nombre del paquete: **`com.equishare.app`** (exactamente así).
   * Apodo: `EQUISHARE`. El SHA-1 es opcional para correo y contraseña.
   * Descarga **`google-services.json`** y cópialo en **`equishare-android/app/google-services.json`**.
   * Los pasos de "Agregar el SDK" ya están hechos en el código; puedes saltarlos.
3. **Authentication**: menú *Compilación > Authentication > Comenzar*. En *Método de acceso* habilita **Correo electrónico/contraseña** y guarda.
4. **Cloud Firestore**: menú *Compilación > Firestore Database > Crear base de datos*.
   * Ubicación: la más cercana disponible (por ejemplo `southamerica-east1`).
   * Modo: **producción**.
   * En la pestaña **Reglas**, borra el contenido, pega el archivo `equishare-android/firestore.rules` y pulsa **Publicar**.
5. **Servidor en modo Firebase**: copia el ID del proyecto (*Configuración del proyecto > General > ID del proyecto*) y créalo en `equishare-api/.env`:

   ```
   FIREBASE_PROJECT_ID=equishare-upn
   ```

   Reinicia el servidor (`npm start`). Debe decir `Autenticación: Firebase (equishare-upn)`. Desde ese momento el servidor verifica la firma de cada token con las claves públicas de Google.

> No subas `google-services.json` ni `.env` a un repositorio público: ya están en `.gitignore`.

## 4. Crear la clave de Google Maps (opcional, recomendado)

Sin la clave la app funciona igual y la pestaña *Mapa* muestra una lista de ubicaciones.

1. Entra a https://console.cloud.google.com y selecciona el **mismo proyecto** que creó Firebase.
2. *APIs y servicios > Biblioteca* → busca **Maps SDK for Android** → **Habilitar**.
3. Google exige **asociar una cuenta de facturación** al proyecto para usar Maps. En la lista de precios de Google Maps Platform el SKU "Maps SDK" figura con uso gratuito ilimitado; aun así, revisa la página oficial antes de activarla y configura un presupuesto con alerta en *Facturación > Presupuestos y alertas*.
4. *APIs y servicios > Credenciales > Crear credenciales > Clave de API*.
5. Restringe la clave: *Restricciones de aplicaciones > Apps para Android* → agrega el paquete `com.equishare.app` y el SHA-1 de depuración. Para obtenerlo, en Android Studio abre la terminal y ejecuta `./gradlew signingReport` (Windows: `gradlew signingReport`) y copia el valor `SHA1` de la variante `debug`. En *Restricciones de API* elige **Maps SDK for Android**.
6. Abre `equishare-android/local.properties` (lo crea Android Studio) y agrega al final:

   ```
   MAPS_API_KEY=AIza...tu_clave...
   ```

## 5. Ejecutar la app

1. En Android Studio: *File > Open* → elige la carpeta **`equishare-android`** y espera la sincronización de Gradle.
2. Inicia el servidor (paso 2).
3. Pulsa **Run ▶** con un emulador.
   * Emulador: la app ya apunta a `http://10.0.2.2:3000/`.
   * Celular físico: conecta el celular y la PC a la **misma red Wi-Fi**; en la app ve a *Ajustes de conexión* (pantalla de inicio de sesión) o *Perfil > Conexión con el servidor* y escribe la IP que mostró la consola, por ejemplo `http://192.168.1.50:3000/`. Si no conecta, permite Node.js en el firewall de Windows.
4. Para que el emulador tenga fotos que elegir, arrastra imágenes JPG/PNG a la ventana del emulador (se guardan en Descargas).
5. Pruebas unitarias de la app: clic derecho en `app/src/test` → **Run 'Tests in test'**, o `./gradlew testDebugUnitTest`.

## 6. Recorrido sugerido para la exposición (8 minutos)

1. Iniciar sesión como `eventos@demo.pe` → catálogo con filtros por categoría, búsqueda y "Cerca de mí" (permiso de ubicación).
2. Abrir *Mezcladora de concreto* → reseñas, disponibilidad real y mapa → **Elegir fechas** → desglose de costos transparente (comisión 12 % y política de cancelación) → **Confirmar reserva**.
3. Intentar reservar para mañana → mensaje de la regla RN-07 (24 h de anticipación).
4. Cerrar sesión e ingresar como `constructora@demo.pe` → *Reservas > Mis equipos alquilados* → Confirmar → Marcar entregado → Marcar devuelto.
5. Volver como `eventos@demo.pe` → **Calificar** (RN-05).
6. *Mis equipos > Publicar equipo* → elegir una foto pequeña para mostrar el rechazo automático con su motivo (RN-04) → elegir una foto válida → publicar.
7. Activar el modo avión → aviso "Sin conexión" y catálogo guardado en Room → desactivarlo → recarga automática.

Con **Firebase activo** las empresas demo no tienen cuenta en Firebase, así que crea dos cuentas nuevas (una por rol) desde *¿Tu empresa es nueva? Crea una cuenta*. Para el registro puedes usar estos RUC ficticios que cumplen el dígito verificador: `20999999931`, `20999999949`, `20999999957`, `20999999965`, `20999999973`. Con la cuenta "arrendadora" publica un equipo y con la otra resérvalo.

## 7. Subir el código a GitHub (para el informe)

1. En https://github.com pulsa **New repository**. Nombre: `equishare`. Privado o público (si es público, no subas credenciales).
2. En la carpeta raíz `EQUISHARE`:

   ```bash
   git init
   git add .
   git commit -m "EQUISHARE: avance del proyecto (API REST + app Android)"
   git branch -M main
   git remote add origin https://github.com/<usuario>/equishare.git
   git push -u origin main
   ```

3. Agrega a los integrantes en *Settings > Collaborators* y copia el enlace en la sección 7.2 y 8.4 del informe.

## 8. Problemas frecuentes

| Problema | Solución |
|---|---|
| "No se pudo conectar con el servidor" | Verifica que `npm start` esté corriendo y la dirección en *Ajustes de conexión* (emulador: `http://10.0.2.2:3000/`). |
| `Unresolved reference: BuildConfig` | *Build > Rebuild Project* (se genera al compilar). |
| El mapa sale gris | Falta `MAPS_API_KEY`, la API no está habilitada o el SHA-1 no coincide. |
| "Correo o contraseña incorrectos" con Firebase | La cuenta debe crearse primero con *¿Tu empresa es nueva? Crea una cuenta*. |
| Error 401 en modo Firebase | El `FIREBASE_PROJECT_ID` del `.env` debe ser el mismo proyecto del `google-services.json`. |
| La sincronización de Gradle falla por la versión de Java | En *File > Settings > Build, Execution, Deployment > Build Tools > Gradle* elige como *Gradle JDK* el JDK incluido en Android Studio (versión 17 o 21) y vuelve a sincronizar. |
| `SDK location not found` | Abre el proyecto desde Android Studio para que cree `local.properties` con la ruta del SDK, o agrégala a mano: `sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk`. |
