package com.jslib.email;

import java.net.URL;
import java.util.Date;

import javax.mail.internet.InternetAddress;

final class Person {
	String subject;
	InternetAddress from;
	InternetAddress[] to;
	InternetAddress[] cc;
	InternetAddress[] bcc;
	String name;
	String picture;
	InternetAddress emailAddr;
	URL webPage;
	Date birthday;
	State state;

	Person() {
		try {
			this.subject = "test subject";
			this.from = new InternetAddress("iulian@bbnet.ro");
			this.to = new InternetAddress[] { new InternetAddress("iulian@gnotis.ro") };
			this.cc = new InternetAddress[] { new InternetAddress("mr.iulianrotaru@yahoo.com") };
			this.bcc = new InternetAddress[] { new InternetAddress("johndoe@email.server.com") };

			this.name = "Iulian Rotaru";
			this.picture = "image.png";
			this.emailAddr = new InternetAddress("iulian@gnotis.ro");
			this.webPage = new URL("http://gnotis.ro");
			this.birthday = new Date();
			this.state = State.ACTIVE;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
