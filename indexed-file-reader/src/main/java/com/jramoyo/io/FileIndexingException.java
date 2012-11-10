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
 * FileIndexingException.java
 * 10 Nov 2012 
 */
package com.jramoyo.io;

import java.io.File;

/**
 * FileIndexingException
 * <p>
 * Signals that an exception occurred while indexing a file.
 * </p>
 * 
 * @author jramoyo
 */
public class FileIndexingException extends RuntimeException {

	private static final long serialVersionUID = -6457380789213673476L;

	/**
	 * Creates a FileIndexingException
	 * 
	 * @param file
	 *            the file object being indexed
	 */
	public FileIndexingException(File file) {
		super("An exception occurred while indexing " + file.getName());
	}

	/**
	 * Creates a FileIndexingException
	 * 
	 * @param file
	 *            the file object being indexed
	 * @param cause
	 *            the cause of the exception
	 */
	public FileIndexingException(File file, Throwable cause) {
		super("An exception occurred while indexing " + file.getName(), cause);
	}
}