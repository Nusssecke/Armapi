package http

class Log {

    companion object {
        fun d(t: String, m: String) {
            println("$t: $m")
        }

        fun e(t: String, m: String) {
            println("$t: $m")
        }

        fun e(t: String, m: String, tr: Throwable) {
            println("$t: $m; ${tr.message}")
        }
    }

}