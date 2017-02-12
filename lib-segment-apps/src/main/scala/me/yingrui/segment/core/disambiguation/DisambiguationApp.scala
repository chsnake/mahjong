package me.yingrui.segment.core.disambiguation

import java.io.{BufferedReader, InputStreamReader}

import me.yingrui.segment.conf.MPSegmentConfiguration
import me.yingrui.segment.core.SegmentWorker
import me.yingrui.segment.crf.{CRFClassifier, CRFCorpus, CRFModel, CRFViterbi}
import me.yingrui.segment.filter.SegmentResultFilter
import me.yingrui.segment.filter.disambiguation.CRFDisambiguationFilter
import me.yingrui.segment.tools.accurary.SegmentAccuracy
import me.yingrui.segment.tools.accurary.SegmentErrorType._

object DisambiguationApp extends App {
  val trainFile = if (args.indexOf("--train-file") >= 0) args(args.indexOf("--train-file") + 1) else "disambiguation-corpus.txt"
  val saveFile = if (args.indexOf("--save-file") >= 0) args(args.indexOf("--save-file") + 1) else "disambiguation.m"
  val debug = args.indexOf("--debug") >= 0

  println("model loading...")
  val model = CRFModel(saveFile)
  println("model loaded...")
//  closeTest(model, trainFile)

  val filter = new SegmentResultFilter(MPSegmentConfiguration())
  filter.addFilter(new CRFDisambiguationFilter(new CRFClassifier(model)))
  val segmentWorker = SegmentWorker(Map[String, String](), filter)

  val segmentAccuracy = new SegmentAccuracy("./lib-segment/src/test/resources/PFR-199801-utf-8.txt", segmentWorker)
  segmentAccuracy.checkSegmentAccuracy()
  println("Accuracy rate of segment is: " + segmentAccuracy.getAccuracyRate())
  println("There are " + segmentAccuracy.getWrong() + " errors and total expect word is " + segmentAccuracy.getTotalWords() + " when doing accuracy test.")

  println("There are " + segmentAccuracy.getErrorAnalyzer(UnknownWord).getErrorOccurTimes() + " errors because of new word.")
  println("There are " + segmentAccuracy.getErrorAnalyzer(NER_NR).getErrorOccurTimes() + " errors because of name recognition.")
  println("There are " + segmentAccuracy.getErrorAnalyzer(NER_NS).getErrorOccurTimes() + " errors because of place name recognition.")
  println("There are " + segmentAccuracy.getErrorAnalyzer(ContainDisambiguate).getErrorOccurTimes() + " errors because of contain disambiguate.")
  println("There are " + segmentAccuracy.getErrorAnalyzer(Other).getErrorOccurTimes() + " other errors")

  println("\nType QUIT to exit:")
  val inputReader = new BufferedReader(new InputStreamReader(System.in))
  var line = inputReader.readLine()
  while (line != null && !line.equals("QUIT")) {
    if (!line.isEmpty) {
      val result = segmentWorker.tokenize(line)
      println(result.mkString(" "))
    }
    line = inputReader.readLine()
  }

  def closeTest(model: CRFModel, trainFile: String): Unit = {
    var total = 0
    var correctCount = 0
    val corpus = CRFCorpus(trainFile, false, true, model.featureRepository, model.labelRepository)
    println("test corpus loaded")
    val classifier = new CRFViterbi(model)
    var index = 0D
    corpus.docs.foreach(doc => {
      val result = classifier.calculateResult(doc.data).getBestPath
      total += result.length
      var success = true
      for (index <- 0 until result.length) yield {
        val label = doc.label(index)
        correctCount += (if (label == result(index)) 1 else 0)

        if (label != result(index)) success = false
      }

      if (!success) {
        try {
          val errors = (0 until result.length)
            .map(index => {
              def isCurrentOrNextFailed(currentIndex: Int) = {
                val line = doc.rowData(currentIndex)
                val label = model.labelRepository.getFeature(result(currentIndex))
                val failed = !line.endsWith(label)
                if (!failed && currentIndex < result.length - 1) {
                  val nextLine = doc.rowData(currentIndex + 1)
                  val nextLabel = model.labelRepository.getFeature(result(currentIndex + 1))
                  !nextLine.endsWith(nextLabel)
                } else {
                  failed
                }
              }

              if (isCurrentOrNextFailed(index)) {
                doc.rowData(index) + " " + model.labelRepository.getFeature(result(index)) + " --"
              } else {
                doc.rowData(index) + " " + model.labelRepository.getFeature(result(index))
              }
            })
          if (debug) {
            errors.foreach(println(_))
            println()
          }
        } catch {
          case _: Exception =>
        }
      }

      if (!debug) {
        index = index + 1
        val progress = (index / corpus.docs.size.toDouble * 100D).toInt
        print(s"\rProgress: $progress %")
      }
    })
    println("\ntotal: " + total + " correct: " + correctCount + " error: " + (total - correctCount) + " rate: " + correctCount.toDouble / total.toDouble)
  }

}
