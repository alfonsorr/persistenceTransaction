package org.alfiler.persistence.transaction

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor
import akka.util.Timeout
import org.alfiler.persistence.account._

trait TransactionCommand
case class DoTransaction(idOrig:ActorRef, idDest:ActorRef, amount:Int, operationId: String) extends Operation

trait TransactionEvents extends Operation
case class TransactionStarted(idOrig:ActorRef, idDest:ActorRef, amount:Int, operationId: String, replyTo:ActorRef) extends TransactionEvents
case class OriginSubstracted(amount:Int, operationId: String) extends TransactionEvents
case class TransactionCancel(reason:String, operationId: String) extends TransactionEvents
case class TransactionFinished(operationId: String) extends TransactionEvents



object Transaction{
  def props(id:String):Props = Props(new Transaction(id))
}

class Transaction(id:String) extends PersistentActor{

  import akka.pattern._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout:Timeout = 2.seconds



  var starter:Option[ActorRef] = None
  var origin:Option[ActorRef] = None
  var destiny:Option[ActorRef] = None


  private def updateState(e:TransactionEvents): Unit = e match {
    case TransactionStarted(ori, dest, amount, op, startActor) =>
      starter = Some(startActor)
      origin = Some(ori)
      destiny = Some(dest)
      context.become(started)
    case _:TransactionCancel => context.become(transactionStopped)
    case _:OriginSubstracted => context.become(waitingForDeposit)
    case _:TransactionFinished => context.become(finished)
  }
  override def receiveRecover: Receive = {
    case event:TransactionEvents => updateState(event)
  }

  override def receiveCommand: Receive = {
    case DoTransaction(ori,dest,amount,op) => persist(TransactionStarted(ori, dest, amount, op, sender())){ event =>
      updateState(event)
      starter = Some(sender())
      (ori ? Withdraw(op+1, amount)).pipeTo(self)
    }
  }

  val started:Receive = {
    case Withdrawed(operationId,amount) => persist(OriginSubstracted(amount,operationId)) { event =>
      updateState(event)
      (destiny.get ? Deposit(operationId+2,amount)).pipeTo(self)
    }
    case CantWithdraw(op,_,_) => persist(TransactionCancel("the origin doesn't have enough money", op+1)){event =>
      updateState(event)
      starter.get ! event
    }
  }

  val waitingForDeposit:Receive = {
    case Deposited(op,_) => persist(TransactionFinished(op)){
      event =>
        updateState(event)
        starter.get ! event
    }
  }

  val transactionStopped:Receive = {
    case op:Operation => sender() ! TransactionCancel("cant acept more commands",op.operationId)
  }

  val finished:Receive = {
    case op:Operation => sender() ! TransactionFinished(op.operationId)
  }

  override def persistenceId: String = id
}
