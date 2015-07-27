package eu.stratosphere.peel.datagen.flink

import eu.stratosphere.peel.datagen.util.RanHash
import org.apache.flink.api.scala._
import org.apache.flink.util.NumberSequenceIterator

import scala.io.{Codec, Source}

object WordGenerator {

  val SEED = 1010

  def sampleWord (wordlist : Array[String]) (index : (RanHash, Int) => Int) (seed : Long) (seqNo : Long) : String = {
    val ran = new RanHash(seed)
    ran.skipTo(seqNo)
    wordlist(index(ran, wordlist.length))
  }

  def main(args : Array[String]) : Unit = {

    if (args.length != 4) {
      return
    }
    val numberOfWorkers = args(0).toInt
    val coresPerWorker = args(1).toInt
    val tuplesPerTask = args(2).toInt
    val outputPath = args(3)

    val dop = coresPerWorker * numberOfWorkers
    val n = dop * tuplesPerTask

    // index function
    val distribution : (RanHash, Int) => Int = { (ran, len) => Math.floor(ran.next() * len + 0.5).toInt }

    // load the word list from the resources folder and partially apply the sampleWord function to it
    val wordlist = Source.fromFile(getClass.getResource("/wordlist").getPath, Codec.UTF8.toString()).getLines().toArray
    val word = sampleWord (wordlist) (distribution) (SEED) _

    val env = ExecutionEnvironment.getExecutionEnvironment
    val randomWords = env
      // create a sequence [1 .. N] to create N words
      .fromParallelCollection(new NumberSequenceIterator(1, n))
      // set up workers
      .setParallelism(dop)
      // map every n <- [1 .. N] to a random word sampled from a word list
      .map(i => word(i))

    randomWords.writeAsText(outputPath)
    env.execute()
  }
}
