/**
 * Reference implementation of simple sender for email messages based on templates.
 * Templates based email sender. This package is a thin adapter for JavaMail implementation dealing only with email send; 
 * for complete mail functionality applications should use JavaMail directly. Email object contains the content and all 
 * information needed for delivery and is based on a HTML {@link js.dom.template.Template} that can contain operators used 
 * to inject dynamic content. Also head meta elements are used to store data needed for send, like <em>from</em> and <em>subject</em>;
 * content type is also configured via template meta, but usually is UTF-8 which is the default value. To exclude template head
 * from resulting email add <code>data-exclude</code> attribute - see {@link js.dom.template.ExcludeOperator}.
 * 
 * <pre>
 *  &lt;html&gt;
 *      &lt;head data-exclude="true"&gt;
 *          &lt;meta name="from" content="customers@cloud.ro" /&gt;
 *          &lt;meta name="subject" content="cloud registration" /&gt;
 *          &lt;meta http-equiv="Content-Type" content="text/html; charset=UTF-8" /&gt;
 *      &lt;/head&gt;
 *      &lt;body&gt;
 *          &lt;h3&gt;Dear &lt;span data-text="name"&gt;&lt;/span&gt;,&lt;/h3&gt;
 *          &lt;p&gt;Your account name is &lt;span data-text="account"&gt;&lt;/span&gt;.&lt;/h3&gt;
 *          . . .
 *      &lt;/body&gt;
 *  &lt;/html&gt;
 * </pre> 
 * 
 * Using this package is straightforward: obtain new email instance from {@link js.email.EmailProcessor#newEmail(String)},
 * set specific fields, inject dynamic content and send email, see code snippet. There is an convenient way to inject and send
 * in a single call. On inject / send gives object data containing dynamic content; in sample code <code>user</code>
 * instance has a field <code>name</code> that is injected into <code>body/h3/span</code> element and <code>account</code>
 * into <code>body/p/span</code> from above email template.
 * <pre>
 *  class User implements EmailContent {
 *  	private String name;
 *  	private String account;
 *  	private String emailAddr;
 *  }
 *  
 *  EmailProcessor processor = Factory.getInstance(EmailProcessor.class);
 *  Email email = processor.newEmail("user-registration");
 *  
 *  email.to(user.getEmailAddr()).inject(user);
 *  processor.send(email);
 *  
 *  // convenient inject and send
 *  email.to(user.getEmailAddr()).send(user);
 * </pre>
 *  Note that email instance is not reusable; create a new instance for every delivery.
 * <h5>Content Injection</h5>
 * Above we see how user instance is injected in email body. An alternative solution, applicable when want to avoid creating data
 * object class, is the use use of {@link js.dom.template.ModelObject}, see sample below. Model object allows for gathering together
 * properties from multiple instances. 
 * <pre>
 *  ModelObject model = new ModelObject();
 *  model.put("name", userName);
 *  model.put("account", userAccount);
 *  email.to(userEmail).send(model);
 * </pre>
 * There is third way to inject dynamic content: by argument position. For this need to change email template and replace
 * named template variables with arguments by position. In our example replace <code>name</code> with <code>$0</code>, respective
 * <code>account</code> with <code>$1</code>. On email inject / send gives arguments in the proper order, in our case user 
 * name and account.
 * <pre>
 *  &lt;body&gt;
 *  	&lt;h3&gt;Dear &lt;span data-text="$0"&gt;&lt;/span&gt;,&lt;/h3&gt;
 *  	&lt;p&gt;Your account name is &lt;span data-text="$1"&gt;&lt;/span&gt;.&lt;/h3&gt;
 *  	. . .
 *  &lt;/body&gt;
 *      
 *  email.to(userEmail).send(userName, userAccount);
 * </pre>
 * <h5>Email Instance Fields</h5> 
 * An email instance has internal fields that need to be initialized before sending; is quite common to have related emails that differ
 * on couple fields, perhaps on destination address only. To go simple, common fields are stored into email template and only those specific
 * are set programmatically. Both template head meta and field setters have the same name; here is the list of supported fields: 
 * <ul>
 * <li>{@link js.email.Email#from(javax.mail.internet.InternetAddress) from} - originator email address,
 * <li>{@link js.email.Email#envelopeFrom(javax.mail.internet.InternetAddress) envelopeFrom} - this is reverse path used by remote agent to send back bounce message,
 * <li>{@link js.email.Email#to(javax.mail.internet.InternetAddress...) to} - list of email addresses for destinations,
 * <li>{@link js.email.Email#cc(javax.mail.internet.InternetAddress...) cc} - list of email addresses for copy carbon destinations,
 * <li>{@link js.email.Email#bcc(javax.mail.internet.InternetAddress...) bcc} - list of email addresses for blind copy carbon destinations,
 * <li>{@link js.email.Email#subject(String) subject} - email subject,
 * <li>{@link js.email.Email#replyTo(javax.mail.internet.InternetAddress...) replyTo} - a list of email addresses where email reply should be sent.
 * </ul>
 * If <code>envelopeFrom</code> and <code>replyTo</code> are missing <code>from</code> address is used instead. 
 * <p>
 * There is a third method to initialize email fields: {@link js.email.Email#inject(Object...)} method searches given object for fields with 
 * names from above list. If found, initializes email field from injected object. For example, if user object has a field named <code>to</code>
 * of type array of addresses, email instance <code>to</code> is initialized with it. Of course, fields injection is enabled only for not already set fields.
 * <pre>
 *  Person person = {
 *      private String subject;
 *      private InternetAddress from;
 *      private InternetAddress[] to;
 *      private InternetAddress[] cc;
 *      private InternetAddress[] bcc;
 *      . . .
 *      // object specific fields
 *  }
 *  . . .
 *  email.inject(person);
 *  // email.subject is initialized from person.subject, if not set yet
 *  // the same is true for from, to, cc and bcc
 * </pre>
 * <h5>Bounce Back</h5>
 * Envelope address is used for sending back notification about delivery failing. This package support a slightly changed version of
 * VERP algorithm, enabled if <code>bounce-domain</code> attribute is present into configuration section, see {@link js.email.javamail.EmailSenderImpl#config(java.util.List)}.
 * Email MessageID is encode Base64 and used as local part for <code>envelopeFrom</code> address. If bounce back, delivery failing notification address
 * is exactly <code>envelopeFrom</code> we set on email sent. Just extract local part an decode to get MesssageID for failing email. 
 * 
 * @author Iulian Rotaru
 * @version 1.0
 */
package js.email.javamail;

