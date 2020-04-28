package com.alexdupre.bmp

import java.awt.Color

class BMPWriter(out: WriterInterface, info: BMPImageInfo, allowHuge: Boolean = false) {

  private val line    = new BMPImageLine(info)
  private var curLine = 0

  info.writeHeader(out, allowHuge)

  def needsNextLine(): Boolean = curLine < info.height

  def writeColorLine(row: Array[Color]): Unit = {
    require(needsNextLine(), "All lines have been already written")
    line.writeColorLine(out, row)
    curLine += 1
  }

  def writeIndexLine(row: Array[Byte]): Unit = {
    require(needsNextLine(), "All lines have been already written")
    line.writeIndexLine(out, row)
    curLine += 1
  }

  def close(): Unit = {
    require(!needsNextLine(), "Not all lines have been written")
    out.close()
  }

}
