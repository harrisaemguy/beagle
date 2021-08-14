
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.*

import javax.jcr.SimpleCredentials

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.text.StringEscapeUtils
import org.apache.jackrabbit.commons.JcrUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Document.OutputSettings.Syntax
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.parser.ParseSettings
import org.jsoup.parser.Parser
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.helpers.XMLReaderFactory

import groovy.io.FileType

class CommonUtil {

  String projectDir

  //FileType: ANY,FILES,DIRECTORIES
  // name.matches('(?ui).*html.*'), ex: .*\\.yaml, guideContainer
  LinkedList<String> findRelativePaths(final File baseDir, FileType fileType, String fileNamePattern) {
    def relPaths = new LinkedList<String>()
    Path rootPath = Paths.get(baseDir.absolutePath)
    String ptn = '(?ui)' + fileNamePattern

    baseDir.eachFileRecurse (fileType) { file ->
      if(file.name.matches(ptn)) {
        String relPath = rootPath.relativize(Paths.get(file.absolutePath)).toString()
        relPaths.add(relPath.replace('\\','/'))
      }
    }

    return relPaths
  }

  // to create unique path/name
  String[] splitDigits(String str) {
    def trailingDigitstPat = Pattern.compile('(.*?)(\\d+)$')
    Matcher digitMatcher = trailingDigitstPat.matcher(str)
    if(digitMatcher.find()) {
      def txt = digitMatcher.group(1)
      def dd = digitMatcher.group(2)
      return [txt, dd] as String[]
    }
    return [str, '0'] as String[]
  }

  HashMap<String, String> findContentList(final File jcr_root_dir, String contentContains) {

    def contentpages = [:]
    Path rootPath = Paths.get(jcr_root_dir.absolutePath)

    jcr_root_dir.eachFileRecurse (FileType.FILES) { file ->
      if(file.name == '.content.xml') {
        String xml =  file2xml(file)
        if(xml.contains(contentContains)) {
          String relPath = rootPath.relativize(Paths.get(file.absolutePath)).toString()
          relPath = relPath.replace("\\", "/")
          contentpages.put('/'+relPath, '/'+relPath - '/.content.xml')
        }
      }
    }

    return contentpages
  }

  // read vpath from content inventory
  HashMap<String, String> readSrcCIList(final File ciFile) {

    def uniqueCIs = [:]
    def relPath
    ciFile.withReader { reader ->
      while ((relPath = reader.readLine())!=null) {
        if(relPath.startsWith('#') || !relPath) {
          continue
        }

        if(relPath.startsWith('/')) {
          uniqueCIs.put(relPath.substring(1) + '/.content.xml', relPath)
        } else {
          uniqueCIs.put(relPath + '/.content.xml', relPath)
        }
      }
    }

    //return uniqueCIs.values() as Collection<String>
    return uniqueCIs
  }

  Document xml2dom(String xml) {
    // bug of Jsoup.parse, when element name startsWith _
    xml = xml.replace('<_', '<V_').replace('</_', '</V_')
    Parser parser = Parser.xmlParser()
    parser.settings(new ParseSettings(true, true))
    Document doc = Jsoup.parse(xml, '', parser)
    return doc
  }

  def xmlAttr2Array(String value) {
    if(value.startsWith('[') && value.endsWith(']')) {
      value = value.substring(1, value.length()-1)
      value = value.replace('\\,', '[comma]')
      def result = value.split(',')
      def result1 = []
      result.each {
        result1.add(it.replace('[comma]', '\\,'))
      }
      return result1
    } else {
      return []
    }
  }

  def clone(node) {
    if(node) {
      def copy = node.clone()
      Parser parser = Parser.xmlParser()
      parser.settings(new ParseSettings(true, true))
      if(copy instanceof Document) {
        copy.parser(parser)
      } else {
        Document doc = new Document('.')
        doc.parser(parser)
        doc.appendChild(copy)
      }
      return copy
    }
    return null
  }

  String file2xml(final File srcFile) {
    def xmlIS = new FileInputStream(srcFile)
    String xml = IOUtils.toString(xmlIS, 'UTF-8')
    xmlIS.close()
    return xml
  }

