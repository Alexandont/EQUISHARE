package com.equishare.app.data.repositorio

import com.equishare.app.data.aDominio
import com.equishare.app.data.remote.ApiProvider
import com.equishare.app.data.remote.dto.EmpresaEntradaDto
import com.equishare.app.data.remote.llamarApi
import com.equishare.app.domain.model.Empresa
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.repositorio.EmpresaRepository

class EmpresaRepositoryImpl(private val apiProvider: ApiProvider) : EmpresaRepository {

    override suspend fun miEmpresa(): Resultado<Empresa?> =
        when (val resultado = llamarApi { apiProvider.api().miEmpresa() }) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error ->
                // Solo un 404 propio de la API significa "empresa aún no registrada".
                if (resultado.error.tipo == TipoError.NO_ENCONTRADO && resultado.error.codigo == "NO_ENCONTRADO") {
                    Resultado.Exito(null)
                } else {
                    resultado
                }
        }

    override suspend fun guardar(ruc: String, razonSocial: String, telefono: String): Resultado<Empresa> =
        when (val resultado = llamarApi {
            apiProvider.api().guardarEmpresa(EmpresaEntradaDto(ruc.trim(), razonSocial.trim(), telefono.trim()))
        }) {
            is Resultado.Exito -> Resultado.Exito(resultado.datos.aDominio())
            is Resultado.Error -> resultado
        }
}
