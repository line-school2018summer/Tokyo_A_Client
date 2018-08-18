package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.Talk
import retrofit2.http.*
import rx.Completable
import rx.Single


interface TalkService {
    /* --- talk --- */
    @GET("/talk")
    fun getAllTalk(): Single<List<Talk>>

    @GET("/talk/{roomId}/{sinceTalkId}")
    fun getTalk(@Path("roomId") roomId: Long, @Path("sinceTalkId") sinceTalkId: Long): Single<List<Talk>>

    @POST("/talk/create/{senderId}/{roomId}/{text}")
    fun addTalk(@Path("senderId") senderId: String, @Path("roomId") roomId: Long, @Path("text") text: String): Completable
}