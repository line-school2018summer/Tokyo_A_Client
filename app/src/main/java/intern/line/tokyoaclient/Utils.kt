package intern.line.tokyoaclient

import android.content.Context
import android.widget.Toast


fun debugLog(context: Context?, str: String) {
    Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
    println(str)
}