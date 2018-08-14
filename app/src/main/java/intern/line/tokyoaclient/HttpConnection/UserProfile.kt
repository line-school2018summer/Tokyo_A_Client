package intern.line.tokyoaclient.HttpConnection

import java.sql.Timestamp

data class UserProfile(
        var id: String = "default",
        var name: String = "unknown",
        var created_at: Timestamp = Timestamp(0L),
        var updated_at: Timestamp = Timestamp(0L)
)
