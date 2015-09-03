package eu.stratosphere.peel.datagen.flink

import eu.stratosphere.peel.datagen.flink.Distributions._
import eu.stratosphere.peel.datagen.util.RanHash
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem
import org.apache.flink.util.NumberSequenceIterator

object WordGenerator {

  val SEED = 1010

  def main(args: Array[String]): Unit = {

    if (args.length != 6) {
      Console.err.println("Usage: <jar> numberOfWorkers coresPerWorker tuplesPerTask sizeOfDictionary distribution[params] outputPath")
      System.exit(-1)
    }

    val numberOfWorkers       = args(0).toInt
    val coresPerWorker        = args(1).toInt
    val tuplesPerTask         = args(2).toInt
    val sizeOfDictionary      = args(3).toInt
    implicit val distribution = parseDist(sizeOfDictionary, args(4))
    val outputPath            = args(5)

    val dop                   = coresPerWorker * numberOfWorkers
    val N                     = dop * tuplesPerTask

    // generate dictionary of random words
    implicit val dictionary = new Dictionary(SEED, sizeOfDictionary).words()

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

  def word(i: Long)(implicit dictionary: Array[String], distribution: DiscreteDistribution) = {
    dictionary(distribution.sample(new RanHash(SEED + i).next()))
  }

  object Patterns {
    val DiscreteUniform = """(Uniform)""".r
    val Binomial = """Binomial\[(1|1\.0|0\.\d+)\]""".r
    val Zipf = """Zipf\[(\d+(?:\.\d+)?)\]""".r
  }

  def parseDist(card: Int, s: String): DiscreteDistribution = s match {
    case Patterns.DiscreteUniform(_) => DiscreteUniform(card)
    case Patterns.Binomial(a) => Binomial(card, a.toDouble)
    case Patterns.Zipf(a) => Zipf(card, a.toDouble)
  }

}
