package js.email.javamail;

import js.converter.Converter;
import js.email.EmailException;

/**
 * RFC2822 message identifier. This is a unique message identifier that refers to a particular version of a particular message.
 * It is structured as bellow:
 * 
 * <pre>
 * msg-id = [CFWS] "&lt;" id-left "@" id-right "&gt;" [CFWS]
 * </pre>
 * 
 * where optional <code>CFWS</code> means comment, folding or white-space whereas, in our case, <code>id-left</code> is a random
 * UUID and <code>id-right</code> is a mailer exchange unique identifier. Note that the same <code>id-left</code> is used to set
 * envelope sender, as described by VERP algorithm.
 */
public final class MessageIDConverter implements Converter {
	/**
	 * Create an email message ID from given string. Return newly create message ID instance. If given <code>string</code> value
	 * is empty returns null.
	 * 
	 * @param string RFC2822 email message ID string representation.
	 * @param valueType unused value type, always MessageID.class.
	 * @return message ID instance or null.
	 * @throws EmailException if string argument is not a well formed email message ID.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T asObject(String string, Class<T> valueType) throws EmailException {
		if (string.isEmpty()) {
			return null;
		}
		return (T) new MessageID(string);
	}

	/**
	 * Return string representation of an email message ID instance.
	 * 
	 * @param object email message ID instance.
	 * @return given message ID string representation.
	 */
	@Override
	public String asString(Object object) {
		return ((MessageID) object).getValue();
	}
}
