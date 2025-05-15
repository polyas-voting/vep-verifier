package de.polyas.core3.vep.verif.reporting

import java.lang.StringBuilder

/**
 * Formatting context. Used to print out formatted text with structured blocks (indentation).
 *
 * @see lineFormatter
 */
class Fmt(
    private val lineFormatter: (String) -> String = {it}
) {
    operator fun String.unaryPlus() {
        this.lines().forEach { println(lineFormatter(it)) }
    }

    fun indent(lead: String = "    ", block: Fmt.() -> Unit) = decorate(block) { line ->
        "$lead$line"
    }

    fun framed(header: String, block: Fmt.() -> Unit) {
        + "┌── $header"
        indent("│   ") {
            block()
        }
        + "└──"
    }

    fun between(prefix: String, suffix: String, block: Fmt.() -> Unit) {
        decorate(block) {
            "$prefix$it$suffix"
        }
    }

    fun formatted(width: Int, str: String) {
        val words = str.split(" ")
        val out = StringBuilder()
        for (word in words) {
            if (out.length + word.length > width) {
                +out.toString()
                out.clear()
                out.append("$word ")
            }
            else {
                out.append("$word ")
            }
        }
        + out.toString()
    }

    fun underline(block: Fmt.() -> Unit) {
        Fmt { str: String ->
            val u =  "─".repeat(str.length)
            //val u = repeatStr(str.length, "─")
            "${lineFormatter(str)}\n${lineFormatter(u)}"
        }
        .block()
    }

    private fun decorate(block: Fmt.() -> Unit, f:(String) -> String) {
        Fmt { lineFormatter(f(it)) }
            .block()
    }

}

/**
 * Prints out text created with the provided [block]. The [block] uses an instance of [Fmt] as its receiver.
 */
fun fmt(block: Fmt.() -> Unit) {
    Fmt().block()
}



