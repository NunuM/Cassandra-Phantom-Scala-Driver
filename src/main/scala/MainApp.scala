import com.outworkers.phantom.builder.query.CreateQuery.Default
import com.outworkers.phantom.{CassandraTable, connectors}
import com.outworkers.phantom.connectors.{CassandraConnection, RootConnector}
import com.outworkers.phantom.dsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future => ScalaFuture}
import scala.util.{Failure, Success}

/**
  * Created by nuno on 04-04-2017.
  */
object MainApp extends App {
  val createResult = TweeterDatabase.Tweets.autocreate(TweeterDatabase.connector.provider.space)
  implicit val session = TweeterDatabase.session
  var isFinished = Await.result(createResult.future(), 60.seconds)

  println("Does table tweet created:" + isFinished.wasApplied())

  if (isFinished.wasApplied()) {
    //Insert new Row
    val stored = TweeterDatabase.Tweets.storeTweet(Tweet(java.util.UUID.fromString("4dc23dfe-914a-4954-ac63-0d7059501fa3"), "NunuM", "Hi there"))

    stored.onComplete {
      case Success(element) => {
        val row = TweeterDatabase.Tweets.findTweetByAuthor("NunuM")
        row.onComplete {
          case Success(list) => list.foreach(println)
          case Failure(e) => println("Error", e)
        }
      }
      case Failure(e) => println("Error", e)
    }
  }
}

class TweeterDatabase(override val connector: CassandraConnection)
  extends Database[TweeterDatabase](connector) {

  object Tweets extends TweeterModel with connector.Connector

}

object TweeterDatabase extends TweeterDatabase(connectors.ContactPoint.local.keySpace("tweetapp"))

case class Tweet(id: UUID, author: String, message: String)

abstract class TweeterModel extends CassandraTable[TweeterModel, Tweet] with RootConnector {

  object id extends UUIDColumn(this) with PartitionKey {
    override lazy val name = "id"
  }

  object author extends StringColumn(this) with PartitionKey {
    override lazy val name = "author"
  }

  object message extends StringColumn(this) {
    override lazy val name = "message"
  }

  override lazy val tableName = "tweets"

  def findTweetByAuthor(author: String): ScalaFuture[List[Tweet]] = {
    select.where(_.author eqs author).allowFiltering().fetch()
  }

  def storeTweet(tweet: Tweet): ScalaFuture[ResultSet] = {
    insert()
      .value(_.id, tweet.id)
      .value(_.author, tweet.author)
      .value(_.message, tweet.message)
      .future()
  }

  override def autocreate(keySpace: KeySpace): Default[TweeterModel, Tweet] = {
    create.ifNotExists().`with`(default_time_to_live eqs 10)
      .and(gc_grace_seconds eqs 10.seconds)
      .and(read_repair_chance eqs 0.2)
  }

}
