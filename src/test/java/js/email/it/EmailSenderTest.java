package js.email.it;

import java.util.Properties;
import java.util.ServiceLoader;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import js.email.EmailSender;
import js.lang.Config;

public class EmailSenderTest
{
  @Test
  public void sendYandexEmail() throws Exception
  {
    EmailSender sender = ServiceLoader.load(EmailSender.class).iterator().next();

    Config config = new Config("config");
    config.setProperty("mail.transport.protocol", "smtp");
    config.setProperty("mail.smtp.host", "smtp.yandex.com");
    config.setProperty("mail.smtp.port", "465");
    config.setProperty("mail.smtp.auth", "true");
    config.setProperty("mail.smtp.starttls.enable", "true");
    config.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    config.setProperty("mail.debug", "true");

    // these properties are recognized by this sender implementation
    config.setProperty("js.email.user", "ticket-noreply@fluxclienti.ro");
    config.setProperty("js.email.password", "xsnmovrnuogktarc");

    sender.config(config);
    sender.send("ticket-noreply@fluxclienti.ro", "iulian@gnotis.ro", "js-email test", "js-email test");
    
    // --------------------------------------------------------------------------------------------
    // resource configuration for tomcat context 
    
    // <Resource
    // name="mail/session"
    // type="javax.mail.Session"
    // mail.transport.protocol="smtp"
    // mail.smtp.host="smtp.yandex.com"
    // mail.smtp.port="465"
    // mail.smtp.auth="true"
    // mail.smtp.starttls.enable="true"
    // mail.smtp.socketFactory.class="javax.net.ssl.SSLSocketFactory"
    // mail.smtp.from="ticket-noreply@fluxclienti.ro"
    // mail.smtp.user="ticket-noreply@fluxclienti.ro"
    // password="xsnmovrnuogktarc"
    // mail.debug="true"
    // />
  }

  @Test
  public void sendGoogleEmail() throws Exception
  {
    EmailSender sender = ServiceLoader.load(EmailSender.class).iterator().next();

    Config config = new Config("config");
    config.setProperty("mail.transport.protocol", "smtp");
    config.setProperty("mail.smtp.host", "smtp.gmail.com");
    config.setProperty("mail.smtp.port", "587");
    config.setProperty("mail.smtp.auth", "true");
    config.setProperty("mail.smtp.starttls.enable", "false");
    config.setProperty("mail.smtp.ssl.enable", "true");
    config.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    config.setProperty("mail.debug", "true");

    config.setProperty("mail.smtp.user", "iulian@gnotis.ro");
    config.setProperty("password", "mami1964");

    config.setProperty("js.email.user", "iulian@gnotis.ro");
    config.setProperty("js.email.password", "mami1964");

    sender.config(config);
    sender.send("iulian@gnotis.ro", "iuli@bbnet.ro", "flux test", "flux test");
  }

  @Test
  public void sendGoogleJavaMail() throws Exception
  {
    // final String from = "iulian@gnotis.ro";
    // final String password = "mami1964";

    final String from = "ticket-noreply@allquick.ro";
    final String password = "ticket-noreply123!@#";

    String to = "iuli@bbnet.ro";

    Properties pro = new Properties();
    pro.put("mail.smtp.host", "smtp.gmail.com");
    pro.put("mail.smtp.starttls.enable", "true");
    // pro.put("mail.smtp.ssl.enable", "true");
    pro.put("mail.smtp.auth", "true");
    pro.put("mail.smtp.port", "587");
    pro.put("mail.debug", "true");

    Session ss = Session.getInstance(pro, new javax.mail.Authenticator()
    {
      @Override
      protected PasswordAuthentication getPasswordAuthentication()
      {
        return new PasswordAuthentication(from, password);
      }
    });

    Message msg = new MimeMessage(ss);
    msg.setFrom(new InternetAddress(from));
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    msg.setSubject("flux test");
    msg.setText("java email app");
    Transport trans = ss.getTransport("smtp");
    Transport.send(msg);
    System.out.println("message sent");
  }
}
