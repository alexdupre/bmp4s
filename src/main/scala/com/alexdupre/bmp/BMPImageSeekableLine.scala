package com.alexdupre.bmp

import java.nio.{ByteBuffer, ByteOrder}

class BMPImageSeekableLine(info: BMPImageInfo) {

  private val rawLine = new Array[Byte](info.rowSize)

  private def wrappedRawLine() = ByteBuffer.wrap(rawLine).order(ByteOrder.LITTLE_ENDIAN)

  private lazy val colorLineBuf = new Array[Int](info.width)

  def getColorLine(in: SeekableInterface, sharedBuffer: Boolean = false): Array[Int] = {
    val out = if (sharedBuffer) colorLineBuf else new Array[Int](info.width)
    readColorLine(in, out, 0, out.length)
    out
  }

  def getColorLine(in: SeekableInterface, length: Int): Array[Int] = {
    val out = new Array[Int](length)
    readColorLine(in, out, 0, length)
    out
  }

  def readColorLine(in: SeekableInterface, line: Array[Int], offset: Int, length: Int): Unit = {
    if (info.colorDepth <= 8) {
      val indexes = getIndexLine(in, length)
      for (i <- 0 until line.length) line(i + offset) = info.palette(indexes(i) & 0xff)
    } else {
      require(line.length - offset >= length, "Output buffer is too small")
      val rawLen = length * info.colorDepth / 8
      in.read(rawLine, 0, rawLen)
      val dataLine = wrappedRawLine()
      for (i <- 0 until line.length) line(i + offset) = info.colorDepth match {
        case 32 => dataLine.getInt() & 0xffffff
        case 24 =>
          val b = dataLine.get() & 0xff
          val g = dataLine.get() & 0xff
          val r = dataLine.get() & 0xff
          (r << 16) | (g << 8) | b
        case 16 =>
          val rgb = dataLine.getShort()
          val r   = (rgb >> 10 << 3) & 0xff
          val g   = (rgb >> 5 << 3) & 0xff
          val b   = (rgb << 3) & 0xff
          (r << 16) | (g << 8) | b

      }
    }
  }

  private lazy val indexLineBuf = new Array[Byte](info.width)

  def getIndexLine(in: SeekableInterface, sharedBuffer: Boolean = false): Array[Byte] = {
    val out = if (sharedBuffer) indexLineBuf else new Array[Byte](info.width)
    readIndexLine(in, out, 0, out.length)
    out
  }

  def getIndexLine(in: SeekableInterface, length: Int): Array[Byte] = {
    val out = new Array[Byte](length)
    readIndexLine(in, out, 0, length)
    out
  }

  def readIndexLine(in: SeekableInterface, line: Array[Byte], offset: Int, length: Int): Unit = {
    require(info.colorDepth <= 8, "Invalid color depth")
    require(line.length - offset >= length, "Output buffer is too small")
    val rawLen = info.colorDepth match {
      case 8 => length
      case 4 => (length + 1) / 2
      case 1 => (length + 7) / 8
    }
    in.read(rawLine, 0, rawLen)
    if (info.colorDepth == 8) {
      System.arraycopy(rawLine, 0, line, offset, length)
    } else {
      for (i <- 0 until length) {
        line(i + offset) = ((rawLine(
          i * info.colorDepth / 8
        ) >> (8 - info.colorDepth - (i * info.colorDepth % 8))) & ((1 << info.colorDepth) - 1)).toByte
      }
    }
  }

}
