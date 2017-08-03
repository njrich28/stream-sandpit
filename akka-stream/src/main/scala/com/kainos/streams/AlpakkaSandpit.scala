package com.kainos.streams

import java.nio.file.{FileSystems, Path}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl.Source
import akka.util.ByteString

object AlpakkaSandpit extends App {

  val path = args(0)
  implicit val system = ActorSystem("alpakka-sandpit")
  implicit val materializer = ActorMaterializer()

//  val fs = FileSystems.getDefault
//  val changes = DirectoryChangesSource(fs.getPath(path), pollInterval = 1.second, maxBufferSize = 1000)
//  changes.runForeach {
//    case (path, change) => println("Path: " + path + ", Change: " + change)
//  }

  val fs = FileSystems.getDefault
  val lines: Source[ByteString, NotUsed] = FileTailSource(
    path = fs.getPath(path),
    maxChunkSize = 8192,
    startingPosition = 0,
    pollingInterval = 250.millis
  )

  lines.runForeach(line => System.out.println(line))
}
