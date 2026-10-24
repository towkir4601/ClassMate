package com.shuaib.classmate.network

import com.shuaib.classmate.models.BatchDetailResponse
import com.shuaib.classmate.models.BatchListResponse
import com.shuaib.classmate.models.ScheduleResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

// Response model for the /api/health endpoint
data class HealthResponse(
    val status: String?
)

// Response model for the /api/upload endpoint
data class UploadResponse(
    val message: String?,
    val error: String?,
    val details: String?,
    val batchId: Int?,
    val batch_id: Int?
)

interface ApiService {

    // GET /api/health  — wake the Render instance & verify it is alive
    @GET("api/health")
    suspend fun checkHealth(): Response<HealthResponse>

    // POST /api/upload  — multipart form-data with .xlsx file and metadata
    @Multipart
    @POST("api/upload")
    suspend fun uploadExcelFile(
        @Part file: MultipartBody.Part,
        @Part("faculty") faculty: RequestBody,
        @Part("year") year: RequestBody,
        @Part("semester") semester: RequestBody
    ): Response<ResponseBody>

    // GET /api/batches — list of batches
    @GET("api/batches")
    suspend fun getBatches(): Response<BatchListResponse>

    // GET /api/batches/{id} — batch detail & stats overview
    @GET("api/batches/{id}")
    suspend fun getBatchDetail(
        @Path("id") id: Int
    ): Response<BatchDetailResponse>

    // POST /api/batches/{id}/generate — run backend CSP scheduler algorithm
    @POST("api/batches/{id}/generate")
    suspend fun generateSchedule(
        @Path("id") id: Int
    ): Response<ScheduleResponse>

    // GET /api/batches/{id}/schedule — read persisted generated routine
    @GET("api/batches/{id}/schedule")
    suspend fun getSchedule(
        @Path("id") id: Int
    ): Response<ScheduleResponse>

    // Export PDF
    @Streaming
    @GET("api/batches/{id}/export.pdf")
    suspend fun exportPdf(
        @Path("id") id: Int
    ): Response<ResponseBody>

    // Export DOCX
    @Streaming
    @GET("api/batches/{id}/export.docx")
    suspend fun exportDocx(
        @Path("id") id: Int
    ): Response<ResponseBody>

    // Export CSV
    @Streaming
    @GET("api/batches/{id}/export.csv")
    suspend fun exportCsv(
        @Path("id") id: Int
    ): Response<ResponseBody>
}
