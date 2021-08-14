
import java.nio.charset.Charset
import java.util.regex.*

import org.apache.commons.codec.digest.DigestUtils

class AdobeUtil {
  //String path = "/content/acom/us/en/careers/why-adobe/jcr:content/modalcontent/standalonemodal_fa78";
  String generateModalId(final String nodePath) {

    String mid = "m" + DigestUtils.md5Hex(getEscapedPathForId(nodePath).getBytes(Charset.forName("UTF-8")))
    return mid
  }

  private  String getEscapedPathForId(final String path) {
    Pattern PATH_REPLACEMENT_PATTERN = Pattern.compile("[^a-zA-Z0-9]")
    Pattern PATH_MULTIPLE_UNDERSCORE_REPLACEMENT_PATTERN = Pattern.compile("__+")
    String tmp = PATH_REPLACEMENT_PATTERN.matcher(path).replaceAll("_")
    tmp = PATH_MULTIPLE_UNDERSCORE_REPLACEMENT_PATTERN.matcher(tmp).replaceAll("_")
    return tmp
  }
}

return new AdobeUtil()