package intern.line.tokyoaclient.HttpConnection.service

import intern.line.tokyoaclient.HttpConnection.model.Room
import intern.line.tokyoaclient.HttpConnection.model.RoomMember
import retrofit2.http.*
import rx.Completable
import rx.Single

interface RoomService {
    //Room
    @GET("/room")
    fun getAllRoom(): Single<List<Room>>

    @GET("/room/room_id/{room_id}")
    fun findByRoomId(@Path("room_id") roomId: String): Single<Room>

    @POST("/room/create/{room_name}")
    fun addRoom(@Path("room_name") roomName: String): Completable

    @PUT("/room/modify/{room_id}/{room_name}")
    fun modifyroom(@Path("room_id") roomId: String, @Path("room_name") roomName: String): Completable

    @DELETE("/user/delete/{room_id}")
    fun deleteUser(@Path("room_id") roomId: String): Single<Room>

    //Room Member
    @GET("/room_member")
    fun getAllRoomMembers(): Single<List<RoomMember>>

    @GET("/room_member/room_id/{room_id}")
    fun getRoomMembersByRoomId(@Path("room_id") roomId: String): Single<List<RoomMember>>

    @GET("/room_member/uid/{uid}")
    fun getRoomsByUserId(@Path("uid") uid: String): Single<List<RoomMember>>

    @POST("/room_member/create/{room_id}/{uid}")
    fun addRoomMember(@Path("room_id") roomId: String, @Path("uid") uid: String): Completable

    @DELETE("/user/delete/{room_id}")
    fun deleteAllRoomMembers(@Path("room_id") roomId: String): Single<List<RoomMember>>

    @DELETE("/user/delete/{room_id}/{uid}")
    fun deleteMember(@Path("room_id") roomId: String, @Path("uid") uid: String): Single<List<RoomMember>>
}