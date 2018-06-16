package js.email.javamail;

import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import js.email.EmailException;
import js.lang.Config;
import js.log.Log;
import js.log.LogFactory;

/**
 * JavaMail session factory creates and configures session instance. Factory provides an instance getter, see
 * {@link #getSession()} that just return JavaMail session configured by {@link #config(Config)}. If configuration object has
 * user name, and password for that mater, created session is authenticated.
 * <p>
 * JavaMail session mandates <code>mail.smtp.host</code> property. If it is missing session factory uses <code>localhost</code>.
 * <p>
 * This factory allows for session warm reconfiguration; current implementation just recreate JavaMail session with newly
 * provided configuration object. In order to support warm reconfiguration all session factory methods are synchronized.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class SessionFactory {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(SessionFactory.class);

	/** Property key for SMTP host name. */
	private static final String PROP_HOST = "mail.smtp.host";
	/** Property key for optional user name. If user name is configured session is authenticated. */
	private static final String PROP_USER = "js.email.user";
	/** Property key for user password, mandatory only if user name is present. */
	private static final String PROP_PASSWORD = "js.email.password";

	/** JavaMail session instance created and configured by {@link #config(Config)}. */
	private Session session;

	/**
	 * Create JavaMail session and configure from given configuration object. Configuration object should contain only
	 * properties related to JavaMail service plus optional user name and password; if user name is present password is
	 * mandatory.
	 * 
	 * @param config configuration object.
	 */
	public synchronized void config(final Config config) {
		if (!config.hasProperty(PROP_HOST)) {
			log.debug("Email property <%s> is missing. Force localhost.", PROP_HOST);
			config.setProperty(PROP_HOST, "localhost");
		}

		// at this point configuration object contains only properties related to mail service plus optional user name and
		// password; if user name is present password is mandatory

		log.debug("Configure JavaMail session:\r\n%s", dump(config.getProperties()));
		if (!config.hasProperty(PROP_USER)) {
			// not authenticated session
			session = Session.getInstance(config.getProperties());
			return;
		}

		final String user = config.getProperty(PROP_USER);
		if (!config.hasProperty(PROP_PASSWORD)) {
			throw new EmailException("Missing user |%s| password for authenticated session.", user);
		}

		// authenticated session
		session = Session.getInstance(config.getProperties(), new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, config.getProperty(PROP_PASSWORD));
			}
		});
	}

	private static String dump(Properties properties) {
		StringBuilder builder = new StringBuilder();
		Enumeration<Object> enumeration = properties.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			builder.append("\t- ");
			builder.append(key);
			builder.append(": ");
			builder.append(properties.getProperty(key.toString()));
			builder.append("\r\n");
		}
		return builder.toString();
	}

	/**
	 * Get currently configured JavaMail session instance. This getter should be called after {@link #config(Config)}, otherwise
	 * always returns null.
	 * 
	 * @return JavaMail session instance.
	 */
	public synchronized Session getSession() {
		return session;
	}
}
