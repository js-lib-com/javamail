package js.email.javamail;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import js.converter.Converter;
import js.converter.ConverterException;

/**
 * Email address string converter.
 * 
 * @author Iulian Rotaru
 */
@SuppressWarnings("unchecked")
final class InternetAddressConverter implements Converter {
	/** Package default constructor. */
	InternetAddressConverter() {
	}

	/**
	 * Create email address instance from string representation.
	 * 
	 * @throws ConverterException if string is not a valid email address or personal information cannot be encoded using
	 *             RFC2047.
	 */
	@Override
	public <T> T asObject(String string, Class<T> valueType) throws ConverterException {
		if (string.isEmpty())
			return null;
		assert valueType == InternetAddress.class;

		int index = string.indexOf("<");
		if (index == -1) {
			// we don't have any angular bracket; assume value is a simple email address and parse it strictly
			try {
				return (T) new InternetAddress(string, true);
			} catch (AddressException e) {
				throw new ConverterException("Invalid email address |%s|.", string);
			}
		}

		// we have a leading angular bracket
		// assume value is a full email address string, that is, with both display name and address specification
		String personal = string.substring(0, index).trim();
		String address = string.substring(index + 1, string.length() - 1).trim();
		try {
			return (T) new InternetAddress(address, personal);
		} catch (UnsupportedEncodingException e) {
			// this exception can occur if personal information contains non ASCII characters and RFC2047 encoding fails
			throw new ConverterException("Invalid personal information |%s|. Cannot be encoded using RFC2047.", personal);
		}
	}

	/** Return string representation for email address instance. */
	@Override
	public String asString(Object object) {
		assert object instanceof InternetAddress;
		return object.toString();
	}
}
