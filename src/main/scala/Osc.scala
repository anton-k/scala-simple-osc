import com.illposed.osc._
import java.util.{ArrayList, Date}
import java.net.InetAddress
import scala.util.Try

import collection.JavaConverters._
import scala.collection.JavaConversions._
import resource.Resource

package scala.audio {

    package object osc {
        implicit def oscClientResource[A <: OscClient] = new Resource[A] {
            def close(r: A) = r.close
            override def toString = "Resource[scala.audio.OscClient]"
        }

        implicit def oscServerResource[A <: OscServer] = new Resource[A] {
            def close(r: A) = r.close
            override def toString = "Resource[scala.audio.OscServer"
        }
    }
}

package scala.audio.osc  {

case class OscClient(port: Int, address: InetAddress = InetAddress.getLocalHost()) {
    val client = Try {
        new OSCPortOut(address, port)
    }.toOption

    def channel[A](addr: String)(implicit codec: MessageCodec[A]): Channel[A] = new Channel[A]{
        def send(msg: A) { client.foreach { c =>             
            c.send(new OSCMessage(addr, codec.getArgs(msg)))
        }}
    }

    def close {
        client.foreach(x => x.close)
    }

    def dynamicSend(addr: String, args: List[Object]) {
        client.foreach { c =>
            c.send(new OSCMessage(addr, Util.toArgArray(args)))
        }
    }
}

case class OscServer(port: Int) {
    private val server = Try {
        val res = new OSCPortIn(port)
        res.startListening
        res
    }.toOption

    def listen[A](addr: String)(receive: A => Unit)(implicit codec: MessageCodec[A]) {
        val listener = new OSCListener {
            def acceptMessage(time: Date, msg: OSCMessage) {
                receive(codec.fromOscMessage(msg))
            }
        }
        server.foreach(x => x.addListener(addr, listener))
    }   

    def close {
        server.foreach(_.stopListening)
        server.foreach(_.close)
    }
}

trait Channel[A] {
    def send(msg: A): Unit
}

private object Util {
    def toArgArray(list: List[Object]) = {
        val args = new ArrayList[Object](list.length)
        list.foreach(x => args.add(x))
        args
    }
}

trait MessageCodec[A] {
    def toMessage(a: A): List[Object]
    def fromMessage(xs: List[Object]): (A, List[Object])

    def getArgs(a: A) = Util.toArgArray(toMessage(a))

    def fromOscMessage(msg: OSCMessage): A = fromMessage(msg.getArguments.toList)._1
}

object MessageCodec {

    implicit val floatOscMessageCodec = new MessageCodec[Float] {
        def toMessage(msg: Float) = List(msg.asInstanceOf[java.lang.Float])
        def fromMessage(msg: List[Object]) = (msg.head.asInstanceOf[java.lang.Float].toFloat, msg.tail)
    }

    implicit val intOscMessageCodec = new MessageCodec[Int] {
        def toMessage(msg: Int) = List(msg.asInstanceOf[java.lang.Integer])   
        def fromMessage(msg: List[Object]) = (msg.head.asInstanceOf[java.lang.Integer].toInt, msg.tail)
    }

    implicit val booleanOscMessageCodec = new MessageCodec[Boolean] {
        def toMessage(msg: Boolean) = List((if (msg) 1 else 0).asInstanceOf[java.lang.Integer])
        def fromMessage(msg: List[Object]) = (if (msg.head.asInstanceOf[java.lang.Integer].toInt == 0) false else true, msg.tail)
    }

    implicit val stringOscMessageCodec = new MessageCodec[String] {
        def toMessage(msg: String) = List(msg.asInstanceOf[java.lang.String])
        def fromMessage(msg: List[Object]) = (msg.head.asInstanceOf[java.lang.String].toString, msg.tail)
    }

    implicit def tuple2[A,B](implicit codecA: MessageCodec[A], codecB: MessageCodec[B]) = new MessageCodec[(A,B)] {
        def toMessage(x: (A, B)) = x match {
            case (a, b) => List(codecA.toMessage(a), codecB.toMessage(b)).flatten
        } 

        def fromMessage(msg: List[Object]) = codecA.fromMessage(msg) match {
            case (a, restA) => codecB.fromMessage(restA) match {
                case (b, restB) => ((a, b), restB)
            }
        }
    }

    implicit def tuple3[A,B,C](implicit codecA: MessageCodec[A], codecB: MessageCodec[B], codecC: MessageCodec[C]) = new MessageCodec[(A,B,C)] {
        def toMessage(x: (A, B, C)) = x match {
            case (a, b, c) => List(codecA.toMessage(a), codecB.toMessage(b), codecC.toMessage(c)).flatten
        } 

        def fromMessage(msg: List[Object]) = tuple2[A,B](codecA, codecB).fromMessage(msg) match {
            case ((a, b), rest1) => codecC.fromMessage(rest1) match {
                case (c, rest2) => ((a, b, c), rest2)
            }
        }
    }

    implicit def tuple4[A,B,C,D](implicit codecA: MessageCodec[A], codecB: MessageCodec[B], codecC: MessageCodec[C], codecD: MessageCodec[D]) = new MessageCodec[(A,B,C,D)] {
        def toMessage(x: (A, B, C, D)) = x match {
            case (a, b, c, d) => List(codecA.toMessage(a), codecB.toMessage(b), codecC.toMessage(c), codecD.toMessage(d)).flatten
        } 

        def fromMessage(msg: List[Object]) = tuple3[A,B,C](codecA, codecB, codecC).fromMessage(msg) match {
            case ((a, b, c), rest1) => codecD.fromMessage(rest1) match {
                case (d, rest2) => ((a, b, c, d), rest2)
            }
        }
    }

    implicit def tuple5[A,B,C,D,E](implicit codecA: MessageCodec[A], codecB: MessageCodec[B], codecC: MessageCodec[C], codecD: MessageCodec[D], codecE: MessageCodec[E]) = new MessageCodec[(A,B,C,D,E)] {
        def toMessage(x: (A, B, C, D, E)) = x match {
            case (a, b, c, d, e) => List(codecA.toMessage(a), codecB.toMessage(b), codecC.toMessage(c), codecD.toMessage(d), codecE.toMessage(e)).flatten
        } 

        def fromMessage(msg: List[Object]) = tuple4[A,B,C,D](codecA, codecB, codecC, codecD).fromMessage(msg) match {
            case ((a, b, c, d), rest1) => codecE.fromMessage(rest1) match {
                case (e, rest2) => ((a, b, c, d, e), rest2)
            }
        }
    }

    implicit def tuple6[A,B,C,D,E,F](implicit codecA: MessageCodec[A], codecB: MessageCodec[B], codecC: MessageCodec[C], codecD: MessageCodec[D], codecE: MessageCodec[E], codecF: MessageCodec[F]) = new MessageCodec[(A,B,C,D,E,F)] {
        def toMessage(x: (A, B, C, D, E, F)) = x match {
            case (a, b, c, d, e, f) => List(codecA.toMessage(a), codecB.toMessage(b), codecC.toMessage(c), codecD.toMessage(d), codecE.toMessage(e), codecF.toMessage(f)).flatten
        } 

        def fromMessage(msg: List[Object]) = tuple5[A,B,C,D,E](codecA, codecB, codecC, codecD, codecE).fromMessage(msg) match {
            case ((a, b, c, d, e), rest1) => codecF.fromMessage(rest1) match {
                case (f, rest2) => ((a, b, c, d, e, f), rest2)
            }
        }
    }


    implicit def tuple7[A,B,C,D,E,F,G](implicit codecA: MessageCodec[A], codecB: MessageCodec[B], codecC: MessageCodec[C], codecD: MessageCodec[D], codecE: MessageCodec[E], codecF: MessageCodec[F], codecG: MessageCodec[G]) = new MessageCodec[(A,B,C,D,E,F,G)] {
        def toMessage(x: (A, B, C, D, E, F, G)) = x match {
            case (a, b, c, d, e, f, g) => List(codecA.toMessage(a), codecB.toMessage(b), codecC.toMessage(c), codecD.toMessage(d), codecE.toMessage(e), codecF.toMessage(f), codecG.toMessage(g)).flatten
        }

        def fromMessage(msg: List[Object]) = tuple6[A,B,C,D,E,F](codecA, codecB, codecC, codecD, codecE, codecF).fromMessage(msg) match {
            case ((a, b, c, d, e, f), rest1) => codecG.fromMessage(rest1) match {
                case (g, rest2) => ((a, b, c, d, e, f, g), rest2)
            }
        }
    }

    implicit def tuple8[A,B,C,D,E,F,G,H](implicit codecA: MessageCodec[A], codecB: MessageCodec[B], codecC: MessageCodec[C], codecD: MessageCodec[D], codecE: MessageCodec[E], codecF: MessageCodec[F], codecG: MessageCodec[G], codecH: MessageCodec[H]) = new MessageCodec[(A,B,C,D,E,F,G,H)] {
        def toMessage(x: (A, B, C, D, E, F, G, H)) = x match {
            case (a, b, c, d, e, f, g, h) => List(codecA.toMessage(a), codecB.toMessage(b), codecC.toMessage(c), codecD.toMessage(d), codecE.toMessage(e), codecF.toMessage(f), codecG.toMessage(g), codecH.toMessage(h)).flatten
        }

        def fromMessage(msg: List[Object]) = tuple7[A,B,C,D,E,F,G](codecA, codecB, codecC, codecD, codecE, codecF, codecG).fromMessage(msg) match {
            case ((a, b, c, d, e, f, g), rest1) => codecH.fromMessage(rest1) match {
                case (h, rest2) => ((a, b, c, d, e, f, g, h), rest2)
            }
        }
    }
}

}