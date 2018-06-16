package js.email.javamail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.junit.Ignore;

import js.util.Classes;
import js.util.Files;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

@Ignore
public class TestCaseEx extends TestCase {
	protected static File projectDir;

	protected void start(String repositoryRelativePath, String configXML) throws IOException {
		String projectPath = System.getProperty("project.path");
		projectDir = new File(projectPath == null ? "." : projectPath);
		assertTrue("Invalid project directory. Please see ant junit <sysproperty key='project.path' value...", projectDir.isDirectory());

		File repositoryDir = new File(projectDir, repositoryRelativePath);
		// inject repository directory into config XML
		configXML = String.format(configXML, repositoryDir.getAbsolutePath());
		// Debug.start(Builder.parseXML(configXML));
	}

	public static void assertEquals(String expectedPath, File concreteFile) {
		String expected = expectedPath.replaceAll("\\\\", "").replaceAll("/", "");
		String concrete = concreteFile.toString().replaceAll("\\\\", "").replaceAll("/", "");
		assertEquals(expected, concrete);
	}

	public static void assertDatesEquals(Object object, String fieldName, Object expected) {
		Date d = Classes.getFieldValue(object, fieldName);
		String concrete = new java.sql.Date(d.getTime()).toString();
		if (expected instanceof Date) {
			d = (Date) expected;
			expected = new java.sql.Date(d.getTime()).toString();
		}
		assertEquals(expected, concrete);
	}

	/**
	 * Compare dates by content. Two dates are the same if have the same time value. It ignores the actual implementation class
	 * and milliseconds.
	 * 
	 * @param expected
	 * @param concrete
	 */
	public static void assertTheSame(Date expected, Date concrete) {
		Calendar expectedCalendar = Calendar.getInstance();
		expectedCalendar.setTime(expected);
		expectedCalendar.set(Calendar.MILLISECOND, 0);
		Calendar concreteCalendar = Calendar.getInstance();
		concreteCalendar.setTime(expected);
		concreteCalendar.set(Calendar.MILLISECOND, 0);
		assertEquals(expectedCalendar.getTime().getTime(), concreteCalendar.getTime().getTime());
	}

	/**
	 * Compare files by content. Two files are considered the same if have the same content. They are equals if wraps the same
	 * file system path.
	 * 
	 * @param expectedFile
	 * @param concreteFile
	 */
	public static void assertTheSame(File expectedFile, File concreteFile) {
		assertEquals(expectedFile.length(), concreteFile.length());
		InputStream expectedStream = null;
		InputStream concreteStream = null;
		try {
			expectedStream = new FileInputStream(expectedFile);
			concreteStream = new FileInputStream(concreteFile);
			byte[] expectedBuffer = new byte[1000];
			byte[] concreteBuffer = new byte[1000];
			for (;;) {
				if (expectedStream.read(expectedBuffer) == -1)
					break;
				concreteStream.read(concreteBuffer);
				assertTrue(Arrays.equals(expectedBuffer, concreteBuffer));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Files.close(expectedStream);
			Files.close(concreteStream);
		}
	}

	protected static void assertEmpty(Collection<?> collection) {
		assertTrue(collection.isEmpty());
	}

	protected static void assertEmpty(Map<?, ?> map) {
		assertTrue(map.isEmpty());
	}

	protected static void assertNotEmpty(Collection<?> collection) {
		assertFalse(collection.isEmpty());
	}

	protected static void assertNotEmpty(Map<?, ?> map) {
		assertFalse(map.isEmpty());
	}

	protected static void assertLength(int length, Collection<?> collection) {
		assertEquals(length, collection.size());
	}

	protected static void assertLength(int length, Map<?, ?> map) {
		assertEquals(length, map.size());
	}

	protected static void assertInvokeTrue(Object object, String methodName, Object... arguments) {
		try {
			assertTrue((Boolean) Classes.invoke(object, methodName, arguments));
		} catch (AssertionFailedError e) {
			throw e;
		} catch (Throwable t) {
			fail(String.format("Invocation of %s on %s throws %s", methodName, object.getClass(), t.getClass()));
		}
	}

	protected static void assertInvokeFalse(Object object, String methodName, Object... arguments) {
		try {
			assertFalse((Boolean) Classes.invoke(object, methodName, arguments));
		} catch (AssertionFailedError e) {
			throw e;
		} catch (Throwable t) {
			fail(String.format("Invocation of %s on %s throws %s", methodName, object.getClass(), t.getClass()));
		}
	}

	protected static void assertInvokeNull(Object object, String methodName, Object... arguments) {
		try {
			assertNull(Classes.invoke(object, methodName, arguments));
		} catch (AssertionFailedError e) {
			throw e;
		} catch (Throwable t) {
			fail(String.format("Invocation of %s on %s throws %s", methodName, object.getClass(), t.getClass()));
		}
	}

	protected static void assertInvokeEquals(Object expected, Object object, String methodName, Object... arguments) {
		try {
			assertEquals(expected, Classes.invoke(object, methodName, arguments));
		} catch (AssertionFailedError e) {
			throw e;
		} catch (Throwable t) {
			fail(String.format("Invocation of %s on %s throws %s", methodName, object.getClass(), t.getClass()));
		}
	}

	protected static void assertInvokeThrows(Class<? extends Throwable> expected, Object object, String methodName, Object... arguments) {
		String s = String.format("Invocation of %s on %s should throw %s", methodName, object.getClass(), expected);
		try {
			Classes.invoke(object, methodName, arguments);
			fail(s);
		} catch (Throwable t) {
			if (!t.getClass().equals(expected))
				fail(s);
		}
	}

	protected static void assertInvokeThrows(String expectedMessage, Object object, String methodName, Object... arguments) {
		String s = String.format("Invocation of %s on %s should throw \"%s\"", methodName, object.getClass(), expectedMessage);
		try {
			Classes.invoke(object, methodName, arguments);
			fail(s);
		} catch (Throwable t) {
			if (t.getMessage() == null || !t.getMessage().equals(expectedMessage))
				fail(s);
		}
	}
}
