package intern.line.tokyoaclient.HttpConnection

import java.sql.Timestamp

data class UserProfile(
        var id: String,
        var name: String,
        var created_at: Timestamp,
        var updated_at: Timestamp
)
