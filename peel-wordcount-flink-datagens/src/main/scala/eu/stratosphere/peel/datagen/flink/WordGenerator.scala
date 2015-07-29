package eu.stratosphere.peel.datagen.flink

import eu.stratosphere.peel.datagen.util.RanHash
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem
import org.apache.flink.util.NumberSequenceIterator

object WordGenerator {

  type Distribution = (RanHash, Int) => Int

  val SEED = 1010

  def main(args: Array[String]): Unit = {

    if (args.length != 5) {
      Console.err.println("Usage: <jar> sizeOfDictionary numberOfWorkers coresPerWorker tuplesPerTask outputPath")
      System.exit(-1)
    }

    val sizeOfDictionary = args(0).toInt
    val numberOfWorkers  = args(1).toInt
    val coresPerWorker   = args(2).toInt
    val tuplesPerTask    = args(3).toInt
    val outputPath       = args(4)

    // TODO add distribution and dictionarySize parameters

    val dop              = coresPerWorker * numberOfWorkers
    val N                = dop * tuplesPerTask

    // generate dictionary of random words
    implicit val dictionary = new Dictionary(SEED, sizeOfDictionary).words

    implicit val distribution = uniform _ // TODO pick from 'uniform _', 'pareto _' or 'gaussian _'

    val environment = ExecutionEnvironment.getExecutionEnvironment

    environment
      // create a sequence [1 .. N] to create N words
      .fromParallelCollection(new NumberSequenceIterator(1, N))
      // set up workers
      .setParallelism(dop)
      // map every n <- [1 .. N] to a random word sampled from a word list
      .map(i => word(i))
      // write result to file
      .writeAsText(outputPath, FileSystem.WriteMode.OVERWRITE)
    environment.execute(s"WordGenerator[$N]")
  }

  def word(i: Long)(implicit dictionary: Array[String], distribution: Distribution) = {
    dictionary(distribution(new RanHash(SEED + i), dictionary.length))
  }

  // index function
  def uniform(ran: RanHash, len: Int): Int = {
    Math.floor(ran.next() * (len - 1) + 0.5).toInt
  }
}