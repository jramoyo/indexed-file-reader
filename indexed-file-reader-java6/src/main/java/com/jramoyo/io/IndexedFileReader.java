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
 * IndexedFileReader.java
 * 8 Nov 2012 
 */
package com.jramoyo.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jsr166y.ForkJoinPool;
import jsr166y.RecursiveTask;

/**
 * IndexedFileReader
 * <p>
 * Convenience class for reading character files by line number.
 * </p>
 * <p>
 * The file will be indexed only once during object creation, any updates to the
 * underlying file will not be covered by the index.
 * </p>
 * <p>
 * Depending on the configuration, files greater than 1 MB can be split and
 * indexed concurrently.
 * </p>
 * <p>
 * This class is thread-safe. Because reading from the file updates the position
 * of the offset, a lock is in place to prevent concurrent reads.
 * </p>
 * 
 * @author jramoyo
 */
public final class IndexedFileReader {
	// Shared across all instances
	private static final ForkJoinPool DEFAULT_POOL = new ForkJoinPool();

	private static final long MIN_FORK_THRESHOLD = 1000000L;
	private static final String READ_MODE = "r";

	private final BufferedRandomAccessFile raf;
	private final Charset charset;
	private final SortedSet<Long> index;

	private final Lock lock;

	/**
	 * Creates a IndexedTextFileReader, given the <code>File</code> to read
	 * from.
	 * <p>
	 * The default character set will be used and the file will not be
	 * concurrently indexed.
	 * </p>
	 * 
	 * @param file
	 *            the <code>File</code> to read from
	 * @throws IOException
	 */
	public IndexedFileReader(File file) throws IOException {
		this(file, Charset.defaultCharset(), 1, DEFAULT_POOL);
	}

	/**
	 * Creates a IndexedFileReader
	 * <p>
	 * The specified character set will be used and the file will not be
	 * concurrently indexed.
	 * </p>
	 * 
	 * @param file
	 *            the <code>File</code> to read from
	 * @param charset
	 *            the character set to use
	 * @throws IOException
	 */
	public IndexedFileReader(File file, Charset charset) throws IOException {
		this(file, charset, 1, DEFAULT_POOL);
	}

	/**
	 * Creates a IndexedFileReader
	 * <p>
	 * The specified character set will be used and the file will be
	 * concurrently split according to the specified <code>splitCount</code>.
	 * The default pool will be used to concurrently index the file.
	 * </p>
	 * 
	 * @param file
	 *            the <code>File</code> to read from
	 * @param charset
	 *            the character set to use
	 * @param splitCount
	 *            the number of times the file will be divided during concurrent
	 *            indexing
	 * @throws IOException
	 */
	public IndexedFileReader(File file, Charset charset, int splitCount)
			throws IOException {
		this(file, charset, splitCount, DEFAULT_POOL);
	}

	/**
	 * Creates a IndexedFileReader
	 * <p>
	 * The specified character set will be used and the file will be
	 * concurrently split according to the specified <code>splitCount</code>.
	 * The specified pool will be used to concurrently index the file.
	 * </p>
	 * 
	 * @param file
	 *            the <code>File</code> to read from
	 * @param charset
	 *            the character set to use
	 * @param splitCount
	 *            the number of times the file will be divided during concurrent
	 *            indexing
	 * @param pool
	 *            the pool to use when concurrently indexing the file
	 * @throws IOException
	 */
	public IndexedFileReader(File file, Charset charset, int splitCount,
			ForkJoinPool pool) throws IOException {
		this.raf = new BufferedRandomAccessFile(file, READ_MODE);
		this.charset = charset;

		long threshold = Math.max(MIN_FORK_THRESHOLD, file.length()
				/ splitCount);
		this.index = Collections.unmodifiableSortedSet(pool
				.invoke(new IndexingTask(file, 0, file.length(), threshold)));

		this.lock = new ReentrantLock();
	}

	/**
	 * Creates a IndexedTextFileReader, given the <code>File</code> to read
	 * from.
	 * <p>
	 * The default character set will be used and the file will be concurrently
	 * split according to the specified <code>splitCount</code>. The default
	 * pool will be used to concurrently index the file.
	 * </p>
	 * 
	 * @param file
	 *            the <code>File</code> to read from
	 * @param splitCount
	 *            the number of times the file will be divided during concurrent
	 *            indexing
	 * @throws IOException
	 */
	public IndexedFileReader(File file, int splitCount) throws IOException {
		this(file, Charset.defaultCharset(), splitCount, DEFAULT_POOL);
	}

