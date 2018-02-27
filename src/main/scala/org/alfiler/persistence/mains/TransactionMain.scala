package org.alfiler.persistence.mains

import akka.actor.ActorSystem
import akka.util.Timeout
import org.alfiler.persistence.account.{Account, AccountState, Deposit, State}
import org.alfiler.persistence.transaction.{DoTransaction, Transaction, TransactionEvents}

object TransactionMain {
  def main(args: Array[String]): Unit = {
    import akka.pattern._
    import scala.concurrent.duration._
    implicit val duration: Timeout = 1.second
    import scala.concurrent.ExecutionContext.Implicits.global


    val system = ActorSystem("singleAccountTest")
    val myAccount = system.actorOf(Account.props("Alfonso"))
    val yourAccount = system.actorOf(Account.props("Juan"))


    myAccount ! Deposit("1", 10)
    yourAccount ! Deposit("2", 10)

    val transaction = system.actorOf(Transaction.props("alf2juan"))

    (myAccount ? State("3")).mapTo[AccountState].foreach(println)
    (yourAccount ? State("4")).mapTo[AccountState].foreach(println)

    Thread.sleep(1.second.toMillis)
    (transaction ? DoTransaction(myAccount, yourAccount, 50, "t")).mapTo[TransactionEvents].foreach(println)

    Thread.sleep(1.second.toMillis)
    (myAccount ? State("5")).mapTo[AccountState].foreach(println)
    (yourAccount ? State("6")).mapTo[AccountState].foreach(println)
  }
}
