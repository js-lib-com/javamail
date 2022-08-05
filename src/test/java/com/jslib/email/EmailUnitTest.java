package com.jslib.email;

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

import com.jslib.api.email.Email;
import com.jslib.api.template.Template;
import com.jslib.api.template.TemplateEngine;
import com.jslib.lang.Config;
import com.jslib.util.Base64;
import com.jslib.util.Classes;
import com.jslib.util.Files;
import com.jslib.util.Strings;

public class EmailUnitTest
{
  private EmailSenderImpl sender;

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
  public void emailPropertiesSetters() throws Throwable
  {
    String template = "<!DOCTYPE HTML>" + //
        "<html>" + //
        "<head>" + //
        "   <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />" + //
        "</head>" + //
        "<body></body>" + //
        "</html>";

    sender.config(getConfig());
    EmailImpl email = createEmail(template);

    email.contentType("text/html; charset=UTF-8");
    email.envelopeFrom("from@server.com");

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

  private EmailImpl createEmail(String template) throws IOException
  {
    File templateFile = new File("fixture/emails/template-file.html");
    Strings.save(template, templateFile);
    return (EmailImpl)sender.getEmail(Files.basename(templateFile));
  }

  @Test
  public void send() throws Throwable
  {
    EmailImpl email = getEmail(new File("fixture/email-inject.html"));
    email.from("iuli@bbnet.ro").to("iulian@gnotis.ro").subject("test");
    
    Person person = new Person();
    email.send(person);

    String body = email.body();
    assertNotNull(body);
    assertFalse(body.contains("<head>"));
    assertFalse(body.contains("UTF-8"));
    assertTrue(body.contains("<H1>Iulian Rotaru</H1>"));
  }

  private EmailImpl getEmail(File file) throws IOException
  {
    TemplateEngine templateEngine = Classes.getFieldValue(sender, "templateEngine");
    Template template = templateEngine.getTemplate(file);
    return new EmailImpl(sender, template);
  }

  private static Config getConfig()
  {
    Config config = new Config("test");
    config.setProperty("js.dev.mode", "true");
    config.setProperty("js.repository.path", "fixture/emails");
    config.setProperty("js.files.pattern", "*.html");
    config.setProperty("js.template.engine", "com.jslib.template.xhtml.XhtmlTemplateEngine");
    config.setProperty("mail.transport.protocol", "smtp");
    config.setProperty("mail.smtp.host", "bbnet.ro");
    config.setProperty("mail.debug", "true");

    return config;
  }

  private static void assertEmail(EmailImpl email) throws Throwable
  {
    assertEquals("from@server.com", email.from().toString());
    assertEquals("from@server.com", email.envelopeFrom());
    assertEquals("test subject", email.subject());
    assertEquals("text/html; charset=UTF-8", email.contentType());

    Address[] to = email.to();
    assertEquals(2, to.length);
    assertEquals("to0@server.com", to[0].toString());
    assertEquals("to1@server.com", to[1].toString());

    Address[] cc = email.cc();
    assertEquals(2, cc.length);
    assertEquals("cc0@server.com", cc[0].toString());
    assertEquals("cc1@server.com", cc[1].toString());

    Address[] bcc = email.bcc();
    assertEquals(2, bcc.length);
    assertEquals("bcc0@server.com", bcc[0].toString());
    assertEquals("bcc1@server.com", bcc[1].toString());

    Address[] replyto = email.replyTo();
    assertEquals(2, replyto.length);
    assertEquals("replyto0@server.com", replyto[0].toString());
    assertEquals("replyto1@server.com", replyto[1].toString());

    MessageID messageID = email.messageID();
    assertTrue(Pattern.matches("<[a-z0-9]{32}@j\\(s\\)\\-lib>", messageID.getValue()));
  }
}
