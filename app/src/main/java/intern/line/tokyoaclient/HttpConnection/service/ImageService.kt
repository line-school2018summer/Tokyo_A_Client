package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.ImageUrl
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import rx.Completable
import rx.Single


interface ImageService {
    @GET("/image")
    fun getAllImageUrl(): Single<ArrayList<ImageUrl>>

    @GET("/image/id/{id}")
    fun getImageUrlById(@Path("id") id: String): Single<ImageUrl>

    @GET("/image/url/{url}")
    fun getImageByUrl(@Path("url") url: String): Single<Response<ByteArray>>

    @Multipart
    @POST("/image/create/{id}")
    fun addImage(@Path("id") id: String, @Part file: MultipartBody.Part): Completable

    @Multipart
    @PUT("/image/modify/{id}")
    fun modifyImage(@Path("id") id: String, @Part file: MultipartBody.Part): Completable

    @DELETE("/image/delete/{id}")
    fun deleteImage(@Path("id") id: String): Completable
}