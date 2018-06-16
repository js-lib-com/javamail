package js.email.javamail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.email.Email;
import js.email.EmailException;
import js.email.EmailProperties;
import js.lang.BugError;
import js.template.Template;
import js.util.Base64;
import js.util.Params;
import js.util.Strings;

/**
 * Implementation of {@link Email} interface.
 * 
 * @author Iulian Rotaru
 */
final class EmailImpl implements Email
{
  /** Email sender instance. */
  private final EmailSenderImpl sender;

  /** Source template file, merely for debugging. */
  private final File templateFile;

  /** Template. */
  private final Template template;

  /** Message ID instance. */
  private final MessageID messageID;

  /** Envelope from address. */
  private String envelopeFrom;

  /** Body content type as described into HTML template or {@link CT#DEF_CONTENT_TYPE default value}. */
  private final String contentType;

  /** Email subject. */
  private String subject;

  /** Sender address. */
  private InternetAddress from;

  /**
   * Recipients addresses hash map. Uses recipient type as map key and email addresses array as value. Recognized types
   * are: <code>to</code>, <code>cc</code> and <code>bcc</code>.
   */
  private final Map<String, InternetAddress[]> recipients = new HashMap<String, InternetAddress[]>();

  /** Reply to addresses. */
  private InternetAddress[] replyTo;

  /** Email content. */
  private String body;

  private File[] files;

  /**
   * Initialize this email instance fields from given HTML template. Constructor takes care to only initialize instance
   * fields using information stored into HTML head meta elements - see {@link Email} for meta element syntax and
   * supported fields.
   * <p>
   * Is considered flaw in logic if given <code>document</code> is not a valid email template.
   * 
   * @param sender parent email sender,
   * @param templateFile template file.
   */
  EmailImpl(EmailSenderImpl sender, File templateFile)
  {
    this.sender = sender;
    this.templateFile = templateFile;
    this.template = sender.getTemplate(templateFile);
    this.messageID = new MessageID(CT.DEF_MESSAGE_ID_RIGHT);

    // TODO: hack solution; refine it!!!
    // current email implementation relies on XHTML template provider and is not generic
    // it needs to read from address and subject from template itself, beside other meta data
    // need to find a way to move from address and subject elsewhere
    // maybe into email sender and allow multiple senders per application

    // TODO: brute force solution: load document every time new email is created

    DocumentBuilder builder = sender.getDocumentBuilder();
    Document document;
    try {
      document = builder.loadHTML(templateFile);
    }
    catch(FileNotFoundException e) {
      throw new EmailException("Fail to load template |%s|.", templateFile);
    }
    document.dump();
    Element head = document.getByTag("head");
    assert head != null;

    this.from = parseAddress(head.getByXPath("META[@name='from']"));
    if(sender.getBounceDomain() != null) {
      this.envelopeFrom = Strings.concat(Base64.encode(this.messageID.getValue()), "@", sender.getBounceDomain());
    }

    for(String recipient : new String[]
    {
        "to", "cc", "bcc"
    }) {
      EList addressesList = head.findByXPath("META[@name='%s']", recipient);
      if(addressesList.isEmpty()) {
        continue;
      }
      InternetAddress[] addresses = new InternetAddress[addressesList.size()];
      this.recipients.put(recipient, addresses);
      for(int i = 0; i < addressesList.size(); ++i) {
        addresses[i] = parseAddress(addressesList.item(i));
      }
    }

    EList replyToAddressesList = head.findByXPath("META[@name='reply-to']");
    if(!replyToAddressesList.isEmpty()) {
      this.replyTo = new InternetAddress[replyToAddressesList.size()];
      for(int i = 0; i < replyToAddressesList.size(); ++i) {
        this.replyTo[i] = parseAddress(replyToAddressesList.item(i));
      }
    }

    Element meta = head.getByXPath("META[@name='subject']");
    if(meta != null) {
      this.subject = meta.getAttr("content");
    }

    meta = head.getByXPath("META[@http-equiv='Content-Type']");
    this.contentType = meta != null ? meta.getAttr("content") : CT.DEF_CONTENT_TYPE;
  }

  @Override
  public Email set(EmailProperties properties)
  {
    Params.notNull(properties, "Properties");
    if(properties.hasFrom()) {
      from(properties.getFrom());
    }
    if(properties.hasEnvelopeFrom()) {
      envelopeFrom(properties.getEnvelopeFrom());
    }
    if(properties.hasReplyTo()) {
      replyTo(properties.getReplyTo());
    }
    if(properties.hasTo()) {
      to(properties.getTo());
    }
    if(properties.hasCc()) {
      cc(properties.getCc());
    }
    if(properties.hasBcc()) {
      bcc(properties.getBcc());
    }
    if(properties.hasSubject()) {
      subject(properties.getSubject());
    }
    return this;
  }

  @Override
  public Email from(String address)
  {
    Params.notNullOrEmpty(address, "From address");
    try {
      from = InternetAddress.parse(address)[0];
    }
    catch(AddressException e) {
      throw new EmailException(e);
    }
    return this;
  }

  @Override
  public Email envelopeFrom(String address)
  {
    Params.notNullOrEmpty(address, "Envelope from address");
    try {
      InternetAddress envelopeFromAddress = InternetAddress.parse(address)[0];
      if(envelopeFromAddress.getPersonal() != null) {
        throw new EmailException("Envelope address cannot have personal data.");
      }
      envelopeFrom = envelopeFromAddress.getAddress();
    }
    catch(AddressException e) {
      throw new EmailException(e);
    }
    return this;
  }

  @Override
  public Email to(String... addresses)
  {
    Params.notNullOrEmpty(addresses, "To addresses");
    try {
      recipients.put("to", InternetAddress.parse(Strings.join(addresses, ",")));
    }
    catch(AddressException e) {
      throw new EmailException(e);
    }
    return this;
  }

  @Override
  public Email cc(String... addresses)
  {
    Params.notNullOrEmpty(addresses, "CC addresses");
    try {
      recipients.put("cc", InternetAddress.parse(Strings.join(addresses, ",")));
    }
    catch(AddressException e) {
      throw new EmailException(e);
    }
    return this;
  }

  @Override
  public Email bcc(String... addresses)
  {
    Params.notNullOrEmpty(addresses, "BCC addresses");
    try {
      recipients.put("bcc", InternetAddress.parse(Strings.join(addresses, ",")));
    }
    catch(AddressException e) {
      throw new EmailException(e);
    }
    return this;
  }

  @Override
  public Email subject(String subject)
  {
    Params.notNullOrEmpty(subject, "Subject");
    this.subject = subject;
    return this;
  }

  @Override
  public Email replyTo(String... addresses)
  {
    Params.notNullOrEmpty(addresses, "Reply to address");
    try {
      replyTo = InternetAddress.parse(Strings.join(addresses, ","));
    }
    catch(AddressException e) {
      throw new EmailException(e);
    }
    return this;
  }

  @Override
  public Email file(File... files)
  {
    Params.notNullOrEmpty(files, "Attached files");
    this.files = files;
    return this;
  }

  @Override
  public void send(Object... object)
  {
    Params.LTE(object.length, 1, "Objects count");
    body = template.serialize(object.length == 1 ? object[0] : new Object());
    sender.send(this);
  }

  File getTemplateFile()
  {
    return templateFile;
  }

  /**
   * Return this email message ID.
   * 
   * @return message ID.
   */
  MessageID getMessageID()
  {
    return messageID;
  }

  /**
   * Get email envelope from address. If this email instance envelope from address was not explicitly set uses
   * <code>from</code> address. Returns null if <code>from</code> address is also null.
   * 
   * @return envelope from address or null.
   */
  String getEnvelopeFrom()
  {
    return envelopeFrom != null ? envelopeFrom : from != null ? from.getAddress() : null;
  }

  /**
   * Get email sender address.
   * 
   * @return sender address.
   */
  InternetAddress getFrom()
  {
    return from;
  }

  /**
   * Get email destination addresses.
   * 
   * @return destination addresses.
   */
  InternetAddress[] getTo()
  {
    return recipients.get("to");
  }

  /**
   * Get blind copy carbon recipient addresses.
   * 
   * @return blind copy carbon addresses.
   */
  InternetAddress[] getBcc()
  {
    return recipients.get("bcc");
  }

  /**
   * Get copy carbon recipient addresses.
   * 
   * @return copy carbon addresses.
   */
  InternetAddress[] getCc()
  {
    return recipients.get("cc");
  }

  /**
   * Get email subject or null if this email has no subject.
   * 
   * @return email subject, possible null.
   */
  String getSubject()
  {
    return subject;
  }

  /**
   * Get email content or null if injection was not performed yet. Email content is initialized by
   * {@link #inject(Object...)}; returns null if this getter is called before injection performed.
   * 
   * @return email content, possible null.
   */
  String getBody()
  {
    return body;
  }

  /**
   * Get address to respond to this email. If response address was not explicitly set uses <code>from</code> address.
   * Returns null if <code>from</code> is null.
   * 
   * @return response address or null.
   */
  InternetAddress[] getReplyTo()
  {
    return replyTo != null ? replyTo : from == null ? null : new InternetAddress[]
    {
      from
    };
  }

  public File[] getFiles()
  {
    return files;
  }

  /**
   * Get this email content type. Content type is initialized from HTML template <code>Content-Type</code> meta. If that
   * meta is missing uses {@link CT#DEF_CONTENT_TYPE}.
   * 
   * @return this email content type.
   */
  String getContentType()
  {
    assert contentType != null;
    return contentType;
  }

  @Override
  public String toString()
  {
    return Strings.toString(Strings.join(recipients.get("to")), subject);
  }

  /** Dump this email fields and content to standard out. */
  void dump()
  {
    try {
      dump(new OutputStreamWriter(System.out, "UTF-8"));
    }
    catch(UnsupportedEncodingException e) {
      throw new BugError("JVM with missing support for UTF-8.");
    }
  }

  /**
   * Dump this email fields and content to given writer.
   * 
   * @param writer writer to dump this email to.
   */
  void dump(Writer writer)
  {
    try {
      writer.append("FROM: ");
      writer.append(from != null ? from.toString() : null);
      writer.append("\r\n");

      writer.append("ENVELOPE FROM: ");
      writer.append(envelopeFrom != null ? envelopeFrom.toString() : null);
      writer.append("\r\n");

      if(recipients.get("to") != null) {
        writer.append("TO: ");
        writer.append(Strings.join(recipients.get("to")));
        writer.append("\r\n");
      }

      if(recipients.get("cc") != null) {
        writer.append("CC: ");
        writer.append(Strings.join(recipients.get("cc")));
        writer.append("\r\n");
      }

      if(recipients.get("bcc") != null) {
        writer.append("BCC: ");
        writer.append(Strings.join(recipients.get("bcc")));
        writer.append("\r\n");
      }

      writer.append("SUBJECT: ");
      writer.append(subject);
      writer.append("\r\n");

      writer.append("CONTENT TYPE: ");
      writer.append(contentType);
      writer.append("\r\n");

      writer.append("\r\n");
      writer.append(body);

      if(files != null) {
        writer.append("\r\n");
        for(File file : files) {
          writer.append("FILE: ");
          writer.append(file.getName());
          writer.append("\r\n");
        }
      }

      writer.flush();
    }
    catch(IOException ignore) {}
  }

  /**
   * Parse email address from given meta element. Extract <code>content</code> attribute from meta element and create a
   * strict email address, see {@link InternetAddress#InternetAddress(String, boolean)}. Throws logic flaw exception if
   * given meta element has no <code>content</code> attribute or contained email address is not strict valid.
   * <p>
   * Give meta element parameter can be null in which case this method simply returns null.
   * 
   * @param meta meta element, possible null.
   * @return newly created email address or null.
   * @throws BugError if <code>content</code> attribute is missing from meta element or if address value is not strict
   *           valid.
   */
  private static InternetAddress parseAddress(Element meta) throws BugError
  {
    if(meta == null) {
      return null;
    }
    String emailAddress = meta.getAttr("content");
    if(emailAddress == null) {
      throw new BugError("Invalid meta element |%s|. Missing <content> attribute.", meta);
    }
    try {
      return new InternetAddress(emailAddress, true);
    }
    catch(AddressException e) {
      throw new BugError("Invalid meta element |%s|. Bad email adress |%s|.", meta, emailAddress);
    }
  }

  // ----------------------------------------------------
  // package level methods

  boolean hasFromAddress()
  {
    return from != null;
  }

  void setFromAddress(InternetAddress from)
  {
    this.from = from;
  }
}
