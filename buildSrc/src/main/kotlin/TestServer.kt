import java.net.InetSocketAddress
import java.net.Socket

/** Waits until the local test server accepts connections on its port, for at most ten minutes. */
fun waitForTestServer(port: Int = 25565) {
    val deadline = System.currentTimeMillis() + 10 * 60 * 1000
    println("Waiting for the test server on localhost:$port ...")
    while (System.currentTimeMillis() < deadline) {
        try {
            Socket().use { it.connect(InetSocketAddress("localhost", port), 1000) }
            return
        } catch (e: java.io.IOException) {
            Thread.sleep(1000)
        }
    }
    throw IllegalStateException("The test server did not start on localhost:$port")
}
