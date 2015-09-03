package eu.stratosphere.peel.datagen.flink

import eu.stratosphere.peel.datagen.util.RanHash
import org.apache.commons.math3.distribution.{ZipfDistribution, BinomialDistribution, UniformIntegerDistribution}

object Distributions {

  trait Distribution {
    def sample(rand: RanHash): Double
  }

  case class Gaussian(mu: Double, sigma: Double) extends Distribution {
    def sample(rand: RanHash) = {
      sigma * rand.nextGaussian()
    }
  }

  case class Uniform(k: Int) extends Distribution {
    def sample(rand: RanHash) = {
      Math.floor(rand.next() * k + 0.5).toInt
    }
  }

  case class Pareto(a: Double) extends Distribution {
    def sample(rand: RanHash) = {
      rand.nextPareto(a)
    }
  }

  trait DiscreteDistribution {
    def sample(cumulativeProbability: Double) : Int
  }

  case class DiscreteUniform(k: Int) extends DiscreteDistribution {
    val distribution = new UniformIntegerDistribution(0, k - 1)
    def sample(cp: Double) = distribution.inverseCumulativeProbability(cp)
  }

  case class Binomial(sampleSize: Int, successProbability: Double) extends DiscreteDistribution {
    val distribution = new BinomialDistribution(sampleSize - 1, successProbability)
    def sample(cp: Double) = distribution.inverseCumulativeProbability(cp) - 1
  }

  case class Zipf(sampleSize: Int, exponent: Double) extends DiscreteDistribution {
    val distribution = new ZipfDistribution(sampleSize, exponent)
    def sample(cp: Double) = distribution.inverseCumulativeProbability(cp) - 1
  }
}