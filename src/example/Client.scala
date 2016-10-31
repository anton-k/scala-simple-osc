import scala.audio.osc.OscClient
import resource._

// It's important to close the OSC-client on exit.
// In this example we use the library scala-arm to manage autoclose.
object UsingAutoCloseClient {
    def main {        
        for (client <- managed(OscClient(7711))) {
            val amp = client.channel[Float]("/amp")
            val cps = client.channel[Float]("/cps")
            val play = client.channel[(Float,Float,Float)]("/play")

            amp.send(0.5f)
            cps.send(0.25f)
        }     
    }
}

// In this example we close the client manually.
object PlainClient {    
    val client = OscClient(7711)

    val amp = client.channel[Float]("/amp")
    val cps = client.channel[Float]("/cps")
    val play = client.channel[(Float,Float,Float)]("/play")

    amp.send(0.5f)
    cps.send(0.25f)
    client.close
}

object App {  

    def main(args: Array[String]) {
        UsingAutoCloseClient.main
        // PlainClient.main
    }
}