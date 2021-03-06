package com.alexdupre.bmp

import java.nio.{ByteBuffer, ByteOrder}

class BMPImageLine(info: BMPImageInfo) {

  private val rawLine = new Array[Byte](info.rowSize)

  private def wrappedRawLine() = ByteBuffer.wrap(rawLine).order(ByteOrder.LITTLE_ENDIAN)

  private lazy val colorLineBuf = new Array[Int](info.width)

  def getColorLine(in: ReaderInterface, sharedBuffer: Boolean = false): Array[Int] = {
    val out = if (sharedBuffer) colorLineBuf else new Array[Int](info.width)
    readColorLine(in, out)
    out
  }

  def readColorLine(in: ReaderInterface, line: Array[Int], offset: Int = 0): Unit = {
    if (info.colorDepth <= 8) {
      val indexes = getIndexLine(in, sharedBuffer = true)
      for (i <- 0 until line.length) line(i + offset) = info.palette(indexes(i) & 0xff)
    } else {
      require(line.length - offset >= info.width, "Output buffer is too small")
      in.read(rawLine)
      val dataLine = wrappedRawLine()
      for (i <- 0 until line.length) line(i + offset) = 0xff000000 | info.colorDepth match {
        case 32 => dataLine.getInt()
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

  def writeColorLine(out: WriterInterface, row: Array[Int]): Unit = {
    if (info.colorDepth <= 8) writeIndexLine(out, row.map(info.paletteMap))
    else {
      require(row.length == info.width, "Invalid row length")
      val dataLine = wrappedRawLine()
      info.colorDepth match {
        case 32 => row.foreach(c => dataLine.putInt(c))
        case 24 =>
          row.foreach { c =>
            dataLine.put(c.toByte)
            dataLine.put((c >> 8).toByte)
            dataLine.put((c >> 16).toByte)
          }
        case 16 =>
          row.foreach { c =>
            val mask = 0x1f << 3
            val r    = c & (mask << 16)
            val g    = c & (mask << 8)
            val b    = c & mask
            val rgb  = (r >> 9) | (g >> 6) | (b >> 3)
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
