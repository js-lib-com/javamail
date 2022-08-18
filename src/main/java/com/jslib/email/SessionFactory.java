package com.jslib.email;

import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.jslib.api.email.EmailException;
import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.lang.Config;

/**
 * JavaMail session factory creates and configures session instance. Factory provides an instance getter, see
 * {@link #getSession()} that just return JavaMail session configured by {@link #config(Config)}. If configuration
 * object has user name, and password for that mater, created session is authenticated.
 * <p>
 * JavaMail session mandates <code>mail.smtp.host</code> property. If it is missing session factory uses
 * <code>localhost</code>.
 * <p>
 * This factory allows for session warm reconfiguration; current implementation just recreate JavaMail session with
 * newly provided configuration object. In order to support warm reconfiguration all session factory methods are
 * synchronized.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
final class SessionFactory
{
  /** Class logger. */
  private static final Log log = LogFactory.getLog(SessionFactory.class);

  /** Container root context. */
  private static final String ROOT_CONTEXT = "java:comp/env";

  /** Property key for JNDI resource reference to Java Mail Session. */
  private static final String PROP_RESOURCE_REFERENCE = "js.resource.reference";
  /** Property key for transport protocol. */
  private static final String PROP_TRASNPORT_PROTOCOL = "mail.transport.protocol";
  /** Property key for SMTP host name. */
  private static final String PROP_SMTP_HOST = "mail.smtp.host";
  /**
   * Property key for container from address. This property is optional and its value is stored into
   * {@link #fromAddress}.
   */
  private static final String PROP_SMTP_FROM = "mail.smtp.from";
  /** Property key for optional user name. If user name is configured session is authenticated. */
  private static final String PROP_EMAIL_USER = "js.email.user";
  /** Property key for user password, mandatory only if user name is present. */
  private static final String PROP_EMAIL_PASSWORD = "js.email.password";
  /** Property key for Java Mail API debug. */
  private static final String PROP_MAIL_DEBUG = "mail.debug";

  /** JavaMail session instance created and configured by {@link #config(Config)}. */
  private Session session;

  /** Default <code>from</code> address configured by container, used when email instance has none specified. */
  private InternetAddress fromAddress;

  /**
   * Create JavaMail session and configure from given configuration object. Configuration object should contain only
   * properties related to JavaMail service plus optional user name and password; if user name is present password is
   * mandatory.
   * 
   * @param config configuration object.
   */
  public synchronized void config(final Config config)
  {
    if(!config.hasProperty(PROP_TRASNPORT_PROTOCOL)) {
      log.debug("Email property |{email_property}| is missing. Force to |smtp|.", PROP_TRASNPORT_PROTOCOL);
      config.setProperty(PROP_TRASNPORT_PROTOCOL, "smtp");
    }

    if(!config.hasProperty(PROP_SMTP_HOST)) {
      log.debug("Email property |{email_property}| is missing. Force to |localhost|.", PROP_SMTP_HOST);
      config.setProperty(PROP_SMTP_HOST, "localhost");
    }

    if(!config.hasProperty(PROP_MAIL_DEBUG)) {
      log.debug("Email property |{email_property}| is missing. Force to |false|.", PROP_MAIL_DEBUG);
      config.setProperty(PROP_MAIL_DEBUG, false);
    }

    String resourceReference = config.getProperty(PROP_RESOURCE_REFERENCE);
    if(resourceReference != null) {
      log.debug("Lookup container JavaMail session.");

      // if resource reference is defined there should be a container that provides JNDI context
      try {
        Context initialContext = new InitialContext();
        Context environmentContext = (Context)initialContext.lookup(ROOT_CONTEXT);
        session = (Session)environmentContext.lookup(resourceReference);
      }
      catch(NamingException e) {
        throw new EmailException("Missing container or bad mail resource configuration |%s|. Unable to create Java Mail Session.", resourceReference);
      }

      String fromAddress = session.getProperties().getProperty(PROP_SMTP_FROM);
      if(fromAddress != null) {
        try {
          this.fromAddress = InternetAddress.parse(fromAddress)[0];
          log.debug("Initialize <from> address to |{email_from}| from |{email_property}| property.", fromAddress, PROP_SMTP_FROM);
        }
        catch(AddressException e) {
          throw new EmailException(e);
        }
      }
      else {
        this.fromAddress = null;
      }
      return;
    }

    log.debug("Create JavaMail session:{dump}", dump(config.getProperties()));

    final String user = config.getProperty(PROP_EMAIL_USER);
    if(user == null) {
      // not authenticated session
      session = Session.getInstance(config.getProperties());
      return;
    }

    // here user is defined so password is mandatory
    final String password = config.getProperty(PROP_EMAIL_PASSWORD);
    if(password == null) {
      throw new EmailException("Missing user |%s| password for authenticated session.", user);
    }

    // authenticated session
    session = Session.getInstance(config.getProperties(), new Authenticator()
    {
      protected PasswordAuthentication getPasswordAuthentication()
      {
        return new PasswordAuthentication(user, password);
      }
    });
  }

  /**
   * Dump configuration properties to a string and return it, for debugging purposes.
   * 
   * @param properties configuration properties.
   * @return serialized configuration properties.
   */
  private static String dump(Properties properties)
  {
    StringBuilder builder = new StringBuilder();
    Enumeration<Object> enumeration = properties.keys();
    while(enumeration.hasMoreElements()) {
      Object key = enumeration.nextElement();
      builder.append("\r\n\t- ");
      builder.append(key);
      builder.append(": ");
      builder.append(properties.getProperty(key.toString()));
    }
    return builder.toString();
  }

  /**
   * Get currently configured JavaMail session instance. This getter should be called after {@link #config(Config)},
   * otherwise always returns null.
   * 
   * @return JavaMail session instance.
   */
  public synchronized Session getSession()
  {
    return session;
  }

  /**
   * Get container from address or null if {@link #PROP_SMTP_FROM} property is not configured. This value is optional
   * and is used if email instance has no <code>from</code> address configured.
   * 
   * @return container from address, possible null.
   */
  public synchronized InternetAddress getFromAddress()
  {
    return fromAddress;
  }
}
