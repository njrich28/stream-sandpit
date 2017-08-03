package com.kainos.streams

import java.nio.file.FileSystems

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Main extends App {

  val PAN = "PAN"

  val filePath = args(0)

  implicit val system = ActorSystem("streams-sandpit")
  implicit val materializer = ActorMaterializer()

  val fs = FileSystems.getDefault
//  val lines: Source[ByteString, NotUsed] = FileTailSource(
//    path = fs.getPath(filePath),
//    maxChunkSize = 8192,
//    startingPosition = 0,
//    pollingInterval = 250.millis
//  )
//  val source: Source[Int, NotUsed] = Source(1 to 100)

  val lines: Source[ByteString, NotUsed] = Source(List(
    ByteString("name,PAN,expiry"),
    ByteString("Homer J Simpson,1234567890123456,0118"),
    ByteString("Bart Simpson,0987654321098765,0219")))

  val rows: Source[Map[String, ByteString], NotUsed] = lines.via(CsvParsing.lineScanner()).via(CsvToMap.toMap())

  val httpFlow = Http().superPool[Map[String, ByteString]]()

  val httpRequests: Source[(HttpRequest, Map[String, ByteString]), NotUsed] = rows map { row =>
    val pan = row(PAN).utf8String
    (HttpRequest(uri = Uri(s"http://localhost:8080/token/$pan")), row)
  }

  val httpResponses = httpRequests.via(httpFlow)

  val tokenisedRows: Source[Map[String, ByteString], NotUsed] = httpResponses.flatMapConcat {
    case (Success(response), row) => response.entity.dataBytes.map(token => row.updated(PAN, token))
    case (Failure(e), _) => throw e
  }

  val tokenisedRowsStringVals = tokenisedRows.map(_.mapValues(_.utf8String))

  val kafkaRecords: Source[ProducerRecord[Array[Byte], String], NotUsed] = tokenisedRowsStringVals map { row =>
    new ProducerRecord[Array[Byte], String]("akka-sandpit", row.toString())
  }

  //val sink = Sink.foreach(println)
  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("192.168.33.10:9092")

  val sink = Producer.plainSink(producerSettings)

  val done = kafkaRecords.runWith(sink)
  implicit val ec = system.dispatcher
  done.onComplete {
    case Success(_) => system.terminate()
    case Failure(e) => e.printStackTrace(); system.terminate()
  }
}
