package com.alexdupre.bmp

import java.awt.Color
import java.nio.{ByteBuffer, ByteOrder}

class BMPImageLine(info: BMPImageInfo) {

  private val rawLine = new Array[Byte](info.rowSize)

  private def wrappedRawLine() = ByteBuffer.wrap(rawLine).order(ByteOrder.LITTLE_ENDIAN)

  private lazy val colorLineBuf = new Array[Color](info.width)
  def getColorLine(in: ReaderInterface, sharedBuffer: Boolean = false): Array[Color] = {
    val out = if (sharedBuffer) colorLineBuf else new Array[Color](info.width)
    readColorLine(in, out)
    out
  }

  def readColorLine(in: ReaderInterface, line: Array[Color], offset: Int = 0): Unit = {
    if (info.colorDepth <= 8) {
      val indexes = getIndexLine(in, sharedBuffer = true)
      for (i <- 0 until line.length) line(i + offset) = info.palette(indexes(i) & 0xff)
    } else {
      require(line.length - offset >= info.width, "Output buffer is too small")
      in.read(rawLine)
      val dataLine = wrappedRawLine()
      for (i <- 0 until line.length) line(i + offset) = info.colorDepth match {
        case 32 => new Color(dataLine.getInt())
        case 24 =>
          val b = dataLine.get() & 0xff
          val g = dataLine.get() & 0xff
          val r = dataLine.get() & 0xff
          new Color(r, g, b)
        case 16 =>
          val rgb = dataLine.getShort()
          new Color((rgb >> 10 << 3) & 0xff, (rgb >> 5 << 3) & 0xff, (rgb << 3) & 0xff)
      }
    }
  }

  private lazy val indexLineBuf = new Array[Byte](info.width)
  def getIndexLine(in: ReaderInterface, sharedBuffer: Boolean = false): Array[Byte] = {
    val out = if (sharedBuffer) indexLineBuf else new Array[Byte](info.width)
    readIndexLine(in, out)
    out
  }

  def readIndexLine(in: ReaderInterface, line: Array[Byte], offset: Int = 0): Unit = {
    require(info.colorDepth <= 8, "Invalid color depth")
    require(line.length - offset >= info.width, "Output buffer is too small")
    in.read(rawLine)
    if (info.colorDepth == 8) {
      System.arraycopy(rawLine, 0, line, offset, info.width)
    } else {
      for (i <- 0 until info.width) {
        line(i + offset) = ((rawLine(
          i * info.colorDepth / 8
        ) >> (8 - info.colorDepth - (i * info.colorDepth % 8))) & ((1 << info.colorDepth) - 1)).toByte
      }
    }
  }

  def writeColorLine(out: WriterInterface, row: Array[Color]): Unit = {
    if (info.colorDepth <= 8) writeIndexLine(out, row.map(info.paletteMap))
    else {
      require(row.length == info.width, "Invalid row length")
      val dataLine = wrappedRawLine()
      info.colorDepth match {
        case 32 => row.foreach(c => dataLine.putInt(c.getRGB))
        case 24 =>
          row.foreach { c =>
            dataLine.put(c.getBlue.toByte)
            dataLine.put(c.getGreen.toByte)
            dataLine.put(c.getRed.toByte)
          }
        case 16 =>
          row.foreach { c =>
            val rgb = (c.getRed >> 3 << 10) | (c.getGreen >> 3 << 5) | (c.getBlue >> 3)
            dataLine.putShort(rgb.toShort)
          }
      }
      out.write(rawLine)
    }
  }

  def writeIndexLine(out: WriterInterface, row: Array[Byte]): Unit = {
    require(row.length == info.width, "Invalid row length")
    require(row.forall(_ < info.palette.size), "Invalid palette index")
    require(info.colorDepth <= 8, "Invalid color depth")
    if (info.colorDepth == 8) {
      System.arraycopy(row, 0, rawLine, 0, row.length)
    } else {
      var shi = 8 - info.colorDepth
      var v   = 0
      var off = 0
      for (i <- 0 until info.width) {
        v |= (row(i) << shi)
        shi -= info.colorDepth
        if (shi < 0 || i == info.width - 1) {
          rawLine(off) = v.toByte // (i * info.colorDepth / 8)
          shi = 8 - info.colorDepth
          v = 0
          off += 1
        }
      }
    }
    out.write(rawLine)
  }

}
