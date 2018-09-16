package js.email.javamail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import js.email.Email;
import js.email.EmailException;
import js.email.EmailModel;
import js.lang.BugError;
import js.template.Template;
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

  /** Template. */
  private final Template template;

  /** Message ID instance. */
  private final MessageID messageID;

  /** Envelope from address. */
  private String envelopeFrom;

  /** Body content type. */
  private String contentType;

  /** Email subject. */
  private String subject;

  /** Sender address. */
  private InternetAddress from;

  /**
   * Recipients addresses hash map. Uses recipient type as map key and email addresses array as value. Recognized types
   * are: <code>to</code>, <code>cc</code> and <code>bcc</code>.
   */
  private final Map<String, InternetAddress[]> recipients = new HashMap<>();

  /** Reply to addresses. */
  private InternetAddress[] replyTo;

  /** Email content. */
  private String body;

  /** Attached files. */
  private File[] files;

  /**
   * Initialize this email instance fields from given HTML template. Constructor takes care to only initialize instance
   * fields using information stored into HTML head meta elements - see {@link Email} for meta element syntax and
   * supported fields.
   * <p>
   * Is considered flaw in logic if given <code>document</code> is not a valid email template.
   * 
   * @param sender parent email sender,
   * @param template email template.
   */
  EmailImpl(EmailSenderImpl sender, Template template)
  {
    this.sender = sender;
    this.template = template;
    this.messageID = new MessageID();
  }

  @Override
  public String templateName()
  {
    return template.getName();
  }

  @Override
  public Email contentType(String contentType)
  {
    Params.notNullOrEmpty(contentType, "Content type");
    this.contentType = contentType;
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
  public void send(Object... args)
  {
    Params.LTE(args.length, 1, "Objects count");
    Object object = args.length == 1 ? args[0] : new Object();

    if(object instanceof EmailModel) {
      final EmailModel email = (EmailModel)object;
      try {
        if(email.subject() != null) {
          subject(email.subject());
        }
        if(email.to() != null) {
          // EmailModel#to() returns a list of comma separated email addresses
          recipients.put("to", InternetAddress.parse(email.to()));
        }
        if(email.cc() != null) {
          // EmailModel#cc() returns a list of comma separated email addresses
          recipients.put("cc", InternetAddress.parse(email.cc()));
        }
        if(email.bcc() != null) {
          // EmailModel#bcc() returns a list of comma separated email addresses
          recipients.put("bcc", InternetAddress.parse(email.bcc()));
        }
        if(email.from() != null) {
          from(email.from());
        }
        if(email.envelopeFrom() != null) {
          envelopeFrom(email.envelopeFrom());
        }
        if(email.replyTo() != null) {
          // EmailModel#replyTo() returns a list of comma separated email addresses
          replyTo = InternetAddress.parse(email.replyTo());
        }
        if(email.contentType() != null) {
          contentType(email.contentType());
        }
      }
      catch(AddressException e) {
        throw new EmailException(e);
      }
      object = email.model();
    }

    body = template.serialize(object);
    sender.send(this);
  }

  @Override
  public String toString()
  {
    return template.getName();
  }

  // ----------------------------------------------------------------------------------------------
  // PACKAGE LEVEL METHODS

  /**
   * Return this email message ID.
   * 
   * @return message ID.
   */
  MessageID messageID()
  {
    return messageID;
  }

  /**
   * Get email envelope from address.
   * 
   * @return envelope from address or null.
   */
  String envelopeFrom()
  {
    return envelopeFrom;
  }

  /**
   * Get email sender address.
   * 
   * @return sender address.
   */
  InternetAddress from()
  {
    return from;
  }

  /**
   * Get email destination addresses.
   * 
   * @return destination addresses.
   */
  InternetAddress[] to()
  {
    return recipients.get("to");
  }

  /**
   * Get blind copy carbon recipient addresses.
   * 
   * @return blind copy carbon addresses.
   */
  InternetAddress[] bcc()
  {
    return recipients.get("bcc");
  }

  /**
   * Get copy carbon recipient addresses.
   * 
   * @return copy carbon addresses.
   */
  InternetAddress[] cc()
  {
    return recipients.get("cc");
  }

  /**
   * Get address to respond to this email. If response address was not explicitly set uses <code>from</code> address.
   * Returns null if <code>from</code> is null.
   * 
   * @return response address or null.
   */
  InternetAddress[] replyTo()
  {
    return replyTo != null ? replyTo : from == null ? null : new InternetAddress[]
    {
        from
    };
  }

  /**
   * Get email subject or null if this email has no subject.
   * 
   * @return email subject, possible null.
   */
  String subject()
  {
    return subject;
  }

  /**
   * Get email content or null if injection was not performed yet. Email content is initialized by
   * {@link #inject(Object...)}; returns null if this getter is called before injection performed.
   * 
   * @return email content, possible null.
   */
  String body()
  {
    return body;
  }

  File[] files()
  {
    return files;
  }

  /**
   * Get this email content type. Content type is initialized from HTML template <code>Content-Type</code> meta. If that
   * meta is missing uses {@link CT#DEF_CONTENT_TYPE}.
   * 
   * @return this email content type.
   */
  String contentType()
  {
    return contentType;
  }

  // ----------------------------------------------------------------------------------------------
  // DEBUG

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
}
