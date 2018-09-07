package intern.line.tokyoaclient

class Msg {
    val TYPE_RECEIVED = 0
    val TYPE_SENT = 1
    private val content: String
    private val type: Int
    fun Msg(content: String, type: Int): ??? {
        this.content = content
        this.type = type
    }

    fun getContent(): String {
        return content
    }

    fun getType(): Int {
        return type
    }
}