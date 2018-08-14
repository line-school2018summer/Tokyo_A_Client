package intern.line.tokyoaclient.HttpConnection

import retrofit2.Call
import retrofit2.http.*


interface IApiService {
    @GET("/user")
    fun getAllUsers(): Call<List<UserProfile>>

    @GET("/user/id/{id}")
    fun getUserById(@Path("id") id: String): Call<UserProfile>

    @GET("/user/name/{name}")
    fun getUserByName(@Path("name") name: String): Call<List<UserProfile>>

    @GET("/user/likelyname/{name}")
    fun getUserByLikelyName(@Path("name") name: String): Call<List<UserProfile>>

    @POST("/user/create/{id}/{name}")
    fun addUser(@Path("id") id: String, @Path("name") name: String): Call<Unit>

    @PUT("/user/modify/{id}/{name}")
    fun modifyUser(@Path("id") id: String, @Path("name") name: String): Call<Unit>

    @DELETE("/user/delete/{id}")
    fun deleteUser(@Path("id") id: String): Call<UserProfile>
}
