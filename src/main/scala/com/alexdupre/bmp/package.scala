package com.alexdupre

import java.io.{EOFException, InputStream, OutputStream, RandomAccessFile}
import java.nio.ByteBuffer
import java.nio.channels.{ReadableByteChannel, SeekableByteChannel, WritableByteChannel}

import scala.language.implicitConversions

package object bmp {

  trait ReaderInterface extends AutoCloseable {
    def read(buf: Array[Byte]): Unit = read(buf, 0, buf.length)
    def read(buf: Array[Byte], offset: Int, length: Int): Unit
  }

  trait WriterInterface extends AutoCloseable {
    def write(buf: Array[Byte]): Unit
  }

  trait SeekableInterface extends ReaderInterface {
    def position(): Long
    def position(i: Long): Unit
  }

  implicit def InputStreamToReaderInterface(in: InputStream): ReaderInterface =
    new ReaderInterface {
      override def read(buf: Array[Byte], offset: Int, length: Int): Unit = {
        var n = 0
        while (n < length) {
          val count = in.read(buf, offset + n, length - n)
          if (count < 0) throw new EOFException
          n += count
        }
      }
      override def close(): Unit = in.close()
    }

  implicit def ReadableByteChannelToReaderInterface(in: ReadableByteChannel): ReaderInterface =
    new ReaderInterface {
      override def read(buf: Array[Byte], offset: Int, length: Int): Unit = {
        val bb = ByteBuffer.wrap(buf, offset, length)
        do {
          in.read(bb)
        } while (bb.hasRemaining)
      }
      override def close(): Unit = in.close()
    }

  implicit def RandomAccessFileToSeekableInterface(in: RandomAccessFile): SeekableInterface =
    new SeekableInterface {
      override def position(): Long                                       = in.getFilePointer()
      override def position(i: Long): Unit                                = in.seek(i)
      override def read(buf: Array[Byte], offset: Int, length: Int): Unit = in.readFully(buf, offset, length)
      override def close(): Unit                                          = in.close()
    }

  implicit def SeekableByteChannelToSeekableInterface(in: SeekableByteChannel): SeekableInterface =
    new SeekableInterface {
      override def position(): Long        = in.position()
      override def position(i: Long): Unit = in.position(i)
      override def read(buf: Array[Byte], offset: Int, length: Int): Unit = {
        val bb = ByteBuffer.wrap(buf, offset, length)
        do {
          in.read(bb)
        } while (bb.hasRemaining)
      }
      override def close(): Unit = in.close()
    }

  implicit def OutputStreamToWriterInterface(out: OutputStream): WriterInterface =
    new WriterInterface {
      override def write(buf: Array[Byte]): Unit = out.write(buf)
      override def close(): Unit                 = out.close()
    }

  implicit def WritableByteChannelToWriterInterface(out: WritableByteChannel): WriterInterface =
    new WriterInterface {
      override def write(buf: Array[Byte]): Unit = out.write(ByteBuffer.wrap(buf))
      override def close(): Unit                 = out.close()
    }

}