  // list of attributes for cleanupSrcDom method
  def removeAttrs = [
    //    'jcr:created',
    //    'jcr:createdBy',
    //    'jcr:lastModified',
    //    'jcr:lastModifiedBy',
    //    'cq:lastModified',
    //    'cq:lastModifiedBy',
    //    'cq:lastReplicated',
    //    'cq:lastReplicatedBy',
    //    'cq:lastReplicationAction',
    //    'cq:lastRolledout',
    //    'cq:lastRolledoutBy',
    //    'jcr:isCheckedOut',
    //    'jcr:uuid'
  ]

  def cleanXML(String AbsXmlFile) {

    String newName = AbsXmlFile
    def xmlIS = new FileInputStream(AbsXmlFile)
    String xml = IOUtils.toString(xmlIS, 'UTF-8')
    xmlIS.close()

    def doc = xml2dom(xml)

    // remove jcr:created, jcr:createdBy ...
    cleanXmlAttributes(doc)

    def prettyXML = xmlPretty(doc)

    def filexOS = new FileOutputStream(newName)
    IOUtils.write(prettyXML, filexOS, 'UTF-8')
    filexOS.close()
  }

  def cleanXmlAttributes(Document doc) {
    doc.select('*').each { ele ->
      removeAttrs.each { key ->
        ele.removeAttr(key)
      }
    }
  }

  def cleanupPageDom(Document doc) {

    // remove sibling nodes of jcr:content
    doc.select('jcr|root>*').each { ele ->
      def tagName = ele.tagName()
      if('jcr:content' != tagName) {

        if(ele.attributes().isEmpty()) {
          println "...... orphan node: " + tagName
        }

        println "...... remove jcr:content sibling node from srcDoc: " + tagName
        ele.remove()
      }
    }

    // remove jcr:created, jcr:createdBy ...
    cleanXmlAttributes(doc)

    // remove ghost nodes
    doc.select('[sling:resourceType$=/ghost],[sling:resourceType*=.*/],[sling:resourceType$=$]').each {
      it.remove()
    }

    // child of parsys, must has resouceType
    doc.select('[sling:resourceType$=/parsys]>*:not([sling:resourceType])').each {
      println "...... remove child of parsys: " + getNodePath(it)
      it.remove()
    }

    // remove empty parsys
    doc.select('[sling:resourceType$=/parsys]:not(:has(*))').each {
      println "...... remove empty parsys: " + getNodePath(it)
      it.remove()
    }
  }

  String orderedFolder = '''
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="sling:OrderedFolder">
</jcr:root>
'''

  // baseDir = helpx
  def savePage(Document doc, String outputFile, final String baseDir) {

    File dirFile = new File(outputFile).parentFile
    FileUtils.forceMkdir(dirFile)

    // create sling:OrderedFolder nodes all the way to af folder
    while(dirFile && dirFile.parentFile) {

      // println '...page: ' + dirFile.absolutePath+'/.content.xml'
      def filex = dirFile.absolutePath+'/.content.xml'
      if(! new File(filex).exists()) {
        def docx = xml2dom(orderedFolder)
        String prettyXML = xmlPretty(docx)

        def filexOS = new FileOutputStream(filex)
        IOUtils.write(prettyXML, filexOS, 'UTF-8')
        filexOS.close()
      }

      // next loop
      dirFile = dirFile.parentFile
      if(dirFile.name == baseDir) {
        break
      }
    }

    String prettyXML = xmlPretty(doc)

    def filexOS = new FileOutputStream(outputFile)
    IOUtils.write(prettyXML, filexOS, 'UTF-8')
    filexOS.close()
  }

  String xmlPretty(Document doc) throws Exception {
    def outFormat = new OutputSettings().escapeMode(EscapeMode.xhtml).syntax(Syntax.xml).charset('UTF-8')
    doc.outputSettings(outFormat)
    String xml = doc.toString()

    AemSaxParser parser = new AemSaxParser()
    XMLReader xr = XMLReaderFactory.createXMLReader()
    xr.setContentHandler(parser)

    StringReader stringReader = new StringReader(xml.trim())
    xr.parse(new InputSource(stringReader))
    stringReader.close()
    String prettyXML= parser.output().replace('&gt;','>').replace('&apos;', "'")

    // bug of Jsoup.parse, when element name startsWith _
    prettyXML = prettyXML.replace('<V_', '<_').replace('</V_', '</_').trim()
    return prettyXML
  }

  // http://localhost:5502/content/cc1/us/en/jcr:content/root/content/position_e65f.infinity.json
  // http://localhost:5502/content/cc1/us/en/jcr:content/root/content/position_e65f.html
  String getNodeTextFromAEM(String aemNodeUrl, String username, String password) {
    def url = new URL(aemNodeUrl)
    URLConnection uc = url.openConnection()
    String userpass = username + ':' + password
    String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()))
    uc.setRequestProperty ("Authorization", basicAuth)
    return url.getText()
  }

  String generateMWSUUID() {
    UUID uuid = UUID.randomUUID()
    def randomUUID = uuid.toString()
    //return randomUUID
    return '12345678901234567890123456789012new'
  }

  String getNodePath(Element ele) {
    def navPath = '/' + ele.tagName()
    def suppers = ele.parents()
    for(int i=0; i< suppers.size(); i++) {
      def el = suppers.get(i)
      navPath = '/' + el.tagName() + navPath
      if('jcr:content' == el.tagName()) {
        break
      }
    }

    return navPath
  }

  // prevent duplicated sibling nodes on the same page
  // remove empty attribute
  def duplicateNodeDetect(Document doc) {

    def pathMap = [:]
    doc.select('*').each {
      if('pos' != it.tagName()) {
        def nodePath = getNodePath(it)

        while(pathMap.get(nodePath)) {

          // make unique element
          it.tagName(it.tagName() + '1')
          nodePath = getNodePath(it)
        }

        pathMap.put(nodePath, 'x')

        // remove empty attributes
        def attrIts = it.attributes().iterator()
        while(attrIts.hasNext()) {
          def attr = attrIts.next()
          if(StringUtils.isBlank(attr.value)) {
            attrIts.remove()
          }
        }
      }
    }

    def accordionNames = [:]
    doc.select('[accordionName]').each { accordion ->
      def accdName = accordion.attr('accordionName')

      while(accordionNames.get(accdName)) {

        // make unique
        accordion.attr('accordionName', accdName + '1')
        accdName = accordion.attr('accordionName')
      }

      accordionNames.put(accdName, 'x')
    }
  }

  // logout after entire download
  def getJcrSession(String aemHost, int aemPort, String aemUser, String aemPass) {
    String AEM_REPOSITORY_URI = "http://${aemHost}:${aemPort}/crx/server"
    def repository = JcrUtils.getRepository(AEM_REPOSITORY_URI)
    def session = repository.login(new SimpleCredentials(aemUser, aemPass.toCharArray()))
    return session
  }

  // use to download each asset if the package manager does not work
  def _dam_Asset = null
  def downloadAsset(Object session, String assetPath, String assetDir) throws Exception {

    // download asset /content/dam/help/en/after-effects/using/expression-language-reference/jcr:content/main-pars/image_0/ex_12.png
    if (session.nodeExists(assetPath + "/jcr:content/renditions/original/jcr:content")) {

      def assetXmlFilepath = assetDir + assetPath
      def assetImgFilepath = assetXmlFilepath + '/_jcr_content/renditions/original'
      FileUtils.forceMkdir(new File(assetImgFilepath).parentFile)

      javax.jcr.Node node = session.getNode(assetPath + "/jcr:content/renditions/original/jcr:content")
      InputStream inputStream = node.getProperty("jcr:data").getBinary().getStream()
      FileOutputStream targetStream = new FileOutputStream(assetImgFilepath, false)
      IOUtils.copy(inputStream, targetStream)
      inputStream.close()
      targetStream.close()

      if(_dam_Asset == null) {
        def xmlIS = new FileInputStream("${projectDir}/targetTemplate/dam_Asset.xml")
        String xml = IOUtils.toString(xmlIS, 'UTF-8')
        xmlIS.close()
        _dam_Asset = xml2dom(xml)
      }

      def _dam_Asset1 = clone(_dam_Asset)
      // 1. read properties from /jcr:content node
      def damEl = _dam_Asset1.selectFirst('jcr|root>jcr|content')
      javax.jcr.Node damNode = session.getNode(assetPath + "/jcr:content")
      loadJcrProperties(damNode, damEl)
      // 2. read properties from /jcr:content/metadata node
      def mtEl = _dam_Asset1.selectFirst('jcr|root>jcr|content>metadata')
      javax.jcr.Node mtNode = session.getNode(assetPath + "/jcr:content/metadata")
      loadJcrProperties(mtNode, mtEl)

      cleanXmlAttributes(_dam_Asset1)
      String prettyXML = xmlPretty(_dam_Asset1)
      def filexOS = new FileOutputStream("${assetXmlFilepath}/.content.xml")
      IOUtils.write(prettyXML, filexOS, 'UTF-8')
      filexOS.close()
      //FileUtils.copyFile(new File("${projectDir}/targetTemplate/dam_Asset.xml"), new File("${assetXmlFilepath}/.content.xml"))
    } else {
      println 'missing ' + assetPath
    }
  }

  def loadJcrProperties(javax.jcr.Node jcrNode, Element el) {
    jcrNode.properties.each { prop ->
      def valPrefix = ''
      switch(prop.type) {
        case javax.jcr.PropertyType.LONG:
          valPrefix = '{Long}'
          break
        case javax.jcr.PropertyType.DOUBLE:
          valPrefix = '{Double}'
          break
        case javax.jcr.PropertyType.DECIMAL:
          valPrefix = '{Decimal}'
          break
        case javax.jcr.PropertyType.BOOLEAN:
          valPrefix = '{Boolean}'
          break
        default:
          break
      }
      boolean isMultiple = prop.isMultiple()
      def val = ''
      if(isMultiple) {
        prop.values.each {
          if(val) {
            val += ','
          }
          val += it.string
        }
        el.attr(prop.name, valPrefix + '[' + val + ']')
      } else {
        val = prop.value.string
        el.attr(prop.name, valPrefix + val)
      }
    }
  }

  int getInteger(String encoded) {
    encoded=encoded?:''
    return ('0'+encoded.replaceAll('[^\\d]', '')).toInteger()
  }

  // {Long}56
  // {Boolean}true
  String readAttrVal(Element xmlEle, String xmlAttr) {
    return xmlEle.hasAttr(xmlAttr) ? xmlEle.attr(xmlAttr) : ''
  }

  String getStringFromHex(String hexStr)
  {
    String hex = hexStr?.replaceAll('[^\\d]', '').trim()
    if(hex == null) {
      return ''
    }
    StringBuilder output = new StringBuilder()
    for (int i = 0; i < hex.length(); i+=4) {
      String str = hex.substring(i, i+4)
      output.append((char)Integer.parseInt(str, 16))
    }
    return output.toString()
  }

  def removeAllAttrs(Document doc) {
    doc.select('*').each { ele ->

      Attributes attrs = ele.attributes()

      def tmp=[]
      attrs.each { attr ->
        tmp.add( attr.key)
      }

      tmp.each { key ->
        if(key!='sling:resourceType') {
          attrs.remove(key)
        }
      }
    }
  }

  def Map<String, String> getStyleMap(Element element) {
    Map<String, String> keymaps = new HashMap<>()
    if (!element.hasAttr("style")) {
      return keymaps
    }
    String styleStr = element.attr("style")
    String[] keys = styleStr.split(":")
    String[] split
    if (keys.length > 1) {
      for (int i = 0; i < keys.length; i++) {
        if (i % 2 != 0) {
          split = keys[i].split(";")
          if (split.length == 1) break
            keymaps.put(split[1].trim(), keys[i + 1].split(";")[0].trim())
        } else {
          split = keys[i].split(";")
          if (i + 1 == keys.length) break
            keymaps.put(keys[i].split(";")[split.length - 1].trim(), keys[i + 1].split(";")[0].trim())
        }
      }
    }
    return keymaps
  }

  def ptnSrcPageRef = Pattern.compile("[\\\"|\\'](\\/content\\/[^\\\"\\']+)")
  def gatherSrcPageRefs(final String data, final HashMap<String, String> refs) {
    def tmpStr = StringEscapeUtils.unescapeHtml4(data)
    Matcher digitMatcher = ptnSrcPageRef.matcher(tmpStr)
    while (digitMatcher.find()) {
      def refPath = digitMatcher.group(1)
      // decode %20, escape &
      refPath = groovy.xml.XmlUtil.escapeXml(refPath)
      refPath = URLDecoder.decode(refPath)
      if(!refPath.startsWith('/content/dam')) {
        refs.put(refPath, refPath-'.html')
      }
    }
  }

  // update links
  String postMappingAssetlinks(String docString, final def srcAssetsPtn, final def destAssetsPtn, final def srcPagesPtn, final def destPagesPtn) {

    // step 1: remap asset ref
    def srcDestMap_1 = [:] as HashMap<String, String>
    gatherDamAsset(docString, srcDestMap_1)
    srcDestMap_1.each { key, value ->
      def dest = key.replaceAll(srcAssetsPtn, destAssetsPtn)
      srcDestMap_1.put(key, dest)
    }

    // step 2: remap page ref
    def srcDestMap_2 = [:] as HashMap<String, String>
    gatherSrcPageRefs(docString, srcDestMap_2)
    srcDestMap_2.each { key, value ->
      docString = docString.replace(key, value)

      def dest = value.replaceAll(srcPagesPtn, destPagesPtn)
      srcDestMap_1.put(value, dest)
      // println '....'+value+':    ' + dest
    }

    // remap page and asset
    srcDestMap_1.each { src, dest ->
      docString = docString.replace(src, dest)
    }

    return docString
  }
}

class AemSaxParser extends DefaultHandler {

  private StringBuilder stringBuilder = new StringBuilder()
  private StringBuilder prefixMapping = new StringBuilder()

  private String latestTag = ""
  private boolean isRootElement = true

  private int indent = 0
  private void increase () {
    indent += 4
  }

  private void decrease () {
    if(indent>3) {
      indent -= 4
    }
  }

  private LinkedHashMap<String,String> sortElementAttr(Attributes attributes) {
    def sortedMap = [:] as LinkedHashMap<String,String>
    TreeMap<String, String> map1 = new TreeMap<>()
    TreeMap<String, String> map2 = new TreeMap<>()

    for(int i=0; i<attributes.getLength(); i++) {
      def attName = attributes.getQName(i)
      def attValue = StringEscapeUtils.escapeXml10(attributes.getValue(i)).replace('\\\\\\&quot;', '\\\\&quot;')
      if(attName.contains(':')) {
        map1.put(attName, attValue)
      } else {
        map2.put(attName, attValue)
      }
    }

    map1.each { key, val ->
      sortedMap.put(key, val)
    }

    map2.each { key, val ->
      sortedMap.put(key, val)
    }

    return sortedMap
  }

  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    stringBuilder.append('\n').append(" ".multiply(indent)).append('<').append(qName)
    //update latestTag in order to close immediate
    latestTag = " ".multiply(indent) + qName
    if(indent==0) {
      stringBuilder.append(prefixMapping.toString())
    }

    def sortedMap = sortElementAttr(attributes)
    def attrSize = sortedMap.size()

    if(attrSize>1 || isRootElement) {
      increase()
      sortedMap.each { k,v ->
        stringBuilder.append('\n').append(" ".multiply(indent)).append(k).append('="').append(v).append('"')
      }
      decrease()
    } else if(attrSize == 1) {
      sortedMap.each { k,v ->
        stringBuilder.append(' ').append(k).append('="').append(v).append('"')
      }
    }
    stringBuilder.append('>')

    increase()

    isRootElement = false
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    decrease()

    if(latestTag.equals(" ".multiply(indent) + qName)) {
      stringBuilder.insert(stringBuilder.size()-1, '/')
    } else {
      stringBuilder.append('\n').append(" ".multiply(indent)).append('</').append(qName).append('>')
    }
  }

  @Override
  public void startDocument() throws SAXException {
    stringBuilder.append('<?xml version="1.0" encoding="UTF-8"?>')
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    prefixMapping.append(' xmlns:').append(prefix).append('="').append(uri).append('"')
  }

  public String output() {
    return stringBuilder.toString()
  }
}

return new CommonUtil()
