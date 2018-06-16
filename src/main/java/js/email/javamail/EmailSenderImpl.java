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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import js.dom.DocumentBuilder;
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

import com.sun.mail.smtp.SMTPMessage;

/**
 * Email provider implementation based on Java Mail API.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class EmailSenderImpl implements EmailSender
{
  /** Class logger. */
  private static final Log log = LogFactory.getLog(EmailSenderImpl.class);

  /** Property key for development mode. This property value is used to initialize {@link #developmentMode} flag. */
  private static final String PROP_DEV_MODE = "js.dev.mode";
  /** Property key for templates repository path. Used to create {@link #templatesPool}. */
  private static final String PROP_REPOSITORY = "js.repository.path";
  /** Property key for files pattern from templates repository path. Used to create {@link #templatesPool}. */
  private static final String PROP_PATTERN = "js.files.pattern";
  /** Property key for template engine used to initialize {@link #templateEngine}. */
  private static final String PROP_TEMPLATE = "js.template.engine";
  /** Property key for bounce domain stored into {@link #bounceDomain} field. */
  private static final String PROP_BOUNCE = "js.bounce.domain";

  /** Mail session factory. */
  private SessionFactory sessionFactory;

  /**
   * In development mode email sender does not actually send email messages to email server but just dump email message
   * content to standard out. Development mode is activated by <code>js.dev.mode</code> property.
   */
  private boolean developmentMode;

  /**
   * Bounce domain is the domain where bounce messages are to be sent. It is used into <code>envelopeFrom</code> email
   * field and at SMTP level is processed by MAIL FROM command. Destination server uses this <code>envelopeFrom</code>
   * address to send back bounce messages, of course if email delivery fails.
   */
  private String bounceDomain;

  /** Mail transporter used to send emails. */
  private Transport transport;

  /** DOM builder to load template documents. */
  private DocumentBuilder documentBuilder;

  /** X(HT)ML template builder. */
  private TemplateEngine templateEngine;

  /** Multi-language template documents pool. Null if this email provider is configured without templates. */
  private I18nPool<File> templatesPool;

  /** Default <code>from</code> address used when email instance has none specified. */
  private InternetAddress fromAddress;

  public EmailSenderImpl()
  {
    log.trace("EmailProviderImpl()");
  }

  /**
   * Configure email processor instance. Scan email templates repository and initialize documents pool then create email
   * session instance. Emails configuration section looks like:
   * 
   * <pre>
   *    &lt;emails repository="path/to/email/templates/repository" files-pattern="*.html" bounce-domain="server.com"&gt;
   *        &lt;property name="JavaMail.property" value="JavaMail.value" /&gt;
   *    &lt;/emails&gt;
   * </pre>
   * 
   * Templates repository is optional if this processor is intended to send plain, i.e. not templates based emails - see
   * {@link #send(String, String, String, String)}. Anyway, if present, repository path should be valid on underlying
   * file system. Files pattern is optional, default to {@link #DEF_FILES_PATTERN}; it uses the characters '?' and '*'
   * to represent a single or multiple wild card characters. Bounce domain is the domain where bounce messages are to be
   * sent and is mandatory only if bounce handling is desired; if not set bounce handling is not enabled. Properties are
   * those listed in Appendix A of the JavaMail specification but this library offers sensible defaults:
   * <table>
   * <tr>
   * <td>mail.transport.protocol
   * <td>: smtp
   * <tr>
   * <td>mail.debug
   * <td>: true
   * </table>
   * Implementation note: if <code>mail.smtp.host</code> is not explicitly set this processor works in debugging mode.
   * Session is not created and email send methods just dump emails content to standard out.
   * 
   * @param configSections managed class specific configuration sections.
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

    // instantiate document builder and template engine
    documentBuilder = Classes.loadService(DocumentBuilder.class);
    String templateEngineProvider = config.getProperty(PROP_TEMPLATE);
    if(templateEngineProvider != null) {
      // if template engine provider is configured uses its class to create new instance
      templateEngine = Classes.newInstance(templateEngineProvider);
    }
    else {
      // if template engine is not configured load it via Java service loader
      templateEngine = Classes.loadService(TemplateEngine.class);
    }

    // scan and initialize templates repository
    String repositoryPath = config.getProperty(PROP_REPOSITORY);
    if(repositoryPath != null) {
      ConfigBuilder builder = new I18nRepository.ConfigBuilder(repositoryPath, config.getProperty(PROP_PATTERN));
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

    // prepare optional bounce domain; if property is not configured, bounce domain remains null
    bounceDomain = config.getProperty(PROP_BOUNCE);

    sessionFactory = new SessionFactory();
    sessionFactory.config(config);
  }

  @Override
  public void setFromAddress(String fromAddress)
  {
    Params.notNullOrEmpty(fromAddress, "Email from address");
    try {
      this.fromAddress = InternetAddress.parse(fromAddress)[0];
    }
    catch(AddressException e) {
      throw new IllegalArgumentException(String.format("Invalid email address |%s|.", fromAddress));
    }
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
          PROP_REPOSITORY);
    }
    File templateFile = locale != null ? templatesPool.get(templateName, locale) : templatesPool.get(templateName);
    if(templateFile == null) {
      throw new EmailException(
          "Email template |%s| not found. Template name may be misspelled, forgot to add template file or template name does not match email files pattern.",
          templateName);
    }
    EmailImpl email = new EmailImpl(this, templateFile);
    if(!email.hasFromAddress()) {
      email.setFromAddress(fromAddress);
    }
    log.debug("Create email from template |%s|.", templateFile);
    return email;
  }

  @Override
  public void send(String from, String to, String subject, String content)
  {
    if(developmentMode) {
      dumpEmail(from, to, subject, content);
      return;
    }

    openTransport();
    try {
      SMTPMessage message = new SMTPMessage(sessionFactory.getSession())
      {
        protected void updateMessageID() throws MessagingException
        {
          setHeader("Message-ID", new MessageID(CT.DEF_MESSAGE_ID_RIGHT).getValue());
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
      message.setDataHandler(new DataHandler(content, CT.DEF_CONTENT_TYPE));
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
    if(developmentMode) {
      email.dump();
      return;
    }

    if(email.getFrom() == null) {
      throw new EmailException("Invalid email. Missing <from> address. See |%s| template.", email.getTemplateFile());
    }
    if(email.getTo() == null) {
      throw new EmailException("Invalid email. Missing <to> recipient.");
    }
    if(email.getBody() == null) {
      throw new EmailException("Invalid email. Missing <body> content.");
    }
    openTransport();

    try {
      SMTPMessage message = new SMTPMessage(sessionFactory.getSession())
      {
        protected void updateMessageID() throws MessagingException
        {
          setHeader("Message-ID", email.getMessageID().getValue());
        }
      };

      message.setSentDate(new Date());
      message.setFrom(email.getFrom());

      message.setEnvelopeFrom(email.getEnvelopeFrom());

      message.setReplyTo(email.getReplyTo());

      // null recipients are valid for SMTPMessage but we still need a destination
      message.setRecipients(Message.RecipientType.TO, email.getTo());
      message.setRecipients(Message.RecipientType.BCC, email.getBcc());
      message.setRecipients(Message.RecipientType.CC, email.getCc());

      // null subject is valid in which case SMTPMessage remove existing subject, if any
      message.setSubject(email.getSubject());

      if(email.getFiles() == null) {
        message.setDataHandler(new DataHandler(email.getBody(), email.getContentType()));
      }
      else {
        Multipart multipart = new MimeMultipart();

        BodyPart bodyPart = new MimeBodyPart();
        bodyPart.setDataHandler(new DataHandler(email.getBody(), email.getContentType()));
        multipart.addBodyPart(bodyPart);

        for(File file : email.getFiles()) {
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

  DocumentBuilder getDocumentBuilder()
  {
    return documentBuilder;
  }

  Template getTemplate(File file)
  {
    try {
      return templateEngine.getTemplate(file);
    }
    catch(IOException e) {
      throw new EmailException("Fail to load template |%s|.", file);
    }
  }

  /**
   * Get bounce domain value or null if not configured. See {@link #bounceDomain} for bounce domain description.
   * 
   * @return this processor bounce domain.
   */
  String getBounceDomain()
  {
    return bounceDomain;
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
  private static void dumpEmail(String from, String to, String subject, String content)
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
