package com.alexdupre.bmp

import java.awt.Color
import java.nio.channels.{ReadableByteChannel, WritableByteChannel}
import java.nio.{ByteBuffer, ByteOrder}

class BMPImageLine(info: BMPImageInfo) {

  private val rawLine = ByteBuffer
    .allocate(info.rowSize)
    .order(ByteOrder.LITTLE_ENDIAN)

  private def readLine(in: ReadableByteChannel): Unit = {
    rawLine.clear()
    do {
      in.read(rawLine)
    } while (rawLine.hasRemaining)
  }

  private lazy val colorLineBuf = new Array[Color](info.width)
  def getColorLine(in: ReadableByteChannel, sharedBuffer: Boolean = false): Array[Color] = {
    val out = if (sharedBuffer) colorLineBuf else new Array[Color](info.width)
    readColorLine(in, out)
    out
  }

  def readColorLine(in: ReadableByteChannel, line: Array[Color], offset: Int = 0): Unit = {
    if (info.colorDepth <= 8) {
      val indexes = getIndexLine(in, sharedBuffer = true)
      for (i <- 0 until line.length) line(i + offset) = info.palette(indexes(i) & 0xff)
    } else {
      require(line.length - offset >= info.width, "Output buffer is too small")
      readLine(in)
      rawLine.rewind()
      for (i <- 0 until line.length) line(i + offset) = info.colorDepth match {
        case 32 => new Color(rawLine.getInt())
        case 24 =>
          val b = rawLine.get() & 0xff
          val g = rawLine.get() & 0xff
          val r = rawLine.get() & 0xff
          new Color(r, g, b)
        case 16 =>
          val rgb = rawLine.getShort()
          new Color((rgb >> 10 << 3) & 0xff, (rgb >> 5 << 3) & 0xff, (rgb << 3) & 0xff)
      }
    }
  }

  private lazy val indexLineBuf = new Array[Byte](info.width)
  def getIndexLine(in: ReadableByteChannel, sharedBuffer: Boolean = false): Array[Byte] = {
    val out = if (sharedBuffer) indexLineBuf else new Array[Byte](info.width)
    readIndexLine(in, out)
    out
  }

  def readIndexLine(in: ReadableByteChannel, line: Array[Byte], offset: Int = 0): Unit = {
    require(info.colorDepth <= 8, "Invalid color depth")
    require(line.length - offset >= info.width, "Output buffer is too small")
    readLine(in)
    val buf = rawLine.array()
    if (info.colorDepth == 8) {
      System.arraycopy(buf, 0, line, offset, info.width)
    } else {
      for (i <- 0 until info.width) {
        line(i + offset) = (((buf(
          i * info.colorDepth / 8
        ) >> (8 - info.colorDepth - (i * info.colorDepth % 8))) & ((1 << info.colorDepth) - 1))).toByte
      }
    }
  }

  def writeColorLine(out: WritableByteChannel, row: Array[Color]): Unit = {
    if (info.colorDepth <= 8) writeIndexLine(out, row.map(info.paletteMap))
    else {
      require(row.length == info.width, "Invalid row length")
      rawLine.clear()
      info.colorDepth match {
        case 32 => row.foreach(c => rawLine.putInt(c.getRGB))
        case 24 =>
          row.foreach { c =>
            rawLine.put(c.getBlue.toByte)
            rawLine.put(c.getGreen.toByte)
            rawLine.put(c.getRed.toByte)
          }
        case 16 =>
          row.foreach { c =>
            val rgb = (c.getRed >> 3 << 10) | (c.getGreen >> 3 << 5) | (c.getBlue >> 3)
            rawLine.putShort(rgb.toShort)
          }
      }
      out.write(rawLine.rewind())
    }
  }

  def writeIndexLine(out: WritableByteChannel, row: Array[Byte]): Unit = {
    require(row.length == info.width, "Invalid row length")
    require(row.forall(_ < info.palette.size), "Invalid palette index")
    require(info.colorDepth <= 8, "Invalid color depth")
    rawLine.clear()
    if (info.colorDepth == 8) {
      rawLine.put(row)
    } else {
      var shi = 8 - info.colorDepth
      var v   = 0
      for (i <- 0 until info.width) {
        v |= (row(i) << shi)
        shi -= info.colorDepth
        if (shi < 0 || i == info.width - 1) {
          rawLine.put(v.toByte)
          shi = 8 - info.colorDepth
          v = 0
        }
      }
    }
    out.write(rawLine.rewind())
  }

}
