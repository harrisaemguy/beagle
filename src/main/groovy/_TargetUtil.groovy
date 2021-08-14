
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.ParseSettings
import org.jsoup.parser.Parser

import groovy.io.FileType

// 1. Both position and flex can have background image
// 2. Use flex to map multicolumn, use position to hold one of the column
// 3. Use position to hold responsive child node
class TargetUtil {

  def commonUtil
  String projectDir

  HashMap<String, Element> pageTemplates = new HashMap()
  HashMap<String, Element> allowedComponents = new HashMap()
  // load from pageTemplate/ to validate migrated pages
  def allowedResourceType = [:]

  def textStyle = [
    'no-margin' : '1597354796598',
    'body-XXL'  : '1570836905309',
    'body-XL'   : '1570836931098',
    'body-L'    : '1570836945856',
    'body-M'    : '1570836984205',
    'body-S'    : '1570836958369',
    'body-XS'   : '1570836973765',
    'body-XXS'  : '1597354981853',
    'heading-XXXL' : '1570835465762',
    'heading-XXL'  : '1570838134018',
    'heading-XL'   : '1597355010452',
    'heading-L'    : '1597355057100',
    'heading-M'    : '1597355065959',
    'heading-S'    : '1597355092038',
    'heading-XS'   : '1597355108719',
    'text-left' : '1597354825859',
    'text-center' : '1597354827731',
    'text-right' : '1597354828931'
  ];

  def positionStyle = [
    'size-Body1' : '1545256009523',
    'size-Body2' : '1545256022353',
    'size-Body3' : '1545256034345',
    'size-Body4' : '1545256041308',
    'size-Body5' : '1545256077615',
    'size-Body3Quiet' : '1552068744084',
    'head-quiet1' : '1552068784341',
    'head-quiet2' : '1552068763825',
    'noMargin' : '1555109297947',
    'noMarginTop' : '1555109298825',
    'noMarginBottom' : '1555109301415'
  ];

  def flexStyle = [
    'size-Body1' : '1554157322032',
    'size-Body2' : '1554157330330',
    'size-Body3' : '1554157335744',
    'size-Body4' : '1554157345941',
    'size-Body5' : '1554157352326',
    'size-Body3Quiet' : '1554157360129'
  ];

  def navlistStyle = [
    'heading1' : '1569462669860',
    'heading2' : '1569462671345',
    'heading3' : '1569462672867',
    'weight300' : '1569462673856',
    'weight400' : '1569462675547'
  ];

  Document getTgtTemplate(String tplName) {
    if(pageTemplates.isEmpty()) {
      _init()
    }

    def tpl = pageTemplates.get(tplName)?.clone()
    if(tpl) {
      Parser parser = Parser.xmlParser()
      parser.settings(new ParseSettings(true, true))
      tpl.parser(parser)
    }

    return tpl
  }

  Element getTgtComponent(String tplName) {
    if(allowedComponents.isEmpty()) {
      _init()
    }

    // wrap tpl clone with doc's parser, so that tag and attribute are case-sensitive,
    // needed since jsoup 1.12
    def tpl = allowedComponents.get(tplName)?.clone()
    if(tpl) {
      Parser parser = Parser.xmlParser()
      parser.settings(new ParseSettings(true, true))
      Document doc = new Document('.')
      doc.parser(parser)
      doc.appendChild(tpl)
    }

    return tpl
  }

  HashMap<String, Integer> removeInvalidResourceType(Element doc, boolean remove) {
    def InvalidResourceType = new HashMap<String, Integer>()
    doc?.select('[sling:resourceType]').each {
      def resourceType = it.attr('sling:resourceType')
      if(allowedResourceType.get(resourceType) == null) {
        int x = 1
        if(InvalidResourceType.get(resourceType)) {
          x = InvalidResourceType.get(resourceType) + 1
        }
        InvalidResourceType.put(resourceType, x)
        if(remove) {
          it.remove()
        }
      }
    }

    return InvalidResourceType
  }

  def _init() {
    String inputFolder = projectDir + '/targetTemplate'
    LinkedList<String> xmlFiles = commonUtil.findRelativePaths(new File(inputFolder), FileType.FILES, '.*\\.xml')
    xmlFiles.each { xmlFilename ->
      String inputFile = inputFolder + '/' + xmlFilename
      def xmlIS = new FileInputStream(inputFile)
      String xml = IOUtils.toString(xmlIS, 'UTF-8')
      xmlIS.close()
      Document doc = commonUtil.xml2dom(xml)

      doc.select('[sling:resourceType]').each {
        allowedResourceType.put(it.attr('sling:resourceType'), it.tagName())
      }

      if(xmlFilename.startsWith('allowedComponents')) {
        doc.select('jcr|root>*').each { ele ->
          allowedComponents.put(ele.tagName(), ele)
        }
      } else {
        pageTemplates.put(xmlFilename-'.xml', doc)
      }
    }
  }

  def savePage(Document doc, String outputFile, final def srcAssetsPtn, final def destAssetsPtn, final def srcFragPtn, final def destFragPtn, final def srcPagesPtn, final def destPagesPtn) {

    File dirFile = new File(outputFile).parentFile
    FileUtils.forceMkdir(dirFile)

    // create cq:Page nodes all the way to site folder
    while(dirFile && dirFile.parentFile) {

      // println '...page: ' + dirFile.absolutePath+'/.content.xml'
      def filex = dirFile.absolutePath+'/.content.xml'
      if(! new File(filex).exists()) {
        def docx = getTgtTemplate('cq_page')

        docx.selectFirst('jcr|root').attr('jcr:primaryType', 'cq:Page')
        docx.selectFirst('jcr|root>jcr|content').attr('jcr:title', dirFile.name).attr('jcr:primaryType','cq:PageContent')
        if(dirFile.parentFile.name == 'content') {
          def jcrNd = docx.selectFirst('jcr|root>jcr|content')
          jcrNd.attr('cq:allowedTemplates', '[/apps/help/templates/.*,/apps/helpx/templates/.*,/conf/helpx/settings/wcm/templates/((?!experience-fragment.*).)*,/conf/dexter/settings/wcm/templates/full-width-with-globalnav,/conf/dexter/settings/wcm/templates/full-width]')
          jcrNd.attr('cq:conf', '/conf/helpx')
        }

        def filexOS = new FileOutputStream(filex)
        IOUtils.write(commonUtil.xmlPretty(docx), filexOS, 'UTF-8')
        filexOS.close()
      }

      // next loop
      dirFile = dirFile.parentFile
      if(dirFile.name == 'content') {
        break
      }
    }

    // fragment reference remap
    doc.select('[fragmentPath]').each { frag ->
      def val = frag.attr('fragmentPath')
      val = val.replaceAll(srcFragPtn, destFragPtn)
      frag.attr('fragmentPath', val)
    }

    String prettyXML = commonUtil.xmlPretty(doc)
    //post map asset and link
    //prettyXML = commonUtil.postMappingAssetlinks(prettyXML, srcAssetsPtn, destAssetsPtn, srcPagesPtn, destPagesPtn)

    def filexOS = new FileOutputStream(outputFile)
    IOUtils.write(prettyXML, filexOS, 'UTF-8')
    filexOS.close()
  }

  // create asset from img file
  def boolean createAsset(final String srcFile, final String destDamDir) {

    def srcOriginal = srcFile + '/_jcr_content/renditions/original'
    if(! new File(srcOriginal).exists()) {
      // when original file missing, find the largest file
      def files = new File(srcFile + '/_jcr_content/renditions').listFiles()
      long fileSize = 0
      files.each { file ->
        if(file.isFile()) {
          if(file.length() > fileSize) {
            srcOriginal = file.absolutePath

            println '\n\nuse non original: ' + srcOriginal
            fileSize = file.length()
          }
        }
      }
    }

    if(new File(srcOriginal).exists()) {

      //create asset folder
      FileUtils.forceMkdir(new File(destDamDir + '/_jcr_content/renditions'))
      FileUtils.copyFile(new File(srcOriginal), new File(destDamDir + '/_jcr_content/renditions/original'))

      def dstDir = new File(destDamDir)
      // populate parent folder
      def dirFile = dstDir
      while(dirFile && dirFile.parentFile && dirFile.parentFile.name != 'dam') {
        //next loop
        dirFile = dirFile.parentFile

        //println '...dam folder: ' + dirFile.absolutePath+'/.content.xml'
        def filex = dirFile.absolutePath+'/.content.xml'
        // for folders
        if(new File(filex).exists()) {
          continue
        }

        //output to folder
        def docx = getTgtTemplate('sling_folder')
        docx.selectFirst('jcr|root').attr('jcr:primaryType', 'sling:OrderedFolder')
        //docx.selectFirst('jcr|root>jcr|content').remove()
        //docx.selectFirst('jcr|root').appendElement('jcr:content').attr('jcr:title', dirFile.name)

        def filexOS = new FileOutputStream(filex)
        IOUtils.write(commonUtil.xmlPretty(docx), filexOS, 'UTF-8')
        filexOS.close()
      }

      def damAsset = getTgtTemplate('dam_Asset')
      def imgParentPath = new File(destDamDir).parentFile.absolutePath
      def pos = imgParentPath.indexOf('/content/dam')
      imgParentPath = imgParentPath.substring(pos)

      damAsset.selectFirst('jcr|root>jcr|content').attr('cq:name', new File(destDamDir).name).attr('cq:parentPath', imgParentPath)

      def filexOS = new FileOutputStream(destDamDir+'/.content.xml')
      IOUtils.write(commonUtil.xmlPretty(damAsset), filexOS, 'UTF-8')
      filexOS.close()
      return true
    } else {
      println 'Missing source asset: ' + srcOriginal
      return false
    }
  }

  //create asset folder, and copy into it
  def assetFilter = new AssetFilter()
  def copyAsset(final String srcDamDir, final String destDamDir) {

    if(srcDamDir.contains(':')) {
      return
    }

    def srcDir = new File(srcDamDir)
    def dstDir = new File(destDamDir)
    if(srcDir.exists()) {
      //create asset folder, and copy into it
      FileUtils.forceMkdir(dstDir)
      FileUtils.copyDirectory(srcDir,dstDir,assetFilter)
      //remove extra properties
      cleanAsset(dstDir)

      // populate parent folder
      def dirFile = dstDir
      while(dirFile && dirFile.parentFile && dirFile.parentFile.name != 'dam') {
        //next loop
        dirFile = dirFile.parentFile

        //println '...dam folder: ' + dirFile.absolutePath+'/.content.xml'
        def filex = dirFile.absolutePath+'/.content.xml'
        // for folders
        if(new File(filex).exists()) {
          continue
        }

        //output to folder
        def docx = getTgtTemplate('sling_folder')
        docx.selectFirst('jcr|root').attr('jcr:primaryType', 'sling:OrderedFolder')
        docx.selectFirst('jcr|root>jcr|content').remove()
        docx.selectFirst('jcr|root').appendElement('jcr:content').attr('jcr:title', dirFile.name)

        def filexOS = new FileOutputStream(filex)
        IOUtils.write(commonUtil.xmlPretty(docx), filexOS, 'UTF-8')
        filexOS.close()
      }

      // remove invalid child folder
      def dstDirStr = dstDir.absolutePath
      dstDir.eachFileRecurse (FileType.DIRECTORIES) { dir ->
        if(dir.name.contains(':')) {
          dir.delete()
        }
      }

    } else {
      println 'Error: missing ' + srcDamDir
    }
  }

  def assetAttrs = [
    'cq:lastReplicated',
    'cq:lastReplicatedBy',
    'cq:lastReplicationAction',
    'jcr:lastModified',
    'jcr:lastModifiedBy',
    'jcr:created',
    'jcr:createdBy',
    'jcr:versionHistory',
    'jcr:predecessors',
    'jcr:baseVersion',
    'jcr:isCheckedOut'
  ]
  //remove extra attributes from .content.xml file
  def cleanAsset(final File destDamDir) {
    destDamDir.eachFileRecurse (FileType.FILES) { file ->
      if(file.name == '.content.xml') {
        String xml = commonUtil.file2xml(file)
        Document doc = commonUtil.xml2dom(xml)
        doc.select('*').each { ele ->
          assetAttrs.each { key ->
            ele.removeAttr(key)
          }

          if(ele.hasAttr('dam:relativePath')) {
            def absPath = destDamDir.absolutePath
            def pos = absPath.indexOf('/content/dam')
            if(pos != -1) {
              pos += 13
              ele.attr('dam:relativePath', absPath.substring(pos))
            }
          }
          if(ele.hasAttr('cq:parentPath')) {
            def absPath = destDamDir.absolutePath
            def pos = absPath.indexOf('/content/dam')
            if(pos != -1) {
              def pPath = absPath.substring(pos) - ('/'+destDamDir.name)
              ele.attr('cq:parentPath', pPath)
            }
          }
        }

        def filexOS = new FileOutputStream(file)
        IOUtils.write(commonUtil.xmlPretty(doc), filexOS, 'UTF-8')
        filexOS.close()
      }
    }
  }

  def saveExperienceFragment(Document doc, String outputFile) {

    File dirFile = new File(outputFile).parentFile
    FileUtils.forceMkdir(dirFile)

    //create experience_folder all the way to site folder
    while(dirFile && dirFile.parentFile) {

      //println '...frag: ' + dirFile.absolutePath+'/.content.xml'
      def filex = dirFile.absolutePath+'/.content.xml'
      if(! new File(filex).exists()) {
        def docx = getTgtTemplate('fragment_folder')
        // special for parent of master folder
        String masterFolder = new File(filex).parent+'/master'
        if(new File(masterFolder).exists()) {
          docx = getTgtTemplate('fragment_')
        }

        docx.selectFirst('jcr|root>jcr|content').attr('jcr:title', dirFile.name)
        if(dirFile.parentFile.name == 'experience-fragments') {
          def jcrNd = docx.selectFirst('jcr|root>jcr|content')
          jcrNd.attr('cq:allowedTemplates', '[/conf/dexter/settings/wcm/templates/experience-fragment-web-variation]')
        }

        def filexOS = new FileOutputStream(filex)
        IOUtils.write(commonUtil.xmlPretty(docx), filexOS, 'UTF-8')
        filexOS.close()
      }

      //next loop
      dirFile = dirFile.parentFile
      if(dirFile.name == 'experience-fragments') {
        break
      }
    }

    String prettyXml = commonUtil.xmlPretty(doc)
    //post map asset and link
    //docString = postMappingAssetlinks(docString, outputFile)

    def filexOS = new FileOutputStream(outputFile)
    IOUtils.write(prettyXml, filexOS, 'UTF-8')
    filexOS.close()
  }

  def unwrap(Element ele) {

    if(ele.selectFirst('>cq|responsive')) {
      if('position-par' == ele.parent().tagName()) {
        // moveup responsive before unwrap
        ele.parent().parent().appendChild(ele.selectFirst('>cq|responsive'))
      } else {
        ele.parent().appendChild(ele.selectFirst('>cq|responsive'))
      }
    }

    ele.remove()
  }

  // create default width and offset; no "tablet and phone" for Acom site;
  // return null when width =12 or 0
  Element createResponsive(HashMap<String, Integer> widthOffset) {
    int width = widthOffset.width
    if(width==12 || width==0) {
      return null
    }
    def responsive = getTgtComponent('cq:responsive')
    def size = responsive.selectFirst('>default')
    size.attr('width', ''+widthOffset.width)
    size.attr('offset', ''+widthOffset.offset)

    return responsive
  }
}

// skip renditions folder except original
// skip subassets
class AssetFilter implements FileFilter {

  public boolean accept(File pathname) {

    def absPath = pathname.absolutePath
    if(absPath.endsWith('/subassets')) {
      return false
    } else if(absPath.contains('_jcr_content/renditions')) {
      if(absPath.endsWith('/renditions') ||
          absPath.endsWith('/original') ||
          absPath.endsWith('/original.dir') ||
          absPath.endsWith('/original.dir/.content.xml') ) {
        return true
      } else {
        return false
      }
    }
    return true
  }
}

return new TargetUtil()