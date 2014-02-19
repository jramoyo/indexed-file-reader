/*
 * Copyright 2012 Jan Amoyo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * BufferedRandomAccessFile.java
 * 8 Nov 2012 
 */
package com.jramoyo.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**
 * BufferedRandomAccessFile
 * <p>
 * Extends <code>RandomAccessFile</code> to support buffered reads.
 * </p>
 * 
 * @see http://www.javaworld.com/javaworld/javatips/jw-javatip26.html
 * @author jramoyo
 */
public class BufferedRandomAccessFile extends RandomAccessFile {
	private static final int DEFAULT_BUFFER_SIZE = 256;

	private final int bufferSize;

	private byte buffer[];
	private int bufferEnd = 0;
	private int bufferPos = 0;
	private long realPos = 0L;

	/**
	 * Creates a BufferedRandomAccessFile
	 * 
	 * @param file
	 *            the file object
	 * @param mode
	 *            the access mode
	 * @throws IOException
	 */
	public BufferedRandomAccessFile(File file, String mode) throws IOException {
		this(file, mode, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a BufferedRandomAccessFile
	 * 
	 * @param file
	 *            the file object
	 * @param mode
	 *            the access mode
	 * @param bufferSize
	 *            the size of the read buffer
	 * @throws IOException
	 */
	public BufferedRandomAccessFile(File file, String mode, int bufferSize)
			throws IOException {
		super(file, mode);
		invalidate();
		this.bufferSize = bufferSize;
		this.buffer = new byte[bufferSize];
	}

	/**
	 * Creates a BufferedRandomAccessFile.
	 * 
	 * @param filename
	 *            the path to the file
	 * @param mode
	 *            the access mode
	 * @throws IOException
	 */
	public BufferedRandomAccessFile(String filename, String mode)
			throws IOException {
		this(filename, mode, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a BufferedRandomAccessFile.
	 * 
	 * @param filename
	 *            the path to the file
	 * @param mode
	 *            the access mode
	 * @param bufsize
	 *            the size of the read buffer
	 * @throws IOException
	 */
	public BufferedRandomAccessFile(String filename, String mode, int bufsize)
			throws IOException {
		super(filename, mode);
		invalidate();
		this.bufferSize = bufsize;
		this.buffer = new byte[bufsize];
	}

	/**
	 * Returns the current offset in this file.
	 * 
	 * @return the offset from the beginning of the file, in bytes, at which the
	 *         next read or write occurs.
	 */
	@Override
	public long getFilePointer() throws IOException {
		return (realPos - bufferEnd) + bufferPos;
	}

	/**
	 * <p>
	 * Reads the next line of text from this file using the default character
	 * set.
	 * </p>
	 * <p>
	 * Unlike <code>readLine</code>, this method reads bytes from the buffer.
	 * </p>
	 * <p>
	 * A line of text is terminated by a carriage-return character ('\r'), a
	 * newline character ('\n'), a carriage-return character immediately
	 * followed by a newline character, or the end of the file. Line-terminating
	 * characters are discarded and are not included as part of the string
	 * returned.
	 * </p>
	 * 
	 * @return the next line of text from this file, or null if end of file is
	 *         encountered before even one byte is read.
	 * @throws IOException
	 */
	public final String getNextLine() throws IOException {
		return getNextLine(Charset.defaultCharset());
	}

	/**
	 * <p>
	 * Reads the next line of text from this file using the specified character
	 * set.
	 * </p>
	 * <p>
	 * Unlike <code>readLine</code>, this method reads bytes from the buffer.
	 * </p>
	 * <p>
	 * A line of text is terminated by a carriage-return character ('\r'), a
	 * newline character ('\n'), a carriage-return character immediately
	 * followed by a newline character, or the end of the file. Line-terminating
	 * characters are discarded and are not included as part of the string
	 * returned.
	 * </p>
	 * 
	 * @param charset
	 *            the character set to use when reading the line
	 * @return the next line of text from this file, or null if end of file is
	 *         encountered before even one byte is read.
	 * @throws IOException
	 */
	public final String getNextLine(Charset charset) throws IOException {
		String str = null;

		// Fill the buffer
		if (bufferEnd - bufferPos <= 0) {
			if (fillBuffer() < 0) {
				return null;
			}
		}

		// Find line terminator from buffer
		int lineEnd = -1;
		for (int i = bufferPos; i < bufferEnd; i++) {
			if (buffer[i] == '\n') {
				lineEnd = i;
				break;
			}
		}

		// Line terminator not found from buffer
		if (lineEnd < 0) {
			StringBuilder sb = new StringBuilder(256);

			int c;
			while (((c = read()) != -1) && (c != '\n')) {
				if ((char) c != '\r') {
					sb.append((char) c);
				}
			}
			if ((c == -1) && (sb.length() == 0)) {
				return null;
			}

			return sb.toString();
		}

		if (lineEnd > 0 && buffer[lineEnd - 1] == '\r') {
			str = new String(buffer, bufferPos, lineEnd - bufferPos - 1,
					charset);
		} else {
			str = new String(buffer, bufferPos, lineEnd - bufferPos, charset);
		}

		bufferPos = lineEnd + 1;
		return str;
	}

	/**
	 * <p>
	 * Reads a byte of data from this file.
	 * </p>
	 * <p>
	 * The read is performed on the buffer.
	 * </p>
	 * <p>
	 * The byte is returned as an integer in the range 0 to 255 (
	 * <code>0x00-0x0ff</code>). This method blocks if no input is yet available
	 * </p>
	 * 
	 * @return the next byte of data, or -1 if the end of the file has been
	 *         reached.
	 * @throws IOException
	 */
	@Override
	public final int read() throws IOException {
		if (bufferPos >= bufferEnd) {
			if (fillBuffer() < 0) {
				return -1;
			}
		}

		if (bufferEnd == 0) {
			return -1;
		} else {
			return buffer[bufferPos++];
		}
	}

	/**
	 * <p>
	 * Reads up to <code>len</code> bytes of data from this file into an array
	 * of bytes. This method blocks until at least one byte of input is
	 * available.
	 * </p>
	 * <p>
	 * The read is performed on the buffer.
	 * </p>
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @param off
	 *            the start offset in array b at which the data is written.
	 * @param len
	 *            the maximum number of bytes read.
	 * @throws IOException
	 */
	@Override
	public int read(byte b[], int off, int len) throws IOException {
		int leftover = bufferEnd - bufferPos;
		if (len <= leftover) {
			System.arraycopy(buffer, bufferPos, b, off, len);
			bufferPos += len;
			return len;
		}

		for (int i = 0; i < len; i++) {
			int c = this.read();
			if (c != -1)
				b[off + i] = (byte) c;
			else {
				return i;
			}
		}

		return len;
	}

	/**
	 * <p>
	 * Sets the file-pointer offset, measured from the beginning of this file,
	 * at which the next read or write occurs. The offset may be set beyond the
	 * end of the file. Setting the offset beyond the end of the file does not
	 * change the file length. The file length will change only by writing after
	 * the offset has been set beyond the end of the file.
	 * </p>
	 * <p>
	 * The offset is calculated relative to the buffer.
	 * </p>
	 * 
	 * @param pos
	 *            the offset position, measured in bytes from the beginning of
	 *            the file, at which to set the file pointer.
	 * @throws IOException
	 */
	@Override
	public void seek(long pos) throws IOException {
		int n = (int) (realPos - pos);
		if (n >= 0 && n <= bufferEnd) {
			bufferPos = bufferEnd - n;
		} else {
			super.seek(pos);
			invalidate();
		}
	}

	private int fillBuffer() throws IOException {
		int n = super.read(buffer, 0, bufferSize);
		if (n >= 0) {
			realPos += n;
			bufferEnd = n;
			bufferPos = 0;
		}

		return n;
	}

	private void invalidate() throws IOException {
		bufferEnd = 0;
		bufferPos = 0;
		realPos = super.getFilePointer();
	}
}