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
        var roomId: Long = -1,
        var text: String = "Hello, world!",
        var numRead: Long = 0,
        var createdAt: Timestamp = Timestamp(0L),
        var updatedAt: Timestamp = Timestamp(0L)
)

data class Friend(
        var userId: String = "default",
        var friendId: String = "default"
)