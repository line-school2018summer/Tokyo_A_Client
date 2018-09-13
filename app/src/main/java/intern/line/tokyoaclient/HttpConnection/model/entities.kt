package intern.line.tokyoaclient.HttpConnection.model

import java.sql.Timestamp

data class UserProfile(
        var id: String = "default",
        var name: String = "unknown",
        var createdAt: Timestamp = Timestamp(0L),
        var updatedAt: Timestamp = Timestamp(0L)
)

data class Talk(
        var talkId: Long = -1,
        var senderId: String = "sender",
        var roomId: String = "room0",
        var text: String = "Hello, world!",
        var numRead: Long = 0,
        var createdAt: Timestamp = Timestamp(0L),
        var updatedAt: Timestamp = Timestamp(0L)
)

data class Friend(
        var userId: String = "",
        var friendId: String = "",
        var createdAt: Timestamp = Timestamp(0L),
        var updatedAt: Timestamp = Timestamp(0L)
)

data class ImageUrl(
        var uid: String,
        var pathToFile: String,
        var createdAt: Timestamp,
        var updatedAt: Timestamp
)

data class Room(
        var roomId: Long = -1,
        var roomName: String = "default",
        var createdAt: Timestamp = Timestamp(0L),
        var updatedAt: Timestamp = Timestamp(0L)
)

data class RoomMember(
        var roomId: Long = -1,
        var uid: String = "",
        var createdAt: Timestamp = Timestamp(0L),
        var updatedAt: Timestamp = Timestamp(0L)
)