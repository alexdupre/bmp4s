package com.alexdupre.bmp

import java.awt.Color

class SeekableBMPReader(in: SeekableInterface) {

  val info = BMPImageInfo.read(in)

  private val line           = new BMPImageLine(info)
  private val bitmapStartPos = in.position()

  private def goToLine(n: Int): Unit = {
    require(n >= 0 && n < info.height, "Invalid line number")
    in.position(bitmapStartPos + n * info.rowSize)
  }

  def getColorLine(n: Int, sharedBuffer: Boolean = false): Array[Color] = {
    goToLine(n)
    line.getColorLine(in, sharedBuffer)
  }

  def readColorLine(n: Int, out: Array[Color], offset: Int = 0): Unit = {
    goToLine(n)
    line.readColorLine(in, out, offset)
  }

  def getIndexLine(n: Int, sharedBuffer: Boolean = false): Array[Byte] = {
    goToLine(n)
    line.getIndexLine(in, sharedBuffer)
  }

  def readIndexLine(n: Int, out: Array[Byte], offset: Int = 0): Unit = {
    goToLine(n)
    require(out.length - offset >= info.width, "Output buffer is too small")
    line.readIndexLine(in, out, offset)
  }

  def close(): Unit = {
    in.close()
  }

}
