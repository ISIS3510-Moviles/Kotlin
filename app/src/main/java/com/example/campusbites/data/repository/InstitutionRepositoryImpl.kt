package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.InstitutionDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.InstitutionRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class InstitutionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): InstitutionRepository {

    override suspend fun getInstitutionById(id: String): InstitutionDTO? {
        return try {
            apiService.getInstitutionById(id)
        } catch (e: HttpException) {
            Log.e("InstitutionRepo", "HttpException fetching institution $id: ${e.code()} - ${e.message()}", e)
            null
        } catch (e: IOException) {
            Log.e("InstitutionRepo", "IOException fetching institution $id: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("InstitutionRepo", "Exception fetching institution $id: ${e.message}", e)
            null
        }
    }
}