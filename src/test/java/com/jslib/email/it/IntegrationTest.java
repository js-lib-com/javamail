package com.jslib.email.it;

import java.io.File;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Ignore;

import com.jslib.api.email.Email;
import com.jslib.api.email.EmailException;
import com.jslib.api.email.EmailSender;
import com.jslib.email.EmailSenderImpl;
import com.jslib.lang.BugError;
import com.jslib.lang.Config;

@Ignore
public class IntegrationTest
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
    config.setProperty("js.template.engine", "js.template.xhtml.XhtmlTemplateEngine");
    config.setProperty("mail.transport.protocol", "smtp");
    config.setProperty("mail.debug", "true");

    sender.config(config);
  }

  public void testClassicEmailSending() throws Exception
  {
    Person person = new Person();
    sender.getEmail("invoice-page").send(person);
  }

  public void testInlineEmailSending() throws BugError, EmailException, AddressException
  {
    Person person = new Person();
    sender.getEmail("invoice-page").send(person);
  }

  public void testSendEmailFromFactory() throws BugError, EmailException, AddressException
  {
    Email email = sender.getEmail("invoice-page");
    email.send(new Person());
  }

  public void testSendAdHocEmail()
  {
    sender.send("iulian@gnotis.ro", "iuli@localhost", "subject", "content");
  }

  public void testSendFile()
  {
    Person person = new Person();
    sender.getEmail("user-registration").file(new File("fixture/file1.jpg"), new File("fixture/file2.jpg")).send(person);
  }

  public void testSendWithFieldsFromTemplate()
  {
  }

  public void testSendWithFieldsSetProgrammatically()
  {
  }

  @SuppressWarnings("unused")
  private static final class Person
  {
    String subject;
    InternetAddress from;
    InternetAddress[] to;
    InternetAddress[] cc;
    InternetAddress[] bcc;
    String name;
    String picture;

    Person()
    {
      try {
        this.subject = "test subject";
        this.from = new InternetAddress("iulian@bbnet.ro");
        this.to = new InternetAddress[]
        {
            new InternetAddress("iulian@gnotis.ro")
        };
        this.cc = new InternetAddress[]
        {
            new InternetAddress("mr.iulianrotaru@yahoo.com")
        };
        this.bcc = new InternetAddress[]
        {
            new InternetAddress("johndoe@email.server.com")
        };

        this.name = "Iulian Rotaru";
        this.picture = "image.png";
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
}
