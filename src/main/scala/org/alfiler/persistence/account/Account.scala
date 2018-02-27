package org.alfiler.persistence.account

import akka.actor.Props
import akka.persistence.PersistentActor

trait Operation{
  val operationId:String
}

trait AccountCommand extends Operation
case class Withdraw(operationId: String, amount:Int) extends AccountCommand
case class Deposit(operationId: String, amount:Int) extends AccountCommand
case class State(operationId: String) extends AccountCommand

trait AccountEvent extends Operation
case class Withdrawed(operationId: String, amount:Int) extends AccountEvent
case class CantWithdraw(operationId: String, amount:Int, totalAmount:Int) extends AccountEvent
case class Deposited(operationId: String, amount:Int) extends AccountEvent

case class AccountState(id:String, operationId: String, totalAmount:Int) extends AccountEvent

object Account {
  def props(id:String) = Props(new Account(id))
}

class Account(id:String) extends PersistentActor{

  var totalAmount = 0

  private def updateAccount(e:AccountEvent): Unit = {
    e match {
      case Withdrawed(_,amount) => totalAmount = totalAmount - amount
      case Deposited(_,amount) => totalAmount = totalAmount + amount
    }
  }

  override def receiveRecover: Receive = {
    case e:AccountEvent => updateAccount(e)
  }

  override def receiveCommand: Receive = {
    case Withdraw(op, amount) if amount <= totalAmount => persist(Withdrawed(op, amount)){ event =>
      updateAccount(event)
      sender() ! event
      context.system.eventStream.publish(event)
    }
    case Withdraw(op, amount) =>
      val event = CantWithdraw( op, amount, totalAmount)
      sender() ! event
      context.system.eventStream.publish(event)
    case Deposit(op,amount) => persist(Deposited( op, amount)){ event =>
      updateAccount(event)
      sender() ! event
      context.system.eventStream.publish(event)
    }
    case State(op) => sender() ! AccountState(id, op, totalAmount)
  }

  override def persistenceId: String = id
}
