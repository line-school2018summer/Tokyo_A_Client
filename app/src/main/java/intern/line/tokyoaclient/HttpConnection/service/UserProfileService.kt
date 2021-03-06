package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.UserProfile
import retrofit2.http.*
import rx.Completable
import rx.Single


interface UserProfileService {
    @GET("/user")
    fun getAllUsers(): Single<List<UserProfile>>

    @GET("/user/id/{id}")
    fun getUserById(@Path("id") id: String): Single<UserProfile>

    @GET("/user/name/{name}")
    fun getUserByName(@Path("name") name: String): Single<List<UserProfile>>

    @GET("/user/likelyname/{name}")
    fun getUserByLikelyName(@Path("name") name: String): Single<List<UserProfile>>

    @POST("/user/create/{id}/{name}")
    fun addUser(@Path("id") id: String, @Path("name") name: String): Completable

    @PUT("/user/modify/{id}/{name}")
    fun modifyUser(@Path("id") id: String, @Path("name") name: String): Completable

    @DELETE("/user/delete/{id}")
    fun deleteUser(@Path("id") id: String): Single<UserProfile>
}
