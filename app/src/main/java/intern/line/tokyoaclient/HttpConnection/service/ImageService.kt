package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.Image
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import rx.Completable
import rx.Single


data class GetImageUrlResponse (
        val url: String
)
data class GetImageResponse (
        var data: ByteArray
)

interface ImageService {
    @GET("/image/id/{id}")
    fun getImageUrlById(@Path("id") id: String): Single<GetImageUrlResponse>

    @GET("/image/url/{url}")
    fun getImageByUrl(@Path("url") url: String): Single<Response<ByteArray>>

    @Multipart
    @POST("/image/create/{id}")
    fun addImage(@Path("id") id: String, @Part file: MultipartBody.Part): Completable
}