package com.ptixed.kbdprx

import java.io.BufferedReader
import java.io.InputStreamReader

class Keyboard
{
    private var path: String
    var map: ByteArray
    private var onReport: (report: ByteArray) -> Unit

    private var disposed = false

    companion object
    {
        fun open(onReport: (report: ByteArray) -> Unit): Keyboard
        {
            var script = StringBuilder()
            script.appendLine("set -e")
            script.appendLine("cd /sys/class/input/")
            script.appendLine("for f in event*; do")
            script.appendLine("  if [ f == \"\$(cat \$f/device/capabilities/key 2>/dev/null | tail -c3 | head -c1)\" ]; then")
            script.appendLine("    . \$f/uevent")
            script.appendLine("    rm -f /dev/\$DEVNAME")
            script.appendLine("    find \$f/device/device/driver/ -type l | head -1")
            script.appendLine("    xxd -p \$f/device/device/report_descriptor")
            script.appendLine("    exit 0")
            script.appendLine("  fi")
            script.appendLine("done")

            var process = Runtime.getRuntime().exec(arrayOf("su",  "-c",  script.toString()))
            var reader = BufferedReader(InputStreamReader(process.inputStream))

            var file = "([^/]+)$".toRegex().find(reader.readLine())!!.groups[1]!!.value
            var map = ""
            while (true)
            {
                var line = reader.readLine() ?: break
                map += line.trimEnd()
            }

            process.waitFor()
            if (process.exitValue() != 0)
                throw Exception("Process exited with error " + process.exitValue())

            return Keyboard(file, map.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), onReport)
        }
    }

    private constructor(path: String, map: ByteArray, onReport: (report: ByteArray) -> Unit)
    {
        this.path = path
        this.map = map
        this.onReport = onReport

        Thread {
            var regex = "(( [0-9a-f][0-9a-f])+)$".toRegex()
            var process = Runtime.getRuntime().exec(arrayOf("su",  "-c",  "cat /sys/kernel/debug/hid/$path/events"))
            var reader = BufferedReader(InputStreamReader(process.inputStream))
            while (!disposed)
            {
                var line = reader.readLine()
                var result = regex.find(line) ?: continue
                onReport(result.groups[1]!!.value.chunked(3).map { it.trimStart().toInt(16).toByte() }.toByteArray())
            }
            process.destroy()
        }.start()
    }

    fun destroy()
    {
        // TODO it does not seem possible to return the keyboard to Android system...
        disposed = true
    }
}