package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.Image
import retrofit2.http.*
import rx.Completable
import rx.Single


data class Request (
        val rawData: ByteArray
)

interface ImageService {
    @GET("/image")
    fun getAllImage(): Single<List<Image>>

    @GET("/image/{id}")
    fun getImageById(@Path("id") id: Long): Single<Image>

    @POST("/image/create/{fileName}/{rawData}")
    fun addImage(@Path("fileName") fileName: String, @Path("rawData") rawData: ByteArray): Completable
}