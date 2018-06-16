package js.email.javamail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;

import js.email.Email;
import js.email.EmailSender;
import js.lang.Config;
import js.util.Base64;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

public class EmailUnitTest
{
  private EmailSender sender;

  @Before
  public void beforeTest() throws Exception
  {
    sender = new EmailSenderImpl();
    sender.config(getConfig());
  }

  @Test
  public void emailAddressConstruction() throws UnsupportedEncodingException, AddressException
  {
    InternetAddress emailAddr = new InternetAddress("iuli@bbnet.ro", "Iulian Rotaru");
    assertEquals("rfc822", emailAddr.getType());
    assertEquals("Iulian Rotaru <iuli@bbnet.ro>", emailAddr.toString());
    assertEquals("Iulian Rotaru", emailAddr.getPersonal());
    assertEquals("iuli@bbnet.ro", emailAddr.getAddress());
    assertEquals("Iulian Rotaru <iuli@bbnet.ro>", emailAddr.toString());

    emailAddr = new InternetAddress("iuli@bbnet.ro");
    assertNull(emailAddr.getPersonal());
    assertEquals("rfc822", emailAddr.getType());
    assertEquals("iuli@bbnet.ro", emailAddr.toString());
    assertEquals("iuli@bbnet.ro", emailAddr.getAddress());
    assertEquals("iuli@bbnet.ro", emailAddr.toString());
  }

  @Test(expected = AddressException.class)
  public void invalidEmailAddressContruction() throws AddressException
  {
    new InternetAddress("bad.email.address", true);
  }

  @Test
  public void emailFieldsInitializationFromTemplate() throws Throwable
  {
    Email email = getEmail(new File("fixture/email-meta.html"));
    assertEmail(email);
  }

  @Test
  public void emailFieldsInitializationFromTemplateWithFullEmailAddress() throws Throwable
  {
    Email email = getEmail(new File("fixture/email-from.html"));

    InternetAddress from = Classes.getFieldValue(email, "from");
    assertEquals("from@server.com", from.getAddress());
    assertEquals("Sales Deparment", from.getPersonal());
  }

  @Test
  public void emailFieldsProgrammaticInitialization() throws Throwable
  {
    String template = "<!DOCTYPE HTML>" + //
        "<html>" + //
        "<head>" + //
        "   <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" + //
        "</head>" + //
        "<body></body>" + //
        "</html>";

    sender.config(getConfig());
    Email email = createEmail(template);

    email.from("from@server.com");
    email.to("to0@server.com", "to1@server.com");
    email.cc("cc0@server.com", "cc1@server.com");
    email.bcc("bcc0@server.com", "bcc1@server.com");
    email.subject("test subject");
    email.replyTo("replyto0@server.com", "replyto1@server.com");

    assertEmail(email);
  }

  @Test
  public void emailFieldsOverriding() throws Throwable
  {
    String template = "<!DOCTYPE HTML>" + //
        "<html>" + //
        "<head>" + //
        "   <meta name='from' content='fake-from@server.com' />" + //
        "   <meta name='to' content='fake-to0@server.com' />" + //
        "   <meta name='to' content='fake-to1@server.com' />" + //
        "   <meta name='cc' content='fake-cc0@server.com' />" + //
        "   <meta name='cc' content='fake-cc1@server.com' />" + //
        "   <meta name='bcc' content='fake-bcc0@server.com' />" + //
        "   <meta name='bcc' content='fake-bcc1@server.com' />" + //
        "   <meta name='subject' content='fake test subject' />" + //
        "   <meta name='reply-to' content='fake-replyto0@server.com' />" + //
        "   <meta name='reply-to' content='fake-replyto1@server.com' />" + //
        "   <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" + //
        "</head>" + //
        "<body></body>" + //
        "</html>";

    sender.config(getConfig());
    Email email = createEmail(template);

    email.from("from@server.com");
    email.to("to0@server.com", "to1@server.com");
    email.cc("cc0@server.com", "cc1@server.com");
    email.bcc("bcc0@server.com", "bcc1@server.com");
    email.subject("test subject");
    email.replyTo("replyto0@server.com", "replyto1@server.com");

    assertEmail(email);
  }

  // TODO: work in progress
  public void emailFieldsFromInjectedObject() throws Throwable
  {
    Email email = getEmail(new File("fixture/empty-email.html"));
    email.send(new Person());

    assertEquals("test subject", Classes.invoke(email, "getSubject"));

    InternetAddress from = Classes.invoke(email, "getFrom");
    assertEquals("iulian@bbnet.ro", from.toString());

    InternetAddress[] to = Classes.invoke(email, "getTo");
    assertEquals(1, to.length);
    assertEquals("iulian@gnotis.ro", to[0].toString());

    InternetAddress[] cc = Classes.invoke(email, "getCc");
    assertEquals(1, cc.length);
    assertEquals("mr.iulianrotaru@yahoo.com", cc[0].toString());

    InternetAddress[] bcc = Classes.invoke(email, "getBcc");
    assertEquals(1, bcc.length);
    assertEquals("johndoe@email.server.com", bcc[0].toString());
  }

  // TODO: work in progress
  public void envelopeFromInitialization() throws Throwable
  {
    String template = "<!DOCTYPE HTML>" + //
        "<html>" + //
        "<head></head>" + //
        "<body></body>" + //
        "</html>";

    Email email = createEmail(template);

    String envelopeFrom = Classes.getFieldValue(email, "envelopeFrom");
    assertNotNull("Envelope from header is null.", envelopeFrom);
    int separatorIndex = envelopeFrom.indexOf('@');
    assertTrue(separatorIndex != -1);
    assertTrue(envelopeFrom.endsWith("@bbnet.ro"));
    String localPart = envelopeFrom.substring(0, separatorIndex);
    MessageID messageID = new MessageID(new String(Base64.decode(localPart)));
    assertEquals(messageID, Classes.invoke(email, "getMessageID"));

    email.envelopeFrom("iuli@bbnet.ro");
    assertEquals("iuli@bbnet.ro", Classes.getFieldValue(email, "envelopeFrom"));

  }

  private Email createEmail(String template) throws IOException
  {
    File templateFile = new File("fixture/emails/template-file.html");
    Strings.save(template, templateFile);
    return sender.getEmail(Files.basename(templateFile));
  }

  @Test
  public void send() throws Throwable
  {
    Email email = getEmail(new File("fixture/email-inject.html"));
    Person person = new Person();
    email.send(person);

    String body = Classes.invoke(email, "getBody");
    assertNotNull(body);
    assertFalse(body.contains("<head>"));
    assertFalse(body.contains("UTF-8"));
    assertTrue(body.contains("<H1>Iulian Rotaru</H1>"));
  }

  private Email getEmail(File file)
  {
    return Classes.newInstance("js.email.javamail.EmailImpl", sender, file);
  }

  private static Config getConfig()
  {
    Config config = new Config("test");
    config.setProperty("js.dev.mode", "true");
    config.setProperty("js.repository.path", "fixture/emails");
    config.setProperty("js.files.pattern", "*.html");
    config.setProperty("js.domain.bounce", "gnotis.ro");
    config.setProperty("js.template.engine", "js.template.xhtml.XhtmlTemplateEngine");
    config.setProperty("mail.transport.protocol", "smtp");
    config.setProperty("mail.smtp.host", "bbnet.ro");
    config.setProperty("mail.debug", "true");

    return config;
  }

  private static void assertEmail(Email email) throws Throwable
  {
    assertEquals("from@server.com", Classes.invoke(email, "getFrom").toString());
    assertEquals("from@server.com", Classes.invoke(email, "getEnvelopeFrom").toString());
    assertEquals("test subject", Classes.invoke(email, "getSubject"));
    assertEquals("text/html; charset=UTF-8", Classes.invoke(email, "getContentType"));

    Address[] to = Classes.invoke(email, "getTo");
    assertEquals(2, to.length);
    assertEquals("to0@server.com", to[0].toString());
    assertEquals("to1@server.com", to[1].toString());

    Address[] cc = Classes.invoke(email, "getCc");
    assertEquals(2, cc.length);
    assertEquals("cc0@server.com", cc[0].toString());
    assertEquals("cc1@server.com", cc[1].toString());

    Address[] bcc = Classes.invoke(email, "getBcc");
    assertEquals(2, bcc.length);
    assertEquals("bcc0@server.com", bcc[0].toString());
    assertEquals("bcc1@server.com", bcc[1].toString());

    Address[] replyto = Classes.invoke(email, "getReplyTo");
    assertEquals(2, replyto.length);
    assertEquals("replyto0@server.com", replyto[0].toString());
    assertEquals("replyto1@server.com", replyto[1].toString());

    MessageID messageID = Classes.invoke(email, "getMessageID");
    assertTrue(Pattern.matches("<[a-z0-9]{32}@j\\(s\\)\\-lib>", messageID.getValue()));
  }
}
