package com.jslib.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.mail.Address;
import javax.mail.Session;

import org.junit.Before;
import org.junit.Test;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.Element;
import com.jslib.api.email.Email;
import com.jslib.api.email.EmailSender;
import com.jslib.lang.Config;
import com.jslib.util.Classes;
import com.jslib.util.I18nPool;

public class SenderUnitTest
{
  private EmailSender sender;

  @Before
  public void beforeTest() throws Exception
  {
    sender = new EmailSenderImpl();

    Config config = new Config("test");
    config.setProperty("js.repository.path", "fixture/emails");
    config.setProperty("js.files.pattern", "*.html");
    config.setProperty("js.domain.bounce", "gnotis.ro");
    config.setProperty("js.template.engine", "com.jslib.template.xhtml.XhtmlTemplateEngine");
    config.setProperty("mail.transport.protocol", "smtp");
    config.setProperty("mail.smtp.host", "bbnet.ro");
    config.setProperty("mail.debug", "true");

    sender.config(config);
  }

  @Test
  public void templateDocumentsInitialization() throws IOException
  {
    I18nPool<Document> templatesPool = Classes.getFieldValue(sender, "templatesPool");
    assertNotNull(templatesPool.get("invoice-page"));
  }

  // TODO: work in progress
  public void sessionConfig() throws IOException
  {
    Session session = Classes.getFieldValue(sender, "session");
    assertTrue(session.getDebug());
    assertEquals("smtp", session.getProperty("mail.transport.protocol"));
    assertEquals("bbnet.ro", session.getProperty("mail.smtp.host"));
    assertEquals("true", session.getProperty("mail.debug"));
  }

  // TODO: work in progress
  public void defaultSessionProperties() throws Exception
  {
    sender = new EmailSenderImpl();

    Config config = new Config("test");
    config.setAttribute("repository", "fixture/emails");
    config.setAttribute("files-pattern", "*.html");
    config.setAttribute("bounce", "gnotis.ro");
    config.setProperty("mail.smtp.host", "localhost");

    sender.config(config);
    Session session = Classes.getFieldValue(sender, "session");

    assertFalse(session.getDebug());
    assertEquals("smtp", session.getProperty("mail.transport.protocol"));
    assertEquals("false", session.getProperty("mail.debug"));
  }

  // TODO: work in progress
  public void emailImplementationFields() throws Throwable
  {
    Person person = new Person();
    Email email = sender.getEmail("invoice-page");
    email.send(person);

    MessageID messageID = Classes.getFieldValue(email, "messageID");
    assertNotNull(messageID);
    assertEquals(32, messageID.getIdLeft().length());
    assertEquals("j(s)-lib", messageID.getIdRight());

    String contentType = Classes.getFieldValue(email, "contentType");
    assertNotNull(contentType);
    assertEquals("text/html; charset=UTF-8", contentType);

    String subject = Classes.getFieldValue(email, "subject");
    assertNotNull(subject);
    assertEquals("test subject", subject);

    Address from = Classes.getFieldValue(email, "from");
    assertNotNull(from);
    assertEquals("iulian@bbnet.ro", from.toString());

    Document document = Classes.getFieldValue(email, "document");
    assertNotNull(document);
    for(Field f : Person.class.getDeclaredFields()) {
      f.setAccessible(true);
      Element el = document.getByXPath("//*[data-value='?']", f.getName());
      if(el != null) {
        // Format format = bb.templates.format.Factory.getTypeFormat(f.getType());
        // assertEquals(format.format(f.get(person)), el.getText().trim());
      }
    }
  }
}
