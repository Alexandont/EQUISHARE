# Reglas de ProGuard/R8. La minificación está desactivada en esta etapa del proyecto.
# Si se activa, conservar los DTO usados por Gson:
-keep class com.equishare.app.data.remote.dto.** { *; }
