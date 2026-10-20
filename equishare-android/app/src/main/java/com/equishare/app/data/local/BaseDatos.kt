package com.equishare.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Caché local de equipos. "lista" distingue el catálogo público de los equipos propios. */
@Entity(tableName = "equipos", primaryKeys = ["id", "lista"])
data class EquipoEntity(
    val id: String,
    val lista: String,
    val orden: Int,
    val empresaId: String,
    val empresaNombre: String,
    val nombre: String,
    val categoria: String,
    val descripcion: String,
    val precioDia: Double,
    val fotoUrl: String?,
    val fotoId: String?,
    val lat: Double,
    val lng: Double,
    val direccion: String,
    val estado: String,
    val calificacionPromedio: Double,
    val totalResenas: Int,
    val distanciaKm: Double?,
    val guardadoEn: Long,
)

/** Caché local de reservas por rol (arrendatario / arrendador). */
@Entity(tableName = "reservas", primaryKeys = ["id", "rol"])
data class ReservaEntity(
    val id: String,
    val rol: String,
    val orden: Int,
    val equipoId: String,
    val equipoNombre: String,
    val equipoFotoUrl: String?,
    val arrendatarioNombre: String,
    val arrendadorNombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val dias: Int,
    val precioDia: Double,
    val subtotal: Double,
    val comision: Double,
    val montoArrendador: Double,
    val total: Double,
    val penalidad: Double,
    val estado: String,
    val resenaEnviada: Boolean,
    val devueltaEn: String?,
)

@Dao
abstract class EquipoDao {
    @Query("SELECT * FROM equipos WHERE lista = :lista ORDER BY orden ASC")
    abstract fun observar(lista: String): Flow<List<EquipoEntity>>

    @Query("SELECT * FROM equipos WHERE id = :id LIMIT 1")
    abstract suspend fun buscar(id: String): EquipoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertar(items: List<EquipoEntity>)

    @Query("DELETE FROM equipos WHERE lista = :lista")
    abstract suspend fun borrarLista(lista: String)

    @Query("DELETE FROM equipos WHERE id = :id")
    abstract suspend fun eliminar(id: String)

    @Transaction
    open suspend fun reemplazarLista(lista: String, items: List<EquipoEntity>) {
        borrarLista(lista)
        insertar(items)
    }
}

@Dao
abstract class ReservaDao {
    @Query("SELECT * FROM reservas WHERE rol = :rol ORDER BY orden ASC")
    abstract fun observar(rol: String): Flow<List<ReservaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertar(items: List<ReservaEntity>)

    @Query("DELETE FROM reservas WHERE rol = :rol")
    abstract suspend fun borrarRol(rol: String)

    @Transaction
    open suspend fun reemplazarRol(rol: String, items: List<ReservaEntity>) {
        borrarRol(rol)
        insertar(items)
    }

    @Query("DELETE FROM reservas")
    abstract suspend fun borrarTodo()
}

@Database(entities = [EquipoEntity::class, ReservaEntity::class], version = 1, exportSchema = false)
abstract class EquishareDatabase : RoomDatabase() {
    abstract fun equipoDao(): EquipoDao
    abstract fun reservaDao(): ReservaDao

    companion object {
        fun crear(context: Context): EquishareDatabase =
            Room.databaseBuilder(context, EquishareDatabase::class.java, "equishare.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
