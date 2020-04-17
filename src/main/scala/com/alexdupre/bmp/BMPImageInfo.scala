package com.alexdupre.bmp

import java.awt.Color
import java.nio.{ByteBuffer, ByteOrder}
import java.nio.channels.{ReadableByteChannel, WritableByteChannel}
import java.nio.charset.StandardCharsets
import java.util.Arrays

case class BMPImageInfo(
    width: Int,
    height: Int,
    horizPPM: Int,
    vertPPM: Int,
    colorDepth: Int,
    palette: Array[Color] = Array.empty,
    topDown: Boolean = false
) {

  require(Set(1, 4, 8, 16, 24, 32)(colorDepth), "Invalid number of color depth")
  if (colorDepth <= 8)
    require(
      palette.nonEmpty && 1 << colorDepth >= palette.size,
      s"Invalid palette size (${palette.size}) for color depth $colorDepth"
    )
  else require(palette.isEmpty, s"No palette is needed for color depth $colorDepth")

  val paletteMap = palette.zipWithIndex.map { case (c, i) => c -> i.toByte }.toMap
  val rowSize    = (width * colorDepth + 31) / 32 * 4 // rows padded to 32 bits words

  private val paletteHeaderSize = palette.size * 4
  private val metadataTotalSize = BMPImageInfo.FileHeaderSize + BMPImageInfo.DIBHeaderSize + paletteHeaderSize

  private val imageSize  = height.toLong * rowSize
  private val fileSize   = metadataTotalSize + imageSize
  private val isStandard = fileSize <= 0xffffffffL

  def writeHeader(out: WritableByteChannel, allowHuge: Boolean = false): Unit = {
    require(
      isStandard || allowHuge,
      "Image too big to be stored as a standard BMP"
    )

    writeFileHeader(out)
    writeDIBHeader(out)
  }

  private def writeFileHeader(out: WritableByteChannel): Unit = {
    val buf = ByteBuffer.allocate(BMPImageInfo.FileHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put(BMPImageInfo.MagicHeader)
    buf.putInt(if (isStandard) fileSize.toInt else 0)

    buf.putShort(0) // reserved
    buf.putShort(0) // reserved

    buf.putInt(metadataTotalSize) // offset to image data

    out.write(buf.rewind())
  }

  private def writeDIBHeader(out: WritableByteChannel): Unit = {
    val buf = ByteBuffer.allocate(BMPImageInfo.DIBHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.putInt(BMPImageInfo.DIBHeaderSize)

    buf.putInt(width)
    if (topDown) buf.putInt(-height) // top-down
    else buf.putInt(height)          // bottom-up

    buf.putShort(1)                  // always 1 color plane
    buf.putShort(colorDepth.toShort) // bits per pixel
    buf.putInt(0)                    // no compression

    buf.putInt(if (isStandard) imageSize.toInt else 0) // File size (only known at this time if uncompressed!)

    buf.putInt(horizPPM)
    buf.putInt(vertPPM)

    buf.putInt(
      if (colorDepth > 8 || 1 << colorDepth == palette.size) 0
      else palette.size
    )             // all colors used
    buf.putInt(0) // important colors

    out.write(buf.rewind())

    writePalette(out)
  }

  private def writePalette(out: WritableByteChannel): Unit = {
    val buf =
      ByteBuffer.allocate(paletteHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
    palette.foreach(c => buf.putInt(c.getRGB & 0x00ffffff))
    out.write(buf.rewind())
  }

}

object BMPImageInfo {

  private val MagicHeader    = "BM".getBytes(StandardCharsets.US_ASCII)
  private val FileHeaderSize = 14
  private val DIBHeaderSize  = 40

  private def readBuf(in: ReadableByteChannel, len: Int): ByteBuffer = {
    val buf = ByteBuffer.allocate(len).order(ByteOrder.LITTLE_ENDIAN)
    do {
      in.read(buf)
    } while (buf.hasRemaining)
    buf
  }

  private def readFileHeader(in: ReadableByteChannel): Int = {
    val buf = readBuf(in, FileHeaderSize)
    require(Arrays.equals(Array(buf.get(0), buf.get(1)), MagicHeader), "Not a Windows bitmap")
    buf.getInt(10)
  }

  private def readDIBHeader(in: ReadableByteChannel): BMPImageInfo = {
    val buf = readBuf(in, DIBHeaderSize)
    buf.rewind()
    require(buf.getInt() == DIBHeaderSize, "Not a Windows 3.x bitmap")
    val width  = buf.getInt()
    val height = buf.getInt()
    buf.getShort() // color plane
    val colorDepth = buf.getShort
    require(buf.getInt() == 0, "Unsupported compression type")
    buf.getInt() // image size
    val horizPPM = buf.getInt()
    val vertPPM  = buf.getInt()

    val palette = if (colorDepth <= 8) {
      val paletteSize = buf.getInt()
      readPalette(in, if (paletteSize == 0) 1 << colorDepth else paletteSize)
    } else Array.empty[Color]

    val topDown = height < 0
    BMPImageInfo(width, if (topDown) -height else height, horizPPM, vertPPM, colorDepth, palette, topDown)
  }

  private def readPalette(in: ReadableByteChannel, size: Int): Array[Color] = {
    val buf = readBuf(in, size * 4)
    buf.rewind()
    Array.fill(size)(new Color(buf.getInt()))
  }

  def read(in: ReadableByteChannel): BMPImageInfo = {
    val offset = readFileHeader(in)
    val info   = readDIBHeader(in)
    val skip   = offset - info.metadataTotalSize // skip bytes if needed
    if (skip > 0) readBuf(in, skip)
    info
  }
}
