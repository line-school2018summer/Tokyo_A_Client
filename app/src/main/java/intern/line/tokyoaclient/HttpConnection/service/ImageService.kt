package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.Image
import okhttp3.MultipartBody
import retrofit2.http.*
import rx.Completable
import rx.Single


data class PostImageRequest (
        val fileName: String,
        val rawData: ByteArray
)

interface ImageService {
    @GET("/image")
    fun getAllImage(): Single<List<Image>>

    @GET("/image/{id}")
    fun getImageById(@Path("id") id: Long): Single<Image>

    @Multipart
    @POST("/image/create")
    fun addImage(@Part file: MultipartBody.Part): Completable
}