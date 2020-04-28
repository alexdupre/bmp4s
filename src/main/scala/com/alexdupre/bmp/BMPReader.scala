package com.alexdupre.bmp

import java.awt.Color

class BMPReader(in: ReaderInterface) {

  val info = BMPImageInfo.read(in)

  private val line    = new BMPImageLine(info)
  private var curLine = 0

  def hasNextLine(): Boolean = curLine < info.height

  def getColorLine(sharedBuffer: Boolean = false): Array[Color] = {
    require(hasNextLine(), "All lines have been already read")
    curLine += 1
    line.getColorLine(in, sharedBuffer)
  }

  def readColorLine(out: Array[Color], offset: Int = 0): Unit = {
    require(hasNextLine(), "All lines have been already read")
    curLine += 1
    line.readColorLine(in, out, offset)
  }

  def getIndexLine(sharedBuffer: Boolean = false): Array[Byte] = {
    require(hasNextLine(), "All lines have been already read")
    curLine += 1
    line.getIndexLine(in, sharedBuffer)
  }

  def readIndexLine(out: Array[Byte], offset: Int = 0): Unit = {
    require(hasNextLine(), "All lines have been already read")
    require(out.length - offset >= info.width, "Output buffer is too small")
    curLine += 1
    line.readIndexLine(in, out, offset)
  }

  def close(ignoreUnreadLines: Boolean): Unit = {
    require(ignoreUnreadLines || !hasNextLine(), "Not all lines have been read")
    in.close()
  }

}
