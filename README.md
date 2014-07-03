# Indexed File Reader
[![Build Status](http://img.shields.io/travis/jramoyo/indexed-file-reader.svg?style=flat)](https://travis-ci.org/jramoyo/indexed-file-reader)
[![Issues](http://img.shields.io/github/issues/jramoyo/indexed-file-reader.svg?style=flat)](https://github.com/jramoyo/indexed-file-reader/issues?state=open)
[![Gittip](http://img.shields.io/gittip/jramoyo.svg?style=flat)](https://www.gittip.com/jramoyo/)

## Abstract
There are cases when an application needs to read specific lines from a text file ([1](http://stackoverflow.com/questions/2312756/in-java-how-to-read-from-a-file-a-specific-line-given-the-line-number)).

Unless each line has a fixed length, the popular method to achieve this in Java is to read the file line-by-line, from the beginning until the desired line is reached. While this can be achieved with reasonable performance using existing JDK classes, repeated reads on the same file, particularly when reading non-adjacent lines, may yield unnecessary overhead.

A better solution is to index the positions of each line marker. This index can then be used to retrieve the position of a specific line number and use a seeking reader (i.e `RandomAccessFile` or `BufferedReader`) to start reading from that position.

The purpose of this library is to encapsulate the second solution into a re-usable API. 

In order to perform indexing with good performance, `RandomAccessFile` is extended to support buffered reads ([2](http://www.javaworld.com/javaworld/javatips/jw-javatip26.html)). The API also supports concurrent indexing to take advantage of multiple processors when reading larger files.

Refer to the [Javadoc](http://indexed-file-reader.googlecode.com/svn/javadoc/index.html) for more details.

_This library uses [Fork/Join](http://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html) and hence requires Java 7. For a limited time, a Java 6 version which uses a Fork/Join backport (`jsr166y`) is also available._

### Maven
```xml
<dependency>
  <groupId>com.jramoyo</groupId>
  <artifactId>indexed-file-reader</artifactId>
  <version>1.0</version>
</dependency>
```
Or
```xml
<dependency>
  <groupId>com.jramoyo</groupId>
  <artifactId>indexed-file-reader-java6</artifactId>
  <version>1.0</version>
</dependency>
```

### Example
```java
File file = new File("src/test/resources/file.txt");
reader = new IndexedFileReader(file);

SortedMap<Integer, String> lines = reader.head(5);
assertNotNull("Null result.", lines);
assertEquals("Incorrect length.", 5, lines.size());
assertTrue("Incorrect value.", lines.get(1).startsWith("[1]"));
assertTrue("Incorrect value.", lines.get(2).startsWith("[2]"));
assertTrue("Incorrect value.", lines.get(3).startsWith("[3]"));
assertTrue("Incorrect value.", lines.get(4).startsWith("[4]"));
assertTrue("Incorrect value.", lines.get(5).startsWith("[5]"));

lines = reader.readLines(6, 10);
assertNotNull("Null result.", lines);
assertEquals("Incorrect length.", 5, lines.size());
assertTrue("Incorrect value.", lines.get(6).startsWith("[6]"));
assertTrue("Incorrect value.", lines.get(7).startsWith("[7]"));
assertTrue("Incorrect value.", lines.get(8).startsWith("[8]"));
assertTrue("Incorrect value.", lines.get(9).startsWith("[9]"));
assertTrue("Incorrect value.", lines.get(10).startsWith("[10]"));

lines = reader.tail(5);
assertNotNull("Null result.", lines);
assertEquals("Incorrect length.", 5, lines.size());
assertTrue("Incorrect value.", lines.get(46).startsWith("[46]"));
assertTrue("Incorrect value.", lines.get(47).startsWith("[47]"));
assertTrue("Incorrect value.", lines.get(48).startsWith("[48]"));
assertTrue("Incorrect value.", lines.get(49).startsWith("[49]"));
assertTrue("Incorrect value.", lines.get(50).startsWith("[50]"));

lines = reader.find(6, 10, ".*EVEN.*");
assertNotNull("Null result.", lines);
assertEquals("Incorrect length.", 3, lines.size());
assertTrue("Incorrect value.", lines.get(6).startsWith("[6]"));
assertTrue("Incorrect value.", lines.get(8).startsWith("[8]"));
assertTrue("Incorrect value.", lines.get(10).startsWith("[10]"));
```

The above example reads a text file composed of 50 lines in the following format:
```
[1] The quick brown fox jumped over the lazy dog ODD
[2] The quick brown fox jumped over the lazy dog EVEN
...
```
