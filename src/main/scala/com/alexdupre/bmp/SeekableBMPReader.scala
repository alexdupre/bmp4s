package com.alexdupre.bmp

class SeekableBMPReader(in: SeekableInterface) {

  val info = BMPImageInfo.read(in)

  private val line           = new BMPImageSeekableLine(info)
  private val bitmapStartPos = in.position()

  private def goTo(row: Int, col: Int = 0): Unit = {
    require(row >= 0 && row < info.height, "Invalid row number")
    require(col >= 0 && col < info.width, "Invalid column number")
    require(
      info.colorDepth >= 8 || (info.colorDepth == 4 && col % 2 == 0) || (info.colorDepth == 1 && col % 8 == 0),
      "Column number is not byte aligned"
    )
    in.position(bitmapStartPos + row * info.rowSize + col * info.colorDepth / 8)
  }

  def getColorLine(row: Int, sharedBuffer: Boolean = false): Array[Int] = {
    goTo(row, 0)
    line.getColorLine(in, sharedBuffer)
  }

  def getColorLine(row: Int, colStart: Int, length: Int): Array[Int] = {
    goTo(row, colStart)
    line.getColorLine(in, length)
  }

  def readColorLine(row: Int, colStart: Int, length: Int, out: Array[Int], offset: Int = 0): Unit = {
    goTo(row, colStart)
    line.readColorLine(in, out, offset, length)
  }

  def getIndexLine(row: Int, sharedBuffer: Boolean = false): Array[Byte] = {
    goTo(row, 0)
    line.getIndexLine(in, sharedBuffer)
  }
  def getIndexLine(row: Int, colStart: Int, length: Int): Array[Byte] = {
    goTo(row, colStart)
    line.getIndexLine(in, length)
  }

  def readIndexLine(row: Int, colStart: Int, length: Int, out: Array[Byte], offset: Int): Unit = {
    require(colStart + length <= info.width, "Output buffer exceeds ends of line")
    goTo(row, colStart)
    line.readIndexLine(in, out, offset, length)
  }

  def close(): Unit = {
    in.close()
  }

}
