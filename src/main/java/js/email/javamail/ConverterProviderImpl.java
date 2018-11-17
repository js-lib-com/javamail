package js.email.javamail;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import js.converter.Converter;
import js.converter.ConverterProvider;

/**
 * Provider for converters used by this email sender implementation. Converters are used to convert value types to and
 * from strings representation. A value type is a class that wrap a single value susceptible to be represented as a
 * single string.
 * <p>
 * Current implementation provides converters for:
 * <ul>
 * <li>{@link InternetAddress}
 * <li>{@link MessageID}
 * </ul>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class ConverterProviderImpl implements ConverterProvider
{
  /**
   * Get converters implemented by email sender. Returned value is map associating converter implementations to value
   * types. Map key is the value type and map value is related converter implementation. A value type is a class that
   * can be represented as a single value and can be converted to/from strings.
   * 
   * @return provided converters.
   */
  @Override
  public Map<Class<?>, Class<? extends Converter>> getConverters()
  {
    Map<Class<?>, Class<? extends Converter>> converters = new HashMap<>();
    converters.put(InternetAddress.class, InternetAddressConverter.class);
    converters.put(MessageID.class, MessageIDConverter.class);
    return converters;
  }
}
