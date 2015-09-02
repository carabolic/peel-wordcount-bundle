package eu.stratosphere.peel.datagen.flink

import eu.stratosphere.peel.datagen.flink.Distributions.{Gaussian, Pareto, Uniform, Distribution}
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
    implicit val distribution = parseDist(args(4))
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

  def word(i: Long)(implicit dictionary: Array[String], distribution: Distribution) = {
    dictionary(distribution.sample(new RanHash(SEED + i)).toInt)
  }

  object Patterns {
    val Uniform = """Uniform\[(\d+)\]""".r
    val Gaussian = """Gaussian\[(\d+),(\d+)\]""".r
    val Pareto = """Pareto\[(\d+)\]""".r
  }

  def parseDist(s: String): Distribution = s match {
    case Patterns.Pareto(a) => Pareto(a.toDouble)
    case Patterns.Gaussian(a, b) => Gaussian(a.toDouble, b.toDouble)
    case Patterns.Uniform(a) => Uniform(a.toInt)
    case _ => Pareto(1)
  }

}
