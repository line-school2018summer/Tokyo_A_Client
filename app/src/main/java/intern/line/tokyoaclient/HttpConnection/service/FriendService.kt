package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.Friend
import retrofit2.http.*
import rx.Completable
import rx.Single


interface FriendService {
    @GET("/friend")
    fun getAllFriends(): Single<List<Friend>>

    @GET("/friend/{userId}")
    fun getFriendById(@Path("userId") userId: String): Single<List<Friend>>

    @POST("/friend/add/{userId}/{friendId}")
    fun addFriend(@Path("userId") userId: String, @Path("friendId") friendId: String): Completable
}