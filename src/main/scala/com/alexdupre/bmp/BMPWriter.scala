package com.alexdupre.bmp

import java.awt.Color
import java.nio.channels.WritableByteChannel

class BMPWriter(out: WritableByteChannel, info: BMPImageInfo, allowHuge: Boolean = false) {

  private val line    = new BMPImageLine(info)
  private var curLine = 0

  info.writeHeader(out, allowHuge)

  def writeColorLine(row: Array[Color]): Unit = {
    require(curLine < info.height, "All lines have been already written")
    line.writeColorLine(out, row)
    curLine += 1
  }

  def writeIndexLine(row: Array[Byte]): Unit = {
    require(curLine < info.height, "All lines have been already written")
    line.writeIndexLine(out, row)
    curLine += 1
  }

  def close(): Unit = {
    require(curLine == info.height, "Not all lines have been written")
    out.close()
  }

}
