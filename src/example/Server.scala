import scala.audio.osc.OscServer
import resource._

// It's important to close the OSC-client on exit.
// In this example we use the library scala-arm to manage autoclose.

object Listeners {
    def appendListeners(server: OscServer) {
        server.listen[Float]("/amp") { x =>
            println(s"/amp ${x}")
        }

        server.listen[Float]("/cps") { x =>
            println(s"/cps ${x}")
        }

        server.listen[(Float, Float, Float)]("/play") { x =>
            println(s"/play ${x}")
        }     
    }
}

object UsingAutoCloseServer {

    def main {
        for (server <- managed(OscServer(7711))) {
            Listeners.appendListeners(server)
            Thread.sleep(10 * 60 * 1000)
        }
    }
}

// In this example we close the server manually.
object PlainServer {
    val server = OscServer(7711)
    Listeners.appendListeners(server)
    Thread.sleep(10 * 60 * 1000)
    server.close
}


object App {  

    def main(args: Array[String]) {
        UsingAutoCloseServer.main
        // PlainServer.main
    }
}