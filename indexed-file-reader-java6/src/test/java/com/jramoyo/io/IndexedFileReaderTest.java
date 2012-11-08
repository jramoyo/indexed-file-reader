/*
 * IndexedFileReaderTest.java
 * 8 Nov 2012 
 */
package com.jramoyo.io;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * IndexedFileReaderTest
 * 
 * @author jramoyo
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class IndexedFileReaderTest {

	private IndexedFileReader reader;

	public IndexedFileReaderTest() throws IOException {
		File file = new File("src/test/resources/file.txt");
		reader = new IndexedFileReader(file);
	}

	@Test
	public void testFind() throws Exception {
		SortedMap<Integer, String> lines = reader.find(6, 10, ".*EVEN.*");
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 3, lines.size());
		assertTrue("Incorrect value.", lines.get(6).startsWith("[6]"));
		assertTrue("Incorrect value.", lines.get(8).startsWith("[8]"));
		assertTrue("Incorrect value.", lines.get(10).startsWith("[10]"));
	}

	@Test
	public void testFindExceeded() throws Exception {
		SortedMap<Integer, String> lines = reader.find(46, 55, ".*ODD.*");
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 2, lines.size());
		assertTrue("Incorrect value.", lines.get(47).startsWith("[47]"));
		assertTrue("Incorrect value.", lines.get(49).startsWith("[49]"));
	}

	@Test
	public void testFindIncorrectArgs() throws Exception {
		try {
			reader.find(0, 10, null);
			Assert.fail("Exception not thrown.");
		} catch (NullPointerException ex) {
		}

		try {
			reader.find(0, 10, ".*EVEN.*");
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}

		try {
			reader.find(5, 4, ".*ODD.*");
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}
	}

	@Test
	public void testFindSingle() throws Exception {
		SortedMap<Integer, String> lines = reader.find(5, 5, ".*ODD.*");
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 1, lines.size());
		assertTrue("Incorrect value.", lines.get(5).startsWith("[5]"));
	}

	@Test
	public void testHead() throws Exception {
		SortedMap<Integer, String> lines = reader.head(5);
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 5, lines.size());
		assertTrue("Incorrect value.", lines.get(1).startsWith("[1]"));
		assertTrue("Incorrect value.", lines.get(2).startsWith("[2]"));
		assertTrue("Incorrect value.", lines.get(3).startsWith("[3]"));
		assertTrue("Incorrect value.", lines.get(4).startsWith("[4]"));
		assertTrue("Incorrect value.", lines.get(5).startsWith("[5]"));
	}

	@Test
	public void testHeadIncorrectArgs() throws Exception {
		try {
			reader.head(0);
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}
	}

	@Test
	public void testReadLines() throws Exception {
		SortedMap<Integer, String> lines = reader.readLines(6, 10);
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 5, lines.size());
		assertTrue("Incorrect value.", lines.get(6).startsWith("[6]"));
		assertTrue("Incorrect value.", lines.get(7).startsWith("[7]"));
		assertTrue("Incorrect value.", lines.get(8).startsWith("[8]"));
		assertTrue("Incorrect value.", lines.get(9).startsWith("[9]"));
		assertTrue("Incorrect value.", lines.get(10).startsWith("[10]"));
	}

	@Test
	public void testReadLinesExceeded() throws Exception {
		SortedMap<Integer, String> lines = reader.readLines(46, 55);
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 5, lines.size());
		assertTrue("Incorrect value.", lines.get(46).startsWith("[46]"));
		assertTrue("Incorrect value.", lines.get(47).startsWith("[47]"));
		assertTrue("Incorrect value.", lines.get(48).startsWith("[48]"));
		assertTrue("Incorrect value.", lines.get(49).startsWith("[49]"));
		assertTrue("Incorrect value.", lines.get(50).startsWith("[50]"));
	}

	@Test
	public void testReadLinesIncorrectArgs() throws Exception {
		try {
			reader.readLines(100, 110);
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}

		try {
			reader.readLines(0, 10);
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}

		try {
			reader.readLines(5, 4);
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}
	}

	@Test
	public void testReadLinesSingle() throws Exception {
		SortedMap<Integer, String> lines = reader.readLines(5, 5);
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 1, lines.size());
		assertTrue("Incorrect value.", lines.get(5).startsWith("[5]"));
	}

	@Test
	public void testTail() throws Exception {
		SortedMap<Integer, String> lines = reader.tail(5);
		assertNotNull("Null result.", lines);
		assertEquals("Incorrect length.", 5, lines.size());
		assertTrue("Incorrect value.", lines.get(46).startsWith("[46]"));
		assertTrue("Incorrect value.", lines.get(47).startsWith("[47]"));
		assertTrue("Incorrect value.", lines.get(48).startsWith("[48]"));
		assertTrue("Incorrect value.", lines.get(49).startsWith("[49]"));
		assertTrue("Incorrect value.", lines.get(50).startsWith("[50]"));
	}

	@Test
	public void testTailIncorrectArgs() throws Exception {
		try {
			reader.tail(0);
			Assert.fail("Exception not thrown.");
		} catch (IllegalArgumentException ex) {
		}
	}
}
