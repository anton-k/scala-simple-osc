import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.{Future, Promise}
import resource._

import scala.audio.osc._

object TestUtils {
  def msgThrough[A](client: OscClient, server: OscServer)(addr: String, msg: A)(implicit codec: MessageCodec[A]): Future[Boolean] = {
    val chn = client.channel[A](addr)(codec)
    val isEqual = Promise[Boolean]()
    server.listen[A](addr) { receivedMsg =>
      isEqual.success(receivedMsg == msg)
    }(codec)
    
    chn.send(msg)
    isEqual.future
  }

  def msgListThrough[A](client: OscClient, server: OscServer)(addr: String, msgs: List[A])(implicit codec: MessageCodec[A]) = {
    val chn = client.channel[A](addr)(codec)
    var res = List[A]()

    server.listen[A](addr) { receivedMsg =>
      res = receivedMsg :: res
    }

    msgs.foreach( x => { Thread.sleep(1); chn.send(x) })
    Thread.sleep(5)
    if (res.length < 10) {
        println(s"${msgs.reverse} == ${res}")
    }    
    msgs.reverse == res
  }
}

class ThroughTest extends FunSuite with ScalaFutures with BeforeAndAfterAll {
  import TestUtils._

  val port = 7719
  val client = OscClient(port)
  val server = OscServer(port)

  val strings = List("Hello!", "a", "b", "c", "", "World!")
  val floats  = List(0.1f, 0.2f, 1, 3, 5, 10)
  val booleans = List(true, false)
  val pairs = for (a <- strings; b <- strings) yield (a, b)
  val tripples = for ((a, b) <- pairs; c <- booleans) yield (a, b, c)
  val quartets = for ((a, b, c) <- tripples; d <- floats) yield (a, b, c, d)
  val nestedPairs = for (a <- pairs; b <- tripples) yield (a, b)


  test("String primitive") {    
    assert(msgListThrough[String](client, server)("/string", strings))
  }

  test("Float primitive") {    
    assert(msgListThrough[Float](client, server)("/float", floats))
  }

  test("Boolean primitive") {
    assert(msgListThrough[Boolean](client, server)("/boolean", booleans))
  }

  test("Check pairs") {
    assert(msgListThrough[(String, String)](client, server)("/tuple2", pairs))
  }

  test("Check tripples") {
    assert(msgListThrough[(String, String, Boolean)](client, server)("/tuple3", tripples))
  }  

  test("Check quartets") {
    assert(msgListThrough[(String, String, Boolean, Float)](client, server)("/tuple4", quartets))
  }  

  test("Check nested pairs") {
    assert(msgListThrough[((String, String), (String, String, Boolean))](client, server)("/tuple2_nested", nestedPairs))
  }

  override def afterAll() {
    client.close
    server.close
  }
}



class AutocloseTest extends FunSuite {
    import TestUtils._

    val port = 7718

    def testScript = {
        var res = false
        for {            
            client <- managed(OscClient(port))
            server <- managed(OscServer(port)) } {

            res = msgListThrough[String](client, server)("/string", List("Hello"))
        }
        res
    }

    test("Run the same script several times on the same port") {
        List(testScript, testScript, testScript).forall(x => x)
    }
}