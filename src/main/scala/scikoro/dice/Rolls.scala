package scikoro.dice

import scala.util.Random

/** Rollable dice pool.
  */
trait Roll {

  protected[this] val random = new Random

  def nextValue: Int = random.nextInt(face) + 1

  /** Number of dices in this dice pool.
    */
  def size: Int

  /** Type of the dice represented by faces it has.
    */
  def face: Int

  /** A function applied to sum of the result of this dice roll when it's resolved.
    */
  def modifier: Int => Int

  /** Expression for modifier used to get a `String` representation of this dice roll.
    */
  def expr: String

  /** A symbol used in a expression.
    */
  val symbol: String = "d"

  /** Generate new [[Roll]] with adding given integer to its result.
    */
  def +(i: Int): Roll

  /** Generate new `Roll` with subtracting given integer from its result.
    */
  def -(i: Int): Roll

  /** Generate new Roll with its pool size increased by given number.
   */
  def ++(incr: Int): Roll

  /** Generate new Roll with its pool size decreased by given number.
   */
  def --(decr: Int): Roll

  /** Attempt to roll all of the dice in this pool
    * and get results as `Seq`.
    */
  def roll: Rolled =
    Rolled.ResultSeq(this, (1 to size).map(_ => nextValue).toList)

  override def toString = s"$size$symbol$face$expr"
}

object Roll {

  case class Impl(
    size: Int,
    face: Int,
    modifier: Int => Int = identity,
    expr: String = ""
  ) extends Roll {

    def +(i: Int) = copy(
      modifier = this.modifier.andThen((_ + i)),
      expr = this.expr + s"+$i"
    )

    def -(i: Int) = copy(
      modifier = this.modifier.andThen((_ - i)),
      expr = this.expr + s"-$i"
    )

    def ++(incr: Int) = copy(size = this.size + incr)

    def --(decr: Int) = {
      val newPoolSize = if (size - decr < 1) 0 else size - decr
      copy(size = newPoolSize)
    }
  }

  def apply(size: Int, face: Int): Roll = {
    require(size > -1, s"The size of dice pool must be at least 1: $size")
    Impl(size, face)
  }
  def apply(size: Int, face: Int, modifier: Int => Int, expr: String): Roll = {
    require(size > -1, s"The size of dice pool must be at least 1: $size")
    Impl(size, face, modifier, expr)
  }

  def four = apply(1, 4)
  def six = apply(1, 6)
  def eight = apply(1, 8)
  def ten = apply(1, 10)
  def twelve = apply(1, 12)
  def twenty = apply(1, 20)
  def percent = apply(1, 100)
  def hundred = percent
}

/** Result of a dice roll.
  */
trait Rolled {

  /** A dice pool made this result */
  def source: Roll

  /** Result values of each die in the source pool */
  def values: Seq[Int]

  /** Take the largest numbers of this result up to `count` */
  def top(count: Int): Seq[Int] = values.sorted.takeRight(count)

  /** Take the lowest numbers of this result up to `count` */
  def least(count: Int): Seq[Int] = values.sorted.take(count)

  /** Sum of the result numbers */
  def total: Int = source.modifier(values.sum)

  /** Make the roll again using its source pool */
  def reroll: Rolled = source.roll

  /** Make the roll again with source pool but preserving old values
   *  if the value satisfies given predicate `p`, or else replacing it
   *  with newly generated random value.
   */
  def rerollBy(p: Int => Boolean): Rolled
}

object Rolled {

  case class ResultSeq(source: Roll, values: Seq[Int]) extends Rolled {

    def rerollBy(p: Int => Boolean) =
      copy(values = this.values.map(v => if(p(v)) source.nextValue else v))

    override def toString = values.mkString("(", ",", ")") + source.expr
  }
}
