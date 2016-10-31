OSC library for Scala which is based on [JavaOSC](https://github.com/hoijui/JavaOSC)

OSC protocol is designed to control audio instruments. It's a network protocol. We can create clients and servers on certain ports
and exchange the data beetween them. Library supports automatic resource managment with [scala-arm](https://github.com/jsuereth/scala-arm) library.
We need to close the client and server to release the port.

### Client example

We can create the clients to send the messages to certain ports with constructor:

~~~scala
val client = OscClient(7711)
~~~
By default the host is local but we can also send the messages to other computer in the network
if we specify `java.net.InetAddress` as a second parameter to constructor.

With OSC protocol we send and receive values by certain addresses.
To send the message we create a typed channel for a given address (specified with unix path-like string):

~~~scala
val c = client.channel[Float]("/velocity")
~~~

We can use booleans, strings, integers, floats and tuples of them (up to 8 elements if we want more we
can use nested tuples). 

Then we can use the channel to send the values:

~~~scala
c.send(1.2f)
~~~

At the end of the program we should close the client:

~~~scala
client.close
~~~

Complete example:

~~~scala
import scala.audio.osc._

object PlainClient {    
    val client = OscClient(7711)

    val amp = client.channel[Float]("/amp")
    val cps = client.channel[Float]("/cps")
    val play = client.channel[(String,Float,Float)]("/play")

    def run {
        amp.send(0.5f)
        cps.send(0.25f)
        play.send(("start", 0.2f, 0.3f))
        client.close

    }    
}

object App {

    def main(args: Array[String]) {
        PlainClient.run
    }
}
~~~

Also the library has support for automatic resource managment with [scala-arm](https://github.com/jsuereth/scala-arm) library.
Here is example with automatic close:

~~~scala
import scala.audio.osc.OscClient
import resource._

object App {
   
   def main(args: Array[String]) {
        val port = 7711

        for (client <- managed(OscClient(port))) {
            val amp = client.channel[Float]("/amp")
            val cps = client.channel[Float]("/cps")
            val play = client.channel[(Float,Float,Float)]("/play")

            amp.send(0.5f)
            cps.send(0.25f)
        }
   }
}
~~~

### Server example

Server waits for messages on the certain port:

~~~scala
val server = OscServer(7711)
~~~

We can register callbacks for the addresses:

~~~scala
server.listen[Float]("/cps") { x =>
    println(s"/cps ${x}")
}
~~~

When we are done with our job we should close the server:

~~~scala
server.close
~~~

Complete example:

~~~scala
import scala.audio.osc.OscServer

object App {
    val server = OscServer(7711)
    
    def main(args: Array[String]) {
        server.listen[Float]("/amp") { x =>
            println(s"/amp ${x}")
        }

        server.listen[Float]("/cps") { x =>
            println(s"/cps ${x}")
        }

        // wait for messages for 10 minutes
        Thread.sleep(10 * 60 * 1000)
        server.close
    }
}
~~~

Servers also hav support for automatic resource managmnet (see the [scala-arm](https://github.com/jsuereth/scala-arm) library for details):

~~~scala
import scala.audio.osc.OscServer
import resource._

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

object App
    def main {
        for (server <- managed(OscServer(7711))) {
            Listeners.appendListeners(server)
            Thread.sleep(10 * 60 * 1000)
        }
    }
}
~~~

### Issues

The library is a wrapper around JavaOSC library so along its merits it can also inherent 
some flows. The noticed issue so far is that we can not end too many messages at the same moment
so it's better to introduce the delays between messages if we want the server to keep all the messages.