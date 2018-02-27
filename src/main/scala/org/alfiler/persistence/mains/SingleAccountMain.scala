package org.alfiler.persistence.mains

import akka.actor.{ActorSystem, PoisonPill}
import akka.util.Timeout
import org.alfiler.persistence.account._

import scala.concurrent.duration._

object SingleAccountMain {
  def main(args: Array[String]): Unit = {
    import akka.pattern._
    implicit val duration:Timeout = 1.second
    import scala.concurrent.ExecutionContext.Implicits.global

    val system = ActorSystem("singleAccountTest")
    val myAccount = system.actorOf(Account.props("Alfonso"))

    (myAccount ? Deposit("1", 10)).mapTo[AccountEvent].foreach(println)
    (myAccount ? State).mapTo[AccountState].foreach(println)
    (myAccount ? Withdraw("2", 100)).mapTo[AccountEvent].foreach(println)
    (myAccount ? State).mapTo[AccountState].foreach(println)
    (myAccount ? Withdraw("3", 5)).mapTo[AccountEvent].foreach(println)
    (myAccount ? State).mapTo[AccountState].foreach(println)


    Thread.sleep(1.second.toMillis)
    system.stop(myAccount)

    val myAccount2 = system.actorOf(Account.props("Alfonso"))

    (myAccount2 ? State).mapTo[AccountState].foreach(println)
    (myAccount2 ? Deposit("4", 35)).mapTo[AccountState].foreach(println)

    (myAccount2 ? State).mapTo[AccountState].foreach(println)
  }
}