	/**
	 * Finds lines matching a given regular expression from a range of line
	 * numbers
	 * 
	 * @param from
	 *            the starting line
	 * @param to
	 *            the end line
	 * @param regex
	 *            the regular expression to match
	 * @return a sorted map of lines matching the regular expression, having the
	 *         line number as key and the text as value.
	 * @throws IOException
	 */
	public SortedMap<Integer, String> find(int from, int to, String regex)
			throws IOException {
		if (regex == null) {
			throw new NullPointerException("Regex cannot be null!");
		}
		if (from < 1) {
			throw new IllegalArgumentException("Argument 'from' must"
					+ " be greater than or equal to 1!");
		}
		if (to < from) {
			throw new IllegalArgumentException("Argument 'to' must"
					+ " be greater than or equal to 'from'!");
		}

		SortedMap<Integer, String> lines = new TreeMap<Integer, String>();
		List<Long> positions = new ArrayList<Long>(index);

		try {
			lock.lock();

			raf.seek(positions.get(from - 1));
			for (int i = from; i <= to; i++) {
				String line = raf.getNextLine(charset);
				if (line != null) {
					if (line.matches(regex)) {
						lines.put(i, line);
					}
				} else {
					break;
				}
			}

			return lines;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Reads the first n number of lines
	 * 
	 * @param n
	 *            the number of lines
	 * @return a map of the first n number of lines, having the line number as
	 *         key and the text as value.
	 * @throws IOException
	 */
	public SortedMap<Integer, String> head(int n) throws IOException {
		if (n < 1) {
			throw new IllegalArgumentException("Argument 'n' must"
					+ " be greater than or equal to 1!");
		}

		return readLines(1, n);
	}

	/**
	 * Reads lines given a range of line numbers
	 * 
	 * @param file
	 *            the file to read
	 * @param from
	 *            the starting line
	 * @param to
	 *            the end line
	 * @return a sorted map of lines read, having the line number as key and the
	 *         text as value.
	 * @throws IOException
	 */
	public SortedMap<Integer, String> readLines(int from, int to)
			throws IOException {
		if (from < 1) {
			throw new IllegalArgumentException("Argument 'from' must"
					+ " be greater than or equal to 1!");
		}
		if (to < from) {
			throw new IllegalArgumentException("Argument 'to' must"
					+ " be greater than or equal to 'from'!");
		}
		if (from > index.size()) {
			throw new IllegalArgumentException("Argument 'from' must"
					+ " be less than the file's number of lines!");
		}

		SortedMap<Integer, String> lines = new TreeMap<Integer, String>();
		List<Long> positions = new ArrayList<Long>(index);

		try {
			lock.lock();

			raf.seek(positions.get(from - 1));
			for (int i = from; i <= to; i++) {
				String line = raf.getNextLine(charset);
				if (line != null) {
					lines.put(i, line);
				} else {
					break;
				}
			}

			return lines;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the last n number of lines
	 * 
	 * @param file
	 *            the file to read
	 * @param n
	 *            the number of lines
	 * @return a map of the last n number of lines, having the line number as
	 *         key and the text as value.
	 * @throws IOException
	 */
	public SortedMap<Integer, String> tail(int n) throws IOException {
		if (n < 1) {
			throw new IllegalArgumentException("Argument 'n' must"
					+ " be greater than or equal to 1!");
		}

		int from = index.size() - n;
		int to = from + n;
		return readLines(from, to);
	}

	/**
	 * IndexingTask
	 * <p>
	 * Forked task for indexing text files.
	 * </p>
	 * 
	 * @author jramoyo
	 */
	private static final class IndexingTask extends
			RecursiveTask<SortedSet<Long>> {
		private static final long serialVersionUID = 3509549890190032574L;

		private final File file;
		private final long start;
		private final long end;
		private final long length;
		private final long threshold;

		/**
		 * Creates a IndexingTask
		 * 
		 * @param file
		 *            the file to index
		 * @param start
		 *            the starting offset
		 * @param end
		 *            the end offset
		 * @param threshold
		 *            the threshold used to decide whether to compute directly
		 *            or fork to another task
		 */
		public IndexingTask(File file, long start, long end, long threshold) {
			this.file = file;
			this.start = start;
			this.end = end;
			this.length = end - start;
			this.threshold = threshold;
		}

		/**
		 * The forked computation.
		 * <p>
		 * The resulting index always includes the position to the end-of-file
		 * (EOF).
		 * </p>
		 * 
		 * @return a Sorted set of positions representing a start of line.
		 */
		@Override
		protected SortedSet<Long> compute() {
			SortedSet<Long> index = new TreeSet<Long>();
			try {
				if (length < threshold) {
					BufferedRandomAccessFile raf = null;
					try {
						raf = new BufferedRandomAccessFile(file, "r");
						raf.seek(start);

						// Add the position for 1st line
						if (raf.getFilePointer() == 0L) {
							index.add(Long.valueOf(raf.getFilePointer()));
						}
						while (raf.getFilePointer() < end) {
							raf.getNextLine();
							index.add(Long.valueOf(raf.getFilePointer()));
						}
					} finally {
						if (raf != null) {
							raf.close();
						}
					}
				}

				else {
					long start1 = start;
					long end1 = start + (length / 2);

					long start2 = end1;
					long end2 = end;

					IndexingTask task1 = new IndexingTask(file, start1, end1,
							threshold);
					task1.fork();
					IndexingTask task2 = new IndexingTask(file, start2, end2,
							threshold);

					index.addAll(task2.compute());
					index.addAll(task1.join());
				}
			} catch (IOException ex) {
			}

			return index;
		}
	}
}