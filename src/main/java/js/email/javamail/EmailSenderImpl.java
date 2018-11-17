package js.email.javamail;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;

import js.email.Email;
import js.email.EmailException;
import js.email.EmailSender;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.template.Template;
import js.template.TemplateEngine;
import js.util.Classes;
import js.util.Files;
import js.util.I18nFile;
import js.util.I18nPool;
import js.util.I18nRepository;
import js.util.Params;

/**
 * Email sender implementation based on Java Mail API.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class EmailSenderImpl implements EmailSender
{
  /** Class logger. */
  private static final Log log = LogFactory.getLog(EmailSenderImpl.class);

  /** Mail session factory. */
  private SessionFactory sessionFactory;

  /**
   * In development mode email sender does not actually send email messages to email server but just dump email message
   * content to standard out. Development mode is activated by <code>js.dev.mode</code> property.
   */
  private boolean developmentMode;

  /** Mail transporter used to send emails. */
  private Transport transport;

  /** X(HT)ML template builder. */
  private TemplateEngine templateEngine;

  /** Multi-language template documents pool. Null if this email provider is configured without templates. */
  private I18nPool<File> templatesPool;

  /** Default content type used when email instance has none specified. */
  private String contentType;

  /** Default <code>email subject</code> used when email instance has none specified. */
  private String emailSubject;

  /** Default <code>from</code> address used when email instance has none specified. */
  private InternetAddress fromAddress;

  /**
   * Default <code>envelope from</code> address used when email instance has none specified. This value is optional; if
   * is not configured this sender uses <code>from</code> address.
   */
  private String envelopeFrom;

  /**
   * Default <code>reply to</code> addresses used when email instance has none configured. This value is optional; if is
   * not configured this sender uses <code>from</code> address.
   */
  private InternetAddress[] replyToAddresses;

  public EmailSenderImpl()
  {
    log.trace("EmailProviderImpl()");
  }

  /**
   * Configure email processor instance. Scan email templates repository and initialize documents pool then create email
   * session instance. Emails configuration section looks like:
   * 
   * <pre>
   *    &lt;emails repository="path/to/email/templates/repository" files-pattern="*.html"&gt;
   *        &lt;property name="JavaMail.property" value="JavaMail.value" /&gt;
   *    &lt;/emails&gt;
   * </pre>
   * 
   * Templates repository is optional if this processor is intended to send plain, i.e. not templates based emails - see
   * {@link #send(String, String, String, String)}. Anyway, if present, repository path should be valid on underlying
   * file system. Files pattern is optional, default to <code>*.htm</code>; it uses the characters '?' and '*' to
   * represent a single or multiple wild card characters.
   * 
   * @param config configuration object.
   * @throws ConfigException if templates repository is defined but not a directory or no templates found.
   */
  @Override
  public void config(Config config) throws Exception
  {
    log.trace("config(Config)");

    // if session factory is already create this configuration method runs as warm reconfiguration
    // warm configuration deals only with mail session parameters but does not affect template engine and repository
    if(sessionFactory != null) {
      sessionFactory.config(config);
      return;
    }

    developmentMode = config.getProperty(PROP_DEV_MODE, Boolean.class, false);
    contentType = config.getProperty(PROP_CONTENT_TYPE, DEF_CONTENT_TYPE);
    if(config.hasProperty(PROP_FROM_ADDRESS)) {
      this.fromAddress = InternetAddress.parse(config.getProperty(PROP_FROM_ADDRESS))[0];
    }

    // instantiate template engine
    String templateEngineProvider = config.getProperty(PROP_TEMPLATE_ENGINE);
    if(templateEngineProvider != null) {
      // if template engine provider is configured uses its class to create new instance
      templateEngine = Classes.newInstance(templateEngineProvider);
    }
    else {
      // if template engine is not configured, loads it via Java service loader
      templateEngine = Classes.loadService(TemplateEngine.class);
    }

    // scan and initialize templates repository
    String repositoryPath = config.getProperty(PROP_REPOSITORY_PATH);
    if(repositoryPath != null) {
      ConfigBuilder builder = new I18nRepository.ConfigBuilder(repositoryPath, config.getProperty(PROP_FILE_PATTERN, DEF_FILE_PATTERN));
      I18nRepository repository = new I18nRepository(builder.build());
      templatesPool = repository.getPoolInstance();

      for(I18nFile i18nFile : repository) {
        final File file = i18nFile.getFile();
        String templateName = Files.basename(file);
        if(templatesPool.put(templateName, file, i18nFile.getLocale())) {
          log.warn("Override email template |%s:%s|", templateName, file);
        }
        else {
          log.debug("Register email template |%s:%s|.", templateName, file);
        }
      }
    }

    sessionFactory = new SessionFactory();
    sessionFactory.config(config);
  }

  @Override
  public Email getEmail(String templateName)
  {
    Params.notNullOrEmpty(templateName, "Template name");
    return createEmail(null, templateName);
  }

  @Override
  public Email getEmail(Locale locale, String templateName)
  {
    Params.notNull(locale, "Locale");
    Params.notNullOrEmpty(templateName, "Template name");
    return createEmail(locale, templateName);
  }

  /**
   * Create localized email instance for requested template file.
   * 
   * @param locale locale settings or null for default,
   * @param templateName template name.
   * @return email instance.
   * @throws EmailException if templates repository is not configured or template file is missing.
   */
  private Email createEmail(Locale locale, String templateName)
  {
    if(templatesPool == null) {
      throw new EmailException(
          "Email sender not properly initialized. Attempt to retrieve email template but templates repository is not configured. Maybe forgot to add property |%s|.",
          PROP_REPOSITORY_PATH);
    }
    File templateFile = locale != null ? templatesPool.get(templateName, locale) : templatesPool.get(templateName);
    if(templateFile == null) {
      throw new EmailException(
          "Email template |%s| not found. Template name may be misspelled, forgot to add template file or template name does not match email files pattern.",
          templateName);
    }

    Template template = null;
    try {
      template = templateEngine.getTemplate(templateFile);
    }
    catch(IOException e) {
      throw new EmailException("Fail to load template |%s|.", templateFile);
    }

    log.debug("Create email from template |%s|.", template.getName());
    return new EmailImpl(this, template);
  }

  @Override
  public void send(String from, String to, String subject, String content)
  {
    if(developmentMode) {
      dumpAdHocEmail(from, to, subject, content);
      return;
    }

    openTransport();
    try {
      SMTPMessage message = new SMTPMessage(sessionFactory.getSession())
      {
        protected void updateMessageID() throws MessagingException
        {
          setHeader("Message-ID", new MessageID().getValue());
        }
      };

      message.setSentDate(new Date());
      message.setFrom(new InternetAddress(from));
      message.setEnvelopeFrom(message.getFrom()[0].toString());
      message.setReplyTo(message.getFrom());
      message.setRecipients(Message.RecipientType.TO, new Address[]
      {
          new InternetAddress(to, true)
      });
      message.setSubject(subject);
      message.setDataHandler(new DataHandler(content, DEF_CONTENT_TYPE));
      Transport.send(message);
    }
    catch(Exception e) {
      throw new EmailException(e);
    }
    finally {
      closeTransport();
    }
  }

  void send(Email emailInstance)
  {
    final EmailImpl email = (EmailImpl)emailInstance;
    if(email.to() == null) {
      throw new EmailException("Invalid email |%s|. Missing <to> recipient.", email);
    }
    if(email.body() == null) {
      throw new EmailException("Invalid email |%s|. Missing <body> content.", email);
    }

    // body content type from email instance or from sender default value
    String contentType = email.contentType();
    if(contentType == null) {
      contentType = this.contentType;
    }

    String subject = email.subject();
    if(subject == null) {
      subject = this.emailSubject;
      if(subject == null) {
        throw new EmailException("Invalid email |%s|. Missing email subject.", email);
      }
    }

    InternetAddress from = email.from();
    if(from == null) {
      from = this.fromAddress;
      if(from == null) {
        from = sessionFactory.getFromAddress();
        if(from == null) {
          throw new EmailException("Invalid email |%s|. Missing <from> address.", email);
        }
      }
    }

    String envelopeFrom = email.envelopeFrom();
    if(envelopeFrom == null) {
      envelopeFrom = this.envelopeFrom;
      if(envelopeFrom == null) {
        envelopeFrom = from.getAddress();
      }
    }

    InternetAddress[] replyTo = email.replyTo();
    if(replyTo == null) {
      replyTo = this.replyToAddresses;
      if(replyTo == null) {
        replyTo = new InternetAddress[]
        {
            from
        };
      }
    }

    if(developmentMode) {
      email.dump(from.getAddress(), envelopeFrom, contentType, subject);
      return;
    }

    openTransport();

    try {
      SMTPMessage message = new SMTPMessage(sessionFactory.getSession())
      {
        protected void updateMessageID() throws MessagingException
        {
          setHeader("Message-ID", email.messageID().getValue());
        }
      };

      message.setSentDate(new Date());
      message.setFrom(from);
      message.setEnvelopeFrom(envelopeFrom);
      message.setReplyTo(replyTo);

      // null recipients are valid for SMTPMessage but we still need a destination
      message.setRecipients(Message.RecipientType.TO, email.to());
      message.setRecipients(Message.RecipientType.BCC, email.bcc());
      message.setRecipients(Message.RecipientType.CC, email.cc());

      // null subject is valid in which case SMTPMessage remove existing subject, if any
      message.setSubject(subject);

      if(email.files() == null) {
        message.setDataHandler(new DataHandler(email.body(), contentType));
      }
      else {
        Multipart multipart = new MimeMultipart();

        BodyPart bodyPart = new MimeBodyPart();
        bodyPart.setDataHandler(new DataHandler(email.body(), contentType));
        multipart.addBodyPart(bodyPart);

        for(File file : email.files()) {
          BodyPart attachPart = new MimeBodyPart();
          attachPart.setDataHandler(new DataHandler(new FileDataSource(file)));
          attachPart.setFileName(file.getName());
          multipart.addBodyPart(attachPart);
        }

        message.setContent(multipart);
      }

      Transport.send(message);
    }
    catch(Exception e) {
      throw new EmailException(e);
    }
    finally {
      closeTransport();
    }
  }

  /**
   * Helper method to open transporter. Initialize transport and connect it; intercept checked exception and throw this
   * package specific exception.
   * 
   * @throws EmailException if connection is rejected.
   */
  private void openTransport()
  {
    try {
      transport = sessionFactory.getSession().getTransport();
      transport.connect();
    }
    catch(MessagingException e) {
      throw new EmailException(e);
    }
  }

  /**
   * Helper method to close transport. Catch any exceptions and just log to error.
   */
  private void closeTransport()
  {
    try {
      transport.close();
    }
    catch(MessagingException e) {
      log.error("Fatal error closing connection.", e);
    }
  }

  /**
   * Dump ad hoc email to standard out.
   * 
   * @param from sender address,
   * @param to destination address,
   * @param subject email subject,
   * @param content email content.
   */
  private static void dumpAdHocEmail(String from, String to, String subject, String content)
  {
    System.out.print("FROM: ");
    System.out.println(from);

    System.out.print("TO: ");
    System.out.println(to);

    System.out.print("SUBJECT: ");
    System.out.println(subject);

    System.out.println();
    System.out.println(content);
  }
}
