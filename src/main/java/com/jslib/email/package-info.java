/**
 * Reference implementation of simple sender for email messages based on templates. This package is a thin adapter for
 * JavaMail implementation dealing only with email send; for complete mail functionality applications should use
 * JavaMail directly.
 * <p>
 * Email object contains the content and is based on a X(HT)ML Template that contain operators used to inject dynamic
 * content.
 * 
 * <pre>
 *  &lt;html&gt;
 *      &lt;body&gt;
 *          &lt;h3&gt;Dear &lt;span data-text="name"&gt;&lt;/span&gt;,&lt;/h3&gt;
 *          &lt;p&gt;Your account name is &lt;span data-text="account"&gt;&lt;/span&gt;.&lt;/h3&gt;
 *          . . .
 *      &lt;/body&gt;
 *  &lt;/html&gt;
 * </pre>
 * 
 * Using this package is straightforward: obtain new email instance from email sender - need to know template name, set
 * specific fields and send. On send gives object data containing dynamic content; in sample code <code>user</code>
 * instance has a field <code>name</code> that is injected into <code>body/h3/span</code> element and
 * <code>account</code> into <code>body/p/span</code> from above email template.
 * 
 * <pre>
 * class User
 * {
 *   private String name;
 *   private String account;
 *   private String emailAddr;
 * }
 * 
 * EmailSender sender = Classes.getService(EmailSender.class);
 * . . .
 * sender.getEmail("user-registration").subject("user registration").to(user.getEmailAddr()).send(user);
 * </pre>
 * 
 * Note that email instance is not reusable; create a new instance for every delivery.
 * <p>
 * If need to set many email properties, like long list CC and BCC it is convenient to use email model base class.
 * 
 * <pre>
 * class UserEmail extends EmailModel
 * {
 *   public UserEmail(User user)
 *   {
 *     super(user);
 *   }
 * 
 *   public String subject()
 *   {
 *     return "user registration";
 *   }
 * 
 *   public String to()
 *   {
 *     return user.getEmailAddress();
 *   }
 * 
 *   public String cc()
 *   {
 *     return "list of comma separated email addresses";
 *   }
 * }
 * . . .
 * sender.getEmail("user-registration").send(new UserEmail(user));
 * </pre>
 * 
 * <h3>Email Instance Fields</h3>
 * <p>
 * An email instance has internal fields that need to be initialized before sending; is quite common to have related
 * emails that differ on couple fields, perhaps on destination address only.
 * <ul>
 * <li>from - originator email address,
 * <li>envelopeFrom - this is reverse path used by remote agent to send back bounce message,
 * <li>to - list of email addresses for destinations,
 * <li>cc - list of email addresses for copy carbon destinations,
 * <li>bcc - list of email addresses for blind copy carbon destinations,
 * <li>subject - email subject,
 * <li>replyTo - a list of email addresses where email reply should be sent.
 * </ul>
 * If <code>envelopeFrom</code> and <code>replyTo</code> are missing <code>from</code> address is used instead.
 * 
 * @author Iulian Rotaru
 * @version 1.0
 */
package com.jslib.email;
