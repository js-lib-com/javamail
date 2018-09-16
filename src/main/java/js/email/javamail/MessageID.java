package js.email.javamail;

import java.io.Serializable;
import java.util.UUID;

import js.lang.SyntaxException;
import js.util.Strings;

/**
 * Immutable RFC2822 message identifier. This is a unique message identifier that refers to a particular version of a
 * particular message. It is structured as bellow:
 * 
 * <pre>
 * msg-id = [CFWS] "&lt;" id-left "@" id-right "&gt;" [CFWS]
 * </pre>
 * 
 * where optional <code>CFWS</code> means comment, folding or white-space whereas, in our case, <code>id-left</code> is
 * a random UUID and <code>id-right</code> is a mailer exchange unique identifier. Note that the same
 * <code>id-left</code> is used to set envelope sender, as described by VERP algorithm.
 * 
 * @author Iulian Rotaru
 */
public final class MessageID implements Serializable
{
  /** Java serialization version. */
  private static final long serialVersionUID = -5486610028503520871L;

  /** Default message ID right part is this library identity. */
  private static final String DEF_MESSAGE_ID_RIGHT = "j(s)-lib";

  /** A random UUID used as message ID left part. */
  private final String idLeft;

  /** Mailer exchange unique identifier used as message ID right part. */
  private final String idRight;

  /** Formatted value of this message ID. See class description for value format. */
  private final String value;

  /** Cached value for hash code. This caching is possible because message ID instance is immutable. */
  private final int hashCode;

  /** Default constructor. */
  public MessageID()
  {
    this(DEF_MESSAGE_ID_RIGHT);
  }

  /**
   * Construct a message ID from given string value - RFC2822 message ID or message right part. If <code>value</code>
   * parameter starts with angular bracket it should be a valid RFC2822 message ID, otherwise syntax exception is
   * thrown. If not, it is considered message right part and for left part uses a generated UUID.
   * 
   * @param value RFC2822 message ID or message right part.
   * @throws SyntaxException if given <code>value</code> starts with an angular but is not well formed.
   */
  public MessageID(String value) throws SyntaxException
  {
    // if given parameter does not start with angular bracket it is considered right part
    if(!value.startsWith("<")) {
      this.idLeft = UUID.randomUUID().toString().replace("-", "");
      this.idRight = value;
      this.value = buildValue(this.idLeft, this.idRight);
    }
    else {
      if(!value.endsWith(">")) {
        throw new SyntaxException("Missing trailing angular bracket.");
      }
      int index = value.indexOf('@');
      if(index == -1) {
        throw new SyntaxException("Missing '@' separator.");
      }
      this.idLeft = value.substring(1, index).trim();
      this.idRight = value.substring(index + 1, value.length() - 1).trim();
      this.value = value;
    }
    this.hashCode = computeHashCode(this.idLeft, this.idRight);
  }

  /**
   * Construct a message ID instance from given left and right parts.
   * 
   * @param idLeft left part,
   * @param idRight right part.
   */
  public MessageID(String idLeft, String idRight)
  {
    this.idLeft = idLeft;
    this.idRight = idRight;
    this.value = buildValue(this.idLeft, this.idRight);
    this.hashCode = computeHashCode(this.idLeft, this.idRight);
  }

  /**
   * Get message ID left part.
   * 
   * @return this message ID left part.
   */
  public String getIdLeft()
  {
    return idLeft;
  }

  /**
   * Get message ID right part.
   * 
   * @return this message ID right part.
   */
  public String getIdRight()
  {
    return idRight;
  }

  /**
   * Return this message ID formatted value. For value syntax see {@link MessageID class description}.
   * 
   * @return this message ID formatted value.
   */
  public String getValue()
  {
    return value;
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj)
  {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    MessageID other = (MessageID)obj;
    if(this.idLeft == null) {
      if(other.idLeft != null) return false;
    }
    else if(!this.idLeft.equals(other.idLeft)) return false;
    if(this.idRight == null) {
      if(other.idRight != null) return false;
    }
    else if(!this.idRight.equals(other.idRight)) return false;
    return true;
  }

  /** Cached value for string representation. This caching is possible because message ID instance is immutable. */
  private String string;

  @Override
  public String toString()
  {
    if(string == null) {
      string = Strings.toString(idLeft, idRight);
    }
    return string;
  }

  // ----------------------------------------------------
  // utility functions

  /**
   * Build message ID value into RFC 2822 format for given left and right message ID components.
   * 
   * @param idLeft message ID left part,
   * @param idRight message ID right part.
   * @return RFC2822 message ID.
   */
  private static String buildValue(String idLeft, String idRight)
  {
    return Strings.concat("<", idLeft, "@", idRight, ">");
  }

  /**
   * Compute hash code for requested left and right message ID components.
   * 
   * @param idLeft message ID left part,
   * @param idRight message ID right part.
   * @return computed hash code.
   */
  private static int computeHashCode(String idLeft, String idRight)
  {
    final int prime = 31;
    int hashCode = 1;
    hashCode = prime * hashCode + ((idLeft == null) ? 0 : idLeft.hashCode());
    hashCode = prime * hashCode + ((idRight == null) ? 0 : idRight.hashCode());
    return hashCode;
  }
}
