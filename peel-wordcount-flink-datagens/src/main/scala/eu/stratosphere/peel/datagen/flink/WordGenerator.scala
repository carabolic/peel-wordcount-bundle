package eu.stratosphere.peel.datagen.flink

import eu.stratosphere.peel.datagen.util.RanHash
import org.apache.flink.api.scala._
import org.apache.flink.util.NumberSequenceIterator

import scala.collection.mutable.StringBuilder

object WordGenerator {

  val MAX_LENGTH = 16
  val NUM_CHARACTERS = 26

  def generateWord(seed : Long): String = {
    val ran = new RanHash(seed)
    val length = ran.nextInt(MAX_LENGTH)
    val strBld = new StringBuilder(length)
    for (i <- 0 until length) {
      val c = ('a'.toInt + ran.nextInt(NUM_CHARACTERS)).toChar
      strBld.append(c)
    }
    return strBld.mkString
  }

  def main(args : Array[String]) : Unit = {
    val seed = 1010

    if (args.length != 4) {
      return
    }
    val numberOfWorkers = args(0).toInt
    val coresPerWorker = args(1).toInt
    val tuplesPerTask = args(2).toInt
    val outputPath = args(3)

    val dop = coresPerWorker * numberOfWorkers
    val n = dop * tuplesPerTask

    val env = ExecutionEnvironment.getExecutionEnvironment
    val randomWords = env
      // create a sequence [1 .. N] to create N words
      .fromParallelCollection(new NumberSequenceIterator(1, n))
      // set up workers
      .setParallelism(dop)
      // map every n <- [1 .. N] to a random character string
      .map(i => generateWord(seed + (MAX_LENGTH + 1) * i))

    randomWords.writeAsText(outputPath)
    env.execute()
  }
}
