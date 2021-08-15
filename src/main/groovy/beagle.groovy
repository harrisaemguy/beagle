
/*
 beagle/components/fullwidthcontentpage
 plans/components/page
 beagle/components/onecolumnpage
 */
import java.text.SimpleDateFormat

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

import groovy.json.JsonSlurper


// ==================== start: resolve projectDir and shared library =========
srcUtil = null
adobeUtil = null
commonUtil = null
targetUtil = null
reportUtil = null
projectDir = null

try {

  //inside governor
  Class.forName('com.handshape.dimensions.executor.ui.ConsolePanel')
  def scriptDir = new File(lib.getResource('.')).absolutePath
  projectDir = scriptDir.substring(0, scriptDir.indexOf('/src'))
  commonUtil = lib.load('_CommonUtil.groovy')
  adobeUtil = lib.load('_AdobeUtil.groovy')
  targetUtil = lib.load('_TargetUtil.groovy')
  srcUtil = lib.load('_SrcUtil.groovy')
  reportUtil = lib.load('_ReportUtil.groovy')
} catch(any) {

  //any.printStackTrace()
  scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
  scriptDir = scriptDir.replace("\\","/")
  println "S C R I P T D I R  ---------*************"+scriptDir
  projectDir = scriptDir.substring(0, scriptDir.indexOf('/build'))
  commonUtil = Class.forName('CommonUtil').newInstance()
  adobeUtil = Class.forName('AdobeUtil').newInstance()
  targetUtil = Class.forName('TargetUtil').newInstance()
  srcUtil = Class.forName('SrcUtil').newInstance()
  reportUtil = Class.forName('ReportUtil').newInstance()
}
targetUtil.commonUtil = commonUtil
srcUtil.commonUtil = commonUtil
srcUtil.targetUtil = targetUtil
srcUtil.adobeUtil = adobeUtil
commonUtil.projectDir = projectDir
targetUtil.projectDir = projectDir
targetUtil._init()
println projectDir
projectDir = projectDir.replace("\\","/")
// ==================== end: resolve projectDir and shared library =========

// Specify srcPkg location
packageName = 'beagle_pages'
//packageName = 'Beagle_Country_Sites_Batch1'
//packageName = 'Beagle_Country_Sites_Batch2'
packageName = 'Beagle_en_pages'
//packageName = 'Beagle_Languages_Batch1'
//packageName = 'Beagle_Languages_Batch2'

packageVersion = '1.0'

jsonSlurper = new JsonSlurper()

// provide ci list to migrate from small set of pages
ciFile = new File(projectDir + '/test.txt')

if(packageName.indexOf('/') > 0) {
  // resolve generated packageName
  packageName = packageName.substring(packageName.lastIndexOf('/')+1)
}

// create all_references.csv to hold pages, assets, reference
allReferences = [:]


allPkgsDir = projectDir + '/srcPkgs'

// delete folder all_assets if you are not put all asset together
all_assets = allPkgsDir + '/all_assets'
srcSitename = 'acom'
dxTarget = 'cc1'
useCiFile = false
doDeploy = false
doAsset = true
doPage = true
runScan = true

srcPkgDir = projectDir + '/srcPkgs/' + packageName
////////////////////////////////////////////////////////////////////////
////                      global variables                          ////
////////////////////////////////////////////////////////////////////////

// report collection
todoResourceTypes     = new HashMap<String, List<String>>()
srcTargetPages        = new HashMap<String, String>()
largeTablePages       = []

// collect into filter.txt
targetPages           = new LinkedList<String>()
// original -> filter root, into filter.txt
assets                = new TreeMap<String, String>()
// key -> filter root, into filter.txt
fragmentpages         = new TreeMap<String, String>()

// regex mapping src to dest
srcPagesPtn = 'content/'+srcSitename+'/(.*?)'
destPagesPtn = 'content/'+dxTarget+'/$1'
srcFragPtn = 'content/'+srcSitename+'/(.*?)/fragments/(.*)$'
destFragPtn = 'content/experience-fragments/'+dxTarget+'/$1/$2/master'
srcAssetsPtn = 'content/dam/'+srcSitename+'/(.*?)'
destAssetsPtn = 'content/dam/'+dxTarget+'/$1'

if(! new File(srcPkgDir).exists()) {
  throw new Exception('please unzip source content into ' + srcPkgDir)
}

// clean destination folder before migration
FileUtils.deleteDirectory(new File(projectDir + '/' + dxTarget))

// start migration process
def start = System.currentTimeMillis()
srcpageMap = [:]
largeTable = []

if(useCiFile) {
  srcpageMap = commonUtil.readSrcCIList(ciFile)
} else {
  srcpageMap = commonUtil.findContentList(new File(srcPkgDir + '/jcr_root'), 'resourceType="beagle/components/fullwidthcontentpage"')
  fragmentpages = commonUtil.findContentList(new File(srcPkgDir + '/jcr_root'), 'resourceType="beagle/components/fragmentpage"')
}

if(!new File(projectDir+'/all_references.csv').exists() || runScan) {
  println '\n\n=== Collected page/asset/fragment as reference .....'

  allReferences = srcUtil.findAllReferenceList(new File(allPkgsDir), '[sling:resourceType=beagle/components/fullwidthcontentpage]')

  // create a txt file for reuse
  new File(projectDir+'/all_references.csv').withWriter { out ->
    allReferences.each { ref, type ->
      out.println ref + ',' + type

      // println ref + ',' + type
    }
  }

  Thread.sleep(1000)
} else {

  // run listRefers.groovy to generate all_references
  new File(projectDir + '/all_references.csv').withReader { reader ->
    while ((relPath = reader.readLine())!=null) {
      if(relPath.startsWith('#') || !relPath) {
        continue
      }

      def refType = relPath.split(',')
      allReferences.put(refType[0], refType[1])
    }
  }
}

navListNameUniqueCache = [:]
println 'Total line of srcList: ' + srcpageMap.size()
int xxx = 0
srcpageMap.each { filePath, pagePath  ->

  def file
  if(useCiFile) {
    file = new File(projectDir + '/srcPkgs/' + filePath)

    // use relative string
    filePath = filePath.substring(pagePath.indexOf('/jcr_root') + 9)
    pagePath = pagePath.substring(pagePath.indexOf('/jcr_root') + 9)
  } else {
    file = new File(srcPkgDir + '/jcr_root' + filePath)
  }

  if(file.exists()) {
    println "${xxx++}. process " + pagePath
    navListNameUniqueCache.clear()
    String xml = commonUtil.file2xml(file)
    Document doc = commonUtil.xml2dom(xml)

    def pContent = doc.selectFirst('jcr|root>jcr|content')
    if(pContent && pContent.attr('sling:resourceType') in [
          'beagle/components/fullwidthcontentpage'
        ]) {

      //gather DamAsset
      doc.select('[fileReference]').each {
        if(it.attr('fileReference')) {
          def srcAsset = it.attr('fileReference')
          srcAsset = srcUtil.getRef1(srcAsset)
          def destAsset = srcAsset.replaceAll(srcAssetsPtn, destAssetsPtn)
          assets.put(srcAsset, destAsset)
        }
      }
      doc.select('[sling:resourceType=beagle/components/text][text]').each { ele ->
        def body = Jsoup.parseBodyFragment(ele.attr('text')).body()
        body.select('[href^=/content/dam/]').each { href ->
          def damPath = href.attr('href')
          def destAsset = damPath.replaceAll(srcAssetsPtn, destAssetsPtn)
          assets.put(damPath, destAsset)
        }
      }
      doc.select('[sling:resourceType=beagle/components/image][link]').each { ele ->
        if(ele.attr('link').startsWith('/content/dam')) {
          def damPath = ele.attr('link')
          def destAsset = damPath.replaceAll(srcAssetsPtn, destAssetsPtn)
          assets.put(damPath, destAsset)
        }
      }

      //gatherFragment
      //    doc.select('[fragmentReference][enableFragmentReference]').each { ele ->
      //      def fragmentReference = ele.attr('fragmentReference')
      //      if(fragmentReference) {
      //        def fragmentPath = groovy.xml.XmlUtil.escapeXml(fragmentReference)
      //        fragmentPath = URLDecoder.decode(fragmentPath)
      //        def targetFragPath = fragmentPath.replaceAll(srcFragPtn, destFragPtn)
      //        fragmentpages.put(fragmentPath + '/.content.xml', targetFragPath)
      //      }
      //    }

      migrateDocument(doc, pagePath, file)

      def docDest = targetUtil.getTgtTemplate('hawk')
      def contentBody = doc.selectFirst('jcr|root>jcr|content>contentbody')
      if(contentBody) {
        // add padding to section flex
        contentBody.select('>[sling:resourceType=dexter/components/structure/flex]').each { sectFlex ->
          sectFlex.attr('mobilePaddingBottom', '{Long}24')
          sectFlex.attr('mobilePaddingTop', '{Long}24')
          sectFlex.attr('tabletInheritPadding','{Boolean}false')
          sectFlex.attr('tabletPaddingTop','{Long}48')
          sectFlex.attr('tabletPaddingBottom','{Long}48')
        }

        // add sp1 to prevent top negative margin
        def sp1 = targetUtil.getTgtComponent('spacer')
        sp1.attr('mobileHeight', ''+100)
        docDest.selectFirst('jcr|root>jcr|content>root>content').appendChild(sp1)

        docDest.selectFirst('jcr|root>jcr|content>root>content').appendChild(contentBody)
        contentBody.unwrap()
      }

      //add migrated modal if any
      doc.select('[sling:ResourceType=dexter/components/structure/modal]').each {
        docDest.selectFirst('jcr|root>jcr|content>modalContainer').appendChild(it)
      }

      // remove unhandled components
      def resourceTypeMap =  targetUtil.removeInvalidResourceType(docDest.selectFirst('jcr|content>root'), false)
      resourceTypeMap.each { resourceType, occurs ->

        def pathLst = todoResourceTypes.get(resourceType)?:[]
        if(!pathLst.contains(pagePath)) {
          pathLst.add(pagePath)
          todoResourceTypes.put(resourceType, pathLst)
        }
      }

      transferPageProperties(doc, docDest)
      addExtraNodeProperties(doc, docDest, filePath)
      //postPositionNavlist(docDest)

      // figure out dest file before output
      def destPath = filePath.replaceAll(srcPagesPtn, destPagesPtn)

      // add to filter.txt
      targetPages.add(destPath - '/.content.xml' + '/jcr:content')
      // report.xslx
      srcTargetPages.put(pagePath, destPath - '/.content.xml')

      String outputFile = projectDir + '/'+dxTarget+'/jcr_root' + destPath
      println '.........write out outputFile.................' + outputFile
      targetUtil.savePage(docDest, outputFile, srcAssetsPtn, destAssetsPtn, srcFragPtn, destFragPtn, srcPagesPtn, destPagesPtn)
    }
  }
}

println 'fragmentpage list: ' + fragmentpages.size()
xxx = 0
fragmentpages.each { pageFile, pagePath ->

  def file = new File(srcPkgDir + '/jcr_root/' + pageFile)
  println "... fragment page exists?  " + file.absolutePath
  if(file.exists()) {
    println "${xxx++}. process " + file.absolutePath

    def destPath = pagePath.replaceAll(srcFragPtn, destFragPtn) + '/.content.xml'
    String outputFile = projectDir + '/'+dxTarget+'/jcr_root/' + destPath
    println 'output file: ' + outputFile
    println 'pagePath : ' + pagePath

    navListNameUniqueCache.clear()
    String xml = commonUtil.file2xml(file)
    Document doc = commonUtil.xml2dom(xml)

    //gather DamAsset
    doc.select('[fileReference]').each {
      if(it.attr('fileReference')) {
        def srcAsset = it.attr('fileReference')
        srcAsset = srcUtil.getRef1(srcAsset)
        def destAsset = srcAsset.replaceAll(srcAssetsPtn, destAssetsPtn)
        assets.put(srcAsset, destAsset)
      }
    }
    doc.select('[sling:resourceType=beagle/components/text][text]').each { ele ->
      def body = Jsoup.parseBodyFragment(ele.attr('text')).body()
      body.select('[href^=/content/dam/]').each { href ->
        def damPath = href.attr('href')
        def destAsset = damPath.replaceAll(srcAssetsPtn, destAssetsPtn)
        assets.put(damPath, destAsset)
      }
    }
    doc.select('[sling:resourceType=beagle/components/image][link]').each { ele ->
      if(ele.attr('link').startsWith('/content/dam')) {
        def damPath = ele.attr('link')
        def destAsset = damPath.replaceAll(srcAssetsPtn, destAssetsPtn)
        assets.put(damPath, destAsset)
      }
    }

    migrateDocument(doc, pagePath, file)

    def docDest = targetUtil.getTgtTemplate('fragment_master')
    def body = doc.selectFirst('jcr|root>jcr|content')
    def content = docDest.selectFirst('jcr|content>root')
    if(body) {
      content.appendChild(body)
      body.unwrap()
    }
    //postPositionNavlist(docDest)
    targetUtil.saveExperienceFragment(docDest, outputFile)
  }
}

//assets migration, this path should match the dest of the asset map
if(doAsset) {
  assets.each { srcPath, destPath ->

    if(destPath) {
      println '. process asset: ' + destPath
      def srcDam = srcPkgDir + '/jcr_root' + srcPath
      if(new File(all_assets).exists()) {
        srcDam = all_assets + '/jcr_root' + srcPath
      }
      def destDam = projectDir + '/'+dxTarget+'/jcr_root' + destPath
      if(targetUtil.createAsset(srcDam, destDam) == false ) {
        // empty destPath
        assets.put(srcPath, '')
      }
    }
  }
}

new File(projectDir+'/filter.txt').withWriter { out ->
  targetPages.each { item ->
    out.println item
  }

  fragmentpages.each { key, item ->
    def destPath = item.replaceAll(srcFragPtn, destFragPtn) + '/jcr:content'
    out.println destPath

    //println key
    //println "item: "+item
  }

  if(doAsset) {
    assets.each { fileAbs, destPath ->
      if(destPath) {
        out.println destPath
      }
    }
  }
}

def reportLines = []
todoResourceTypes.each { key, val ->
  val.each { valItem ->
    reportLines.add("${key}|${valItem}")
  }
}
reportUtil.printReport("${packageName}_report.xlsx", 'todos', 'component|page', reportLines)

reportLines = []
assets.each { key, val ->
  if(val) {
    def fileP = key.substring(key.indexOf('srcPkgs')+8)
    reportLines.add("${fileP}|${val}")
  }
}
reportUtil.printReport("${packageName}_report.xlsx", 'local assets', 'asset|move to', reportLines)

reportLines = []
srcTargetPages.each { key, val ->
  reportLines.add("${key}.html|${val}.html")
}
reportUtil.printReport("${packageName}_report.xlsx", 'page map', 'src page | target page', reportLines)

// use gradle to create package
ProcessBuilder pb = null
if(System.properties['os.name'].toLowerCase().contains('windows')) {
  pb = new ProcessBuilder('gradle.bat', 'clean', 'myZip', "-PpackageName=${packageName}", "-PpackageVersion=${packageVersion}")
} else {
  pb = new ProcessBuilder('gradle', 'clean', 'myZip', "-PpackageName=${packageName}", "-PpackageVersion=${packageVersion}")
}

pb.redirectErrorStream(true)
pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)


//pb.directory(new File(projectDir.replace("\\","/")))
pb.directory(new File('.'))
def proc = pb.start()
proc.waitFor()

if(proc.exitValue() != 0) {
  def error = IOUtils.toString(proc.getErrorStream(), 'UTF-8')
  println error
  throw new Exception('gradle build failed')
}


if(doDeploy) {
  // use gradle to create package and deploy it
  pb = new ProcessBuilder('gradle', 'deploy')
  pb.redirectErrorStream(true)
  pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)

  pb.directory(new File(projectDir))
  proc = pb.start()
  proc.waitFor()

  if(proc.exitValue() != 0) {
    def error = IOUtils.toString(proc.getErrorStream(), 'UTF-8')
    println error
    throw new Exception('gradle build failed')
  }
}

def end = System.currentTimeMillis()
println 'AEM migration completed in: ' + (end - start) + ' ms.'



def migrateDocument(Document doc, pagePath, pageFile) {

  commonUtil.cleanupPageDom(doc)
  srcUtil.cleanupSrcDom(doc, pagePath, pageFile)

  do_faqmodal(doc)
  doStandalonemodal(doc)
  doSocialsharemobilepage(doc)
  doStandalonecta(doc)

  doFragment(doc)
  do_horizontalrule(doc)
  doTitle(doc)
  doHeroArticle(doc)
  do_anchor(doc)
  doText(doc)
  do_pullquote(doc)

  do_inlinevideo(doc)
  do_heroFeaturedvideo(doc)
  doImage(doc)
  do_textandimage(doc)
  doTextImageRef(doc)
  do_features(doc)
  do_featurewidget(doc)
  doCalendar(doc)
  doHero(doc)
  recursiveDoColumn(doc)
  // run after doText and doImage
  do_textandimage2col(doc)
  postChartTable(doc)
  doTextTable(doc)
  doTabPanel(doc)
  do_reimtabs(doc)
  doMerchandisingbar(doc)
  do_faasform(doc)

  commonUtil.duplicateNodeDetect(doc)
}

// ctabuttoncolor, ctaType, link, linkText, target, ctabuttonalign
def doStandalonecta(Document doc) {
  // text-center, text-end, text-start, center
  // _blank, _self, _parent
  def styleMap = [
    'merchpod-cta-button':'spectrum-Button spectrum-Button--cta',
    'ui-blue-button':'spectrum-Button doccloud-Button--blue',
    'ui-yellow-button':'spectrum-Button doccloud-Button--yellow',
    'ui-legacy-yellow-button round':'spectrum-Button doccloud-Button--yellow',
    'hollow-white' : 'spectrum-Button doccloud-Button--white',
    'hollow-white hollow-grey':'spectrum-Button doccloud-Button--blue']
  def alignMap = [
    'text-center':'align-center',
    'text-start':'align-left',
    'center':'align-center',
    'text-end':'align-right']
  doc.select('[sling:resourceType=beagle/components/standalonecta]').each { ele ->
    def tagName = ele.tagName()
    def tpl = targetUtil.getTgtComponent('cta')
    ele.before(tpl)
    tpl.tagName(tagName)
    tpl.appendChild(ele)
    tpl.attr('linkText', ele.attr('linkText'))
    tpl.attr('linkURL', ele.attr('link'))

    tpl.attr('ctaType', 'basic')

    def align = ele.attr('ctabuttonalign')
    if(align) {
      align = alignMap.get(align)
      tpl.attr('align', align)
    }

    def style = ele.attr('style')
    style = styleMap.get(style)
    //if(ele.attr('fullWidth')=='true') {
    //tpl.attr('style', style + ' full-width')
    //} else {
    tpl.attr('style', 'spectrum-Button spectrum-Button--cta')
    //}

    ele.unwrap()
  }
}

// a cta link to popup modal, which holds question and answers
def do_faqmodal(Document doc) {
  doc.select('[sling:resourceType=beagle/components/faqmodal][faqText]').each { faqm ->

    // create a modal
    String modalId = srcUtil.generateModalId(commonUtil.getNodePath(faqm))
    def modalTagname = 'faq' + modalId
    def modalTpl = createModal(doc, modalId, '480', '360')
    modalTpl.tagName(modalTagname)

    // use modal to hold accordion
    def qaAccordion = targetUtil.getTgtComponent('accordion')
    qaAccordion.tagName(faqm.tagName() + '_accordion')
    modalTpl.selectFirst('>responsive-grid').appendChild(qaAccordion)

    // create accordion contents
    def item0 = qaAccordion.selectFirst('>items>item0')
    faqm.select('>faqs>*[answerText][questionText]').eachWithIndex { qa, idx ->
      if(idx > 0) {
        item0 = item0.clone()
        item0.tagName('item'+idx)
        qaAccordion.selectFirst('>items').appendChild(item0)
      }

      item0.attr('title', qa.attr('questionText'))
      item0.attr('id', 'x'+(idx+1))
      def contentx = targetUtil.getTgtComponent('responsivegrid')
      contentx.tagName('content-x' + (idx+1))
      contentx.attr('jcr:primaryType', 'nt:unstructured')
      contentx.attr('sling:resourceType', 'wcm/foundation/components/responsivegrid')
      qaAccordion.appendChild(contentx)
      def answer = targetUtil.getTgtComponent('text')
      answer.attr('text', qa.attr('answerText'))
      contentx.appendChild(answer)
    }

    // create cta to point to modal
    def linkText = faqm.attr('faqText')
    def faqCta = targetUtil.getTgtComponent('cta')
    faqm.before(faqCta)
    faqCta.attr('linkURL', "#" + modalId)
    faqCta.attr('linkText', linkText)

    faqm.remove()
  }
}

// freeFormUrl, youTubeUrl, adobeTvNewUrl,
def doStandalonemodal(Document doc) {

  def linkTypes = ['free':'freeFormUrl', 'adobetvnew':'adobeTvNewUrl', 'youtube':'youTubeUrl']
  def linkUrlPrefixs = ['free':'', 'adobetvnew':'video.tv.adobe.com/v/', 'youtube':'youtube.com/embed/']

  doc.select('[sling:resourceType=beagle/components/standalonemodal][type]').each { modal->

    def linkType = modal.attr('type')
    if(linkUrlPrefixs[linkType] == null) {
      throw Exception('new link type: ' + linkType)
    }
    def videoUrl = linkUrlPrefixs[linkType] + modal.attr( linkTypes[linkType] )

    String modalId = modal.attr('modalId')

    // insert modal
    if(modalId) {
      def videoTpl = createFreeformVideo(videoUrl)

      def size = modal.attr('size')?.split('x') ?: ['480', '360']
      def modalTpl = createModal(doc, modalId, size[0], size[1])
      modalTpl.tagName(linkType)
      modalTpl.selectFirst('>responsive-grid').appendChild(videoTpl)
    }
    modal.remove()
  }
}

// freeFormUrl="//video.tv.adobe.com/v/26305t1/?quality=6&amp;autoplay=true&amp;hidetitle=true"
def createFreeformVideo(freeFormUrl) {
  def video = targetUtil.getTgtComponent('video')
  video.tagName('linkVideo')
  def vId = ''

  if(freeFormUrl.contains('video.tv.adobe.com')) {
    vId = freeFormUrl.substring(freeFormUrl.indexOf('video.tv.adobe.com/v/') + 'video.tv.adobe.com/v/'.length())
    //video.tv.adobe.com
    if(vId.contains('/')) {
      vId = vId.substring(0, vId.indexOf('/'))
    }

    if(vId.contains('?')) {
      vId = vId.substring(0, vId.indexOf('?'))
    }

    video.selectFirst('>adobeTv').attr('id', vId)
    video.attr('videoService', 'adobeTv')
  } else if(freeFormUrl.contains('tv.adobe.com')) {
    vId = freeFormUrl.substring(freeFormUrl.indexOf('tv.adobe.com/embed/') + 'tv.adobe.com/embed/'.length())
    // AdobeTV - Old
    if(vId.contains('/')) {
      vId = vId.substring(0, vId.indexOf('/'))
    }

    if(vId.contains('?')) {
      vId = vId.substring(0, vId.indexOf('?'))
    }

    video.selectFirst('>adobeTv').attr('id', vId)
    video.attr('videoService', 'adobeTvOld')
  } else if(freeFormUrl.contains('youtube.com')) {
    vId = freeFormUrl.substring(freeFormUrl.indexOf('youtube.com/embed/') + 'youtube.com/embed/'.length())
    // youtube.com
    if(vId.contains('/')) {
      vId = vId.substring(0, vId.indexOf('/'))
    }

    if(vId.contains('?')) {
      vId = vId.substring(0, vId.indexOf('?'))
    }

    video.selectFirst('>youTube').attr('id', vId)
    video.attr('videoService', 'youTube')
  } else if(freeFormUrl.contains('vimeo.com')) {
    vId = freeFormUrl.substring(freeFormUrl.indexOf('vimeo.com/video/') + 'vimeo.com/video/'.length())
    // Vimeo
    if(vId.contains('/')) {
      vId = vId.substring(0, vId.indexOf('/'))
    }

    if(vId.contains('?')) {
      vId = vId.substring(0, vId.indexOf('?'))
    }

    video.selectFirst('>youTube').attr('id', vId)
    video.attr('videoService', 'ambientVideo')
  } else {
    video.selectFirst('>custom').attr('embed', freeFormUrl)
    video.attr('videoService', 'custom')
  }

  return video
}
// freeFormUrl, adobeTvNewUrl, youTubeUrl, adobeTvUrl

// dexter model only support certain size
def createModal(Document doc, String modalId, String width, String height) {

  def modalTpl = targetUtil.getTgtComponent('modal')
  modalTpl.attr('id', modalId)
  def mobile = modalTpl.selectFirst('>mobile')
  mobile.attr('width', 'custom').attr('widthOverrideValue', width)
  mobile.attr('height', 'custom').attr('heightOverrideValue', height)

  def tablet = modalTpl.selectFirst('>tablet')
  tablet.attr('width', 'custom').attr('widthOverrideValue', width)
  tablet.attr('height', 'custom').attr('heightOverrideValue', height)

  def desktop = modalTpl.selectFirst('>desktop')
  desktop.attr('width', 'custom').attr('widthOverrideValue', width)
  desktop.attr('height', 'custom').attr('heightOverrideValue', height)

  if(doc.selectFirst('modalcontent') == null) {
    doc.appendChild(targetUtil.getTgtComponent('modalcontent'))
  }
  doc.selectFirst('modalcontent').appendChild(modalTpl)

  return modalTpl
}

// videoIconCta when modal
def doSocialsharemobilepage(Document doc) {

  def linkTypes = ['free':'freeFormUrl', 'adobetvnew':'adobeTvNewUrl', 'youtube':'youTubeUrl']
  def linkUrlPrefixs = ['free':'', 'adobetvnew':'video.tv.adobe.com/v/', 'youtube':'youtube.com/embed/']

  doc.select('[sling:resourceType=beagle/components/modalcontent/socialsharemobilepage]').each { link->

    def linkType = link.attr('type')
    if(linkUrlPrefixs[linkType] == null) {
      throw Exception('new link type: ' + linkType)
    }
    def videoUrl = linkUrlPrefixs[linkType] + link.attr( linkTypes[linkType] )

    // get modalId or generate one
    String modalId = link.attr('modalId')?.replace('-', '_') ?: srcUtil.generateModalId(commonUtil.getNodePath(link))

    // insert modal
    def modalTagname = linkType + modalId
    if(doc.selectFirst(modalTagname) == null) {
      // does not exist
      def video = createFreeformVideo(videoUrl)

      def size = link.attr('size')?.split('x') ?: ['480', '360']
      def modalTpl = createModal(doc, modalId, size[0], size[1])

      modalTpl.selectFirst('>responsive-grid').appendChild(video)
    }

    // create videoIconCta
    def videoIconCta = targetUtil.getTgtComponent('videoIconCta')
    link.before(videoIconCta)
    videoIconCta.attr('linkURL', "#" + modalId)
    link.remove()
  }
}



// into position and modal-video
def do_inlinevideo(Document doc) {
  def linkTypes = ['free':'freeFormUrl', 'adobetvnew':'adobeTvNewUrl', 'youtube':'youTubeUrl']
  def linkUrlPrefixs = ['free':'', 'adobetvnew':'video.tv.adobe.com/v/', 'youtube':'youtube.com/embed/']

  doc.select('[sling:resourceType=beagle/components/inlinevideo]').each { ele->
    def txt = ele.selectFirst('>contentlayer>text[text]')
    def video = ele.selectFirst('>contentlayer>video[type]')

    if(txt || video) {
      def inlinePos = targetUtil.getTgtComponent('position')
      ele.before(inlinePos)
      inlinePos.tagName(ele.tagName())

      if(ele.hasAttr('fileReference')) {
        def fileReference = ele.attr('fileReference')
        fileReference = srcUtil.getRef1(fileReference)
        if(allReferences.get(fileReference) != null) {
          fileReference = fileReference.replaceAll(srcAssetsPtn, destAssetsPtn)
        }
        inlinePos.attr('fileReference', fileReference)
        inlinePos.attr('fileReferenceMobile', fileReference)
        inlinePos.attr('fileReferenceTablet', fileReference)
        inlinePos.attr('imageRenditionDesktop', fileReference)
        inlinePos.attr('alt', ele.attr('alt'))
      }

      if(txt) {
        def inlinetxt = targetUtil.getTgtComponent('text')
        inlinetxt.tagName('inlineText')
        inlinetxt.attr('text', txt.attr('text'))
        inlinePos.selectFirst('>position-par').appendChild(inlinetxt)
      }

      if(video) {
        def linkType = video.attr('type')
        if(linkUrlPrefixs[linkType] == null) {
          throw Exception('new link type: ' + linkType)
        }
        def videoUrl = linkUrlPrefixs[linkType] + video.attr( linkTypes[linkType] )

        // get modalId or generate one
        String modalId = srcUtil.generateModalId(commonUtil.getNodePath(video))

        // insert modal
        def modalTagname = linkType + modalId
        if(doc.selectFirst(modalTagname) == null) {
          // does not exist
          def videox = createFreeformVideo(videoUrl)
          def size = video.attr('size')?.split('x') ?: ['480', '360']
          def modalTpl = createModal(doc, modalId, size[0], size[1])
          modalTpl.tagName(modalTagname)
          modalTpl.selectFirst('>responsive-grid').appendChild(videox)
        }

        // create videoIconCta
        def videoIconCta = targetUtil.getTgtComponent('videoIconCta')
        inlinePos.selectFirst('>position-par').appendChild(videoIconCta)
        videoIconCta.attr('linkURL', "#" + modalId)
      }
    }

    ele.remove()
  }
}

// modalType(link,video)
// size="854x480"
// type="free" (youtube)

//youTubeUrl="GmhFFOdWFS8"
// target service: adobeTv, youTube, vimeo, adobeTvOld, ambientVideo, custom
def doVideo(Document doc, String srcPage) {

  // 1. adobeTvOld: https://tv.adobe.com/embed/, id="vid"
  // 2. adobeTv: https://video.tv.adobe.com/v/, id="vid"
  // 3. youTube: https://www.youtube.com/embed/, id="vid"
  // 4. vimeo: https://player.vimeo.com/video/,  id="vid"
  // 5. custom: embed="https://www.youtube.com/embed/Z_Hi0YwYhJE"
  // 6. ambientVideo: videoURL=/content/dam/...
  doc.select('[sling:resourceType$=/video]:not([videoSrcUrl])').each { ele->
    // ele.remove()
  }

  doc.select('[sling:resourceType$=/video][videoSrcUrl]').each { ele->
    def video = targetUtil.getTgtComponent('video')
    video.tagName(ele.tagName())

    def videoSrcUrl = ele.attr('videoSrcUrl')
    def vId = ''
    if(videoSrcUrl.contains('video.tv.adobe.com')) {
      vId = videoSrcUrl.substring(videoSrcUrl.indexOf('video.tv.adobe.com/v/') + 'video.tv.adobe.com/v/'.length())
      //video.tv.adobe.com
      if(vId.endsWith('?')) {
        vId = vId.substring(0, vId.length()-1)
      }
      video.selectFirst('>adobeTv').attr('id', vId)
      video.attr('videoService', 'adobeTv')
    } else if(videoSrcUrl.contains('tv.adobe.com')) {
      vId = videoSrcUrl.substring(videoSrcUrl.indexOf('tv.adobe.com/embed/') + 'tv.adobe.com/embed/'.length())
      // AdobeTV - Old
      if(vId.endsWith('?')) {
        vId = vId.substring(0, vId.length()-1)
      }
      video.selectFirst('>adobeTv').attr('id', vId)
      video.attr('videoService', 'adobeTvOld')
    } else if(videoSrcUrl.contains('youtube.com')) {
      vId = videoSrcUrl.substring(videoSrcUrl.indexOf('youtube.com/embed/') + 'youtube.com/embed/'.length())
      // youtube.com
      if(vId.endsWith('?')) {
        vId = vId.substring(0, vId.length()-1)
      }
      video.selectFirst('>youTube').attr('id', vId)
      video.attr('videoService', 'youTube')
    } else if(videoSrcUrl.contains('vimeo.com')) {
      vId = videoSrcUrl.substring(videoSrcUrl.indexOf('vimeo.com/video/') + 'vimeo.com/video/'.length())
      // Vimeo
      if(vId.endsWith('?')) {
        vId = vId.substring(0, vId.length()-1)
      }
      video.selectFirst('>youTube').attr('id', vId)
      video.attr('videoService', 'ambientVideo')
    } else {
      video.selectFirst('>custom').attr('embed', ele.attr('videoSrcUrl'))
      video.attr('videoService', 'custom')
    }

    ele.before(video)
    video.appendChild(ele)

    ele.select('[sling:resourceType=help/components/expert-image]').each {
      it.remove()
    }
    targetUtil.unwrap(ele)
  }
}

def do_horizontalrule(Document doc) {
  doc.select('[sling:resourceType=beagle/components/horizontalrule]').each { ele ->

    def hRule = targetUtil.getTgtComponent('horizontalrule')
    hRule.tagName(ele.tagName())
    ele.before(hRule)

    targetUtil.unwrap(ele)
  }
}

def doFragment(Document doc) {
  doc.select('[fragmentReference][enableFragmentReference]').each { ele ->
    def xfreference = targetUtil.getTgtComponent('xfreference')
    xfreference.tagName(ele.tagName())
    ele.before(xfreference)
    def nPath = ele.attr('fragmentReference')
    if(allReferences.get(nPath) != null) {
      nPath = nPath.replaceAll(srcFragPtn, destFragPtn)
    }
    xfreference.attr('fragmentPath', nPath)
    ele.remove()
  }
}

def doTitle(Document doc) {
  doc.select('[sling:resourceType=beagle/components/title]').each { ele ->

    def title = targetUtil.getTgtComponent('title')
    title.tagName(ele.tagName())
    ele.before(title)

    title.attr('jcr:title', ele.attr('title'))
    if(ele.hasAttr('headingStyle')) {
      title.attr('type', ele.attr('headingStyle'))
    }

    targetUtil.unwrap(ele)
  }
}

// strong => b
// em => i
// img src=  (/content=>/content/dam, _jcr_content => jcr:content, remove /file.res)
def postDoText(txt) {
  def trimTxt = txt.attr('text')
  if(trimTxt.endsWith('___')) {
    // remove ending linebreak
    trimTxt = trimTxt.substring(0, trimTxt.length()-3)
  }
  def body = Jsoup.parseBodyFragment(trimTxt).body()

  // update class: class="spectrum-Button spectrum-Button--cta"
  body.select('span.kbd>a,span.button>a,a>span.kbd,a>span.button').each {

    if(it.tagName() == 'a') {
      // link
      it.parent().addClass('spectrum-Button').addClass('spectrum-Button--cta')
      it.unwrap()
    } else {
      // span
      it.parent().addClass('spectrum-Button').addClass('spectrum-Button--cta')
      it.unwrap()
    }

  }

  body.select('>div[style]').each {
    // first level div is not supported
    it.remove()
  }
  body.select('>div').each {
    it.unwrap()
  }

  body.select('object').each {
    it.remove()
  }

  def bodyTxt = body.html()
  txt.attr('text', bodyTxt)
}

//author,fontstyle,quote,title
def do_pullquote(Document doc) {
  doc.select('[sling:resourceType=beagle/components/pullquote]').each { ele ->
    def quote = ele.attr('quote')
    def author = ele.attr('author')
    def title = ele.attr('title')
    def fontstyle = ele.attr('fontstyle')
    def multiline = "<p style=\"font-style: ${fontstyle}\">"
    multiline += '"' + quote + '"</p>'
    multiline += '<p><cite><b>— '+author+'</b>, ' + title + '</cite></p>'
    def text = targetUtil.getTgtComponent('text')
    text.tagName(ele.tagName())
    text.attr('text', multiline)
    text.attr('cq:styleIds', '[' + targetUtil.textStyle['text-center'] + ']')
    ele.before(text)
    ele.remove()
  }
}

//h1Title|h2Title|h1Img, text; alignment on both flex and text
def doHeroArticle(Document doc) {
  def alignMap = ['hero2-align-0':'text-right', 'hero2-align-1':'text-left', 'hero2-align-2':'text-center', 'hero2-align-3':'text-right']
  doc.select('[sling:resourceType=beagle/components/hero/article]').each { ele ->
    // pageProperties.useH1Tags == 'heroTitle'? 'h1' : 'h2'
    def useH1Tags = doc.selectFirst('jcr|root>jcr|content').attr('useH1Tags')
    def htag = useH1Tags == 'heroTitle'? 'h1' : 'h2'
    def useImageTitle = 'true' == ele.attr('useImageTitle') && ele.selectFirst('>titleImage[fileReference]') != null
    def h1Header = ele.attr('h1Header')?.replace('<p>','')?.replace('</p>','')
    def title = ele.attr('title')?.replace('<p>','')?.replace('</p>','')

    def styles = []
    if(ele.hasAttr('articleAlignment')) {
      styles.add(targetUtil.textStyle[ alignMap.get(ele.attr('articleAlignment')) ])
    }

    // migrating
    if(useImageTitle) {
      def titleImage = ele.selectFirst('>titleImage')
      def fileReference = titleImage.attr('fileReference')
      def alt = titleImage.attr('alt')

      fileReference = srcUtil.getRef1(fileReference)
      // update reference location
      if(allReferences.get(fileReference) != null) {
        fileReference = fileReference.replaceAll(srcAssetsPtn, destAssetsPtn)
      }

      def hImg = targetUtil.getTgtComponent('text')
      hImg.tagName('hImg')
      ele.before(hImg)
      def txt = "<${htag}<img src=\"${fileReference}\" alt=\"${alt}\"/></${htag}>"
      println 'h img txt: ' + txt
      hImg.attr('text', txt)
      hImg.attr('cq:styleIds', styles.toString())
    }

    if(h1Header) {
      def h1 = targetUtil.getTgtComponent('text')
      h1.tagName('h1Header')
      ele.before(h1)
      def txt = "<h1>${h1Header}</h1>"
      h1.attr('text', txt)
      h1.attr('cq:styleIds', styles.toString())
    }

    if(title) {
      def text = targetUtil.getTgtComponent('text')
      text.tagName('title')
      ele.before(text)
      def txt = "<${htag}>${title}</${htag}>"
      text.attr('text', txt)
      text.attr('cq:styleIds', styles.toString())
    }

    if(ele.hasAttr('text')) {
      def text = targetUtil.getTgtComponent('text')
      text.tagName('text')
      ele.before(text)
      text.attr('text', ele.attr('text'))
      text.attr('cq:styleIds', styles.toString())
    }

    ele.select('>links>*').each { linkx ->
      def target = linkx.attr('target')
      if('modalVideo' == target) {

      } else if('modalRef' == target) {

      } else {
        def cta = targetUtil.getTgtComponent('cta')
        ele.before(cta)
        cta.attr('linkURL', linkx.attr('link'))
        cta.attr('linkText', linkx.attr('text'))
        cta.attr('linkTarget', target)

        // extra
        cta.attr('align', 'align-center')
        cta.attr('style', 'spectrum-Button spectrum-Button--cta')
      }

    }

    ele.remove()
  }
}

// into position and modal-video
def do_heroFeaturedvideo(Document doc) {
  def linkTypes = ['free':'freeFormUrl', 'adobetvnew':'adobeTvNewUrl', 'youtube':'youTubeUrl']
  def linkUrlPrefixs = ['free':'', 'adobetvnew':'video.tv.adobe.com/v/', 'youtube':'youtube.com/embed/']

  doc.select('[sling:resourceType=beagle/components/hero/featuredvideo]').each { ele->

    def titleText = ele.attr('titleText')
    def detail = ele.attr('detail')

    def styles = []
    if(ele.hasAttr('articleAlignment')) {
      styles.add(targetUtil.textStyle[ alignMap.get(ele.attr('articleAlignment')) ])
    }

    if(titleText) {
      def featuredH2 = targetUtil.getTgtComponent('text')
      featuredH2.tagName('h2')
      ele.before(featuredH2)
      def txt = "<h2>${titleText}</h2>"
      featuredH2.attr('text', txt)
      featuredH2.attr('cq:styleIds', styles.toString())
    }

    if(detail) {
      def detailP = targetUtil.getTgtComponent('text')
      detailP.tagName('detail')
      ele.before(detailP)
      def txt = "<h2>${detail}</h2>"
      detailP.attr('text', txt)
      detailP.attr('cq:styleIds', styles.toString())
    }

    if(ele.hasAttr('fileReference')) {
      def img = targetUtil.getTgtComponent('image')
      img.tagName('img')
      ele.before(img)

      def fileReference = srcUtil.getRef1(ele.attr('fileReference'))
      // update reference location
      if(allReferences.get(fileReference) != null) {
        fileReference = fileReference.replaceAll(srcAssetsPtn, destAssetsPtn)
      }

      img.attr('fileReference', fileReference)
      img.attr('fileReferenceMobile', fileReference)
      img.attr('fileReferenceTablet', fileReference)
      img.attr('imageRenditionDesktop', fileReference)
      img.attr('imageRenditionMobile', fileReference)
      if(ele.hasAttr('alt')) {
        img.attr('alt', ele.attr('alt'))
      }
    }

    if(ele.hasAttr('videomodalid')) {
      // create videoIconCta
      def videoIconCta = targetUtil.getTgtComponent('videoIconCta')
      ele.before(videoIconCta)
      videoIconCta.attr('linkURL', "#" + ele.attr('videomodalid'))
    }

    ele.remove()
  }
}

def do_anchor(Document doc) {
  doc.select('[sling:resourceType=beagle/components/anchor][anchorName]').each { ele ->
    def anchorName = ele.attr('anchorName')
    def text = targetUtil.getTgtComponent('text')
    text.tagName('archor')
    text.attr('text', "<p>&nbsp;<a id=\"${anchorName}\"></a> &nbsp;</p>")
    ele.before(text)
    ele.remove()
  }
}

def doText(Document doc) {
  doc.select('[sling:resourceType=beagle/components/text]').each { ele ->

    def body = Jsoup.parseBodyFragment(ele.attr('text')).body()
    // remove empty nodes, except &nbsp;
    def endLoop = false
    while(!endLoop) {
      def emptyNd = body.selectFirst('>*:matches(^$)')
      if(emptyNd && emptyNd.is(':first-child')) {
        emptyNd.remove()
      } else {
        endLoop = true;
      }
    }

    // Beagle_Country_Sites_Batch2/jcr_root/content/acom/ro/ro/products/technicalcommunicationsuite/download-trial/get-started
    body.select('.text-light:not(:matchesOwn(^$))').each {
      if(it.parent().tagName() == 'strong') {
        it.parent().unwrap()
      }
    }


    // remove invisible chunk
    body.select('[style*=absolute;]').each {
      it.remove()
    }

    // request by Ashish: .text-6-block:not(:matchesOwn(^$))>span.text-light
    // add <b>
    body.select('.text-6-block:not(:matchesOwn(^$))>span.text-light:only-child').each { span ->
      def block = span.parent()
      block.childNodes().each { nd ->
        if(nd instanceof TextNode) {
          nd.wrap('<b></b>');
        }
      }
    }

    body.select('[href^=/content/dam/]').each { href ->
      def damPath = href.attr('href')
      if(allReferences.get(damPath) != null) {
        damPath = damPath.replaceAll(srcAssetsPtn, destAssetsPtn)
        href.attr('href', damPath)
      }
    }

    // remove empty <a></a>, although not necessary
    body.select('a:matchesOwn(^$):not(:has(*))').each { it.remove() }

    // editable text can't have div, parent level div -> p
    body.select('>div').each { subSect -> subSect.tagName('p') }

    def blocks = []
    def currentElements = new Elements()
    def currentStyle  = ''
    body.select('>*').eachWithIndex { subSect, idx ->
      // combine elements with same css and style
      def css_style = subSect.className() + subSect.attr('style')

      if(idx==0 || currentStyle != css_style) {
        currentElements = new Elements()
        currentStyle = css_style
        blocks.add(currentElements)
      }

      currentElements.add(subSect)
    }

    blocks.each { elements ->
      // text-0-block, text-7-block => size-Body1, size-Body3
      def css = elements.first().className()
      def style = elements.first().attr('style')
      def firstTagName = elements.first().tagName()
      def buf = new StringBuilder()
      elements.each { element ->
        element.removeAttr('class')
        element.removeAttr('style')
        buf.append(element.toString())
      }

      def text = targetUtil.getTgtComponent('text')
      text.tagName('txt_b')
      text.attr('text', buf.toString())
      ele.before(text)

      def styles = []
      if(style.contains('text-align: left')) {
        styles.add(targetUtil.textStyle['text-left'])
      } else if(style.contains('text-align: center')) {
        styles.add(targetUtil.textStyle['text-center'])
      } else if(style.contains('text-align: right')) {
        styles.add(targetUtil.textStyle['text-right'])
      }

      //      text-0-block --> heading-XXL
      //      text-1-block --> heading-XXL
      //      text-2-block --> heading-XL
      //      text-3-block --> heading-L / body-XXL
      //      text-4-block --> heading-M
      //      text-5-block --> heading-S / body-XL
      //      text-6-block --> heading-XXS / body-S
      //      text-7-block --> paragraph

      if(css.contains('text-0-block') || css.contains('text-1-block')) {
        styles.add(targetUtil.textStyle['heading-XXL'])
      } else if(css.contains('text-2-block')) {
        styles.add(targetUtil.textStyle['heading-XL'])
      } else if(css.contains('text-3-block')) {
        styles.add(targetUtil.textStyle['heading-L'])
      } else if(css.contains('text-4-block')) {
        styles.add(targetUtil.textStyle['heading-M'])
      } else if(css.contains('text-5-block')) {
        styles.add(targetUtil.textStyle['heading-S'])
      } else if(css.contains('text-6-block')) {
        styles.add(targetUtil.textStyle['body-S'])
      }
      def styleIts = styles.iterator()
      styleIts.each { styleIt ->
        if(styleIt == null) {
          styleIts.remove()
        }
      }

      text.attr('cq:styleIds', styles.toString())

      postDoText(text)

    }

    ele.remove()
  }
}

// enableFragmentReference, fragmentReference
def doTextImageRef(Document doc) {
  doc.select('[sling:resourceType=beagle/components/textandimage][enableFragmentReference=true]').each { ele ->
    def tagName = ele.tagName()
    def xfreference = targetUtil.getTgtComponent('xfreference')
    xfreference.tagName(tagName)
    xfreference.attr('fragmentPath', ele.attr('fragmentReference'))
    ele.before(xfreference)
    xfreference.appendChild(ele)
    ele.remove()
  }
}

// create position to hold image and text
def do_textandimage(Document doc) {
  doc.select('[sling:resourceType=beagle/components/textandimage]:not([enableFragmentReference],[sling:resourceType=beagle/components/textandimage][enableFragmentReference=false])').each { ele ->

    // text and image are migrated if exists
    ele.select('>contentlayer').each { it.remove() }

    def txts = ele.select('[sling:resourceType$=/text]')
    def img = ele.select('[sling:resourceType$=/image]')

    if(txts.size()>1) {
      def pos = targetUtil.getTgtComponent('position')
      ele.prependChild(pos)
      txts.each { txt ->
        pos.selectFirst('>position-par').appendChild(txt)
      }
    }

    if(txts && img) {
      // create flex to hold text and image
      def flex = targetUtil.getTgtComponent('flex')
      ele.before(flex)
      flex.tagName(ele.tagName())
      flex.selectFirst('>items').appendChild(ele)
    }

    ele.unwrap()
  }
}

def doImage(Document doc) {

  doc.select('[sling:resourceType=beagle/components/image],image[fileReference]').each { ele ->

    def image = targetUtil.getTgtComponent('image')
    image.tagName(ele.tagName())
    def fileReference = ele.attr('fileReference')
    fileReference = srcUtil.getRef1(fileReference)

    // update reference location
    if(allReferences.get(fileReference) != null) {
      fileReference = fileReference.replaceAll(srcAssetsPtn, destAssetsPtn)
    }

    //fileReference = fileReference.replace('_jcr_content', 'jcr_content')
    //fileReference = fileReference.replace('jcr:content', 'jcr_content')

    image.attr('fileReference', fileReference)
    image.attr('fileReferenceMobile', fileReference)
    image.attr('fileReferenceTablet', fileReference)
    image.attr('imageRenditionDesktop', fileReference)
    image.attr('imageRenditionMobile', fileReference)

    def tip = fileReference.substring(fileReference.lastIndexOf('/')+1)
    if(tip.contains('.')) {
      tip = tip.substring(0, tip.indexOf('.'))
    }
    image.attr('tooltip', tip)
    image.attr('jcr:title', tip)

    if(ele.hasAttr('jcr:title')) {
      image.attr('jcr:title', ele.attr('jcr:title'))
      image.attr('tooltip', ele.attr('jcr:title'))
    }
    if(ele.hasAttr('alt')) {
      image.attr('alt', ele.attr('alt'))
    }
    if(ele.hasAttr('jcr:description')) {
      image.attr('jcr:description', ele.attr('jcr:description'))
    }
    if(ele.hasAttr('alignment')) {
      image.attr('align', ele.attr('alignment'))
    }

    if(ele.hasAttr('modalsize')) {
      // 426x240
      def modalsize = ele.attr('modalsize')

    }

    def setWidth = false
    if(ele.hasAttr('width')) {
      setWidth = true
      image.attr('imageOverrideValueMobile', '{Long}'+ele.attr('width'))
      image.attr('imageOverrideValueDesktop', '{Long}'+ele.attr('width'))
      image.attr('imageOverrideValueTablet', '{Long}'+ele.attr('width'))

    }
    if(ele.hasAttr('htmlWidth')) {
      setWidth = true
      image.attr('imageOverrideValueMobile', '{Long}'+ele.attr('htmlWidth'))
      image.attr('imageOverrideValueDesktop', '{Long}'+ele.attr('htmlWidth'))
      image.attr('imageOverrideValueTablet', '{Long}'+ele.attr('htmlWidth'))
    }
    if(ele.hasAttr('originalWidth')) {
      setWidth = true
      image.attr('imageOverrideValueMobile', '{Long}'+ele.attr('originalWidth'))
      image.attr('imageOverrideValueDesktop', '{Long}'+ele.attr('originalWidth'))
      image.attr('imageOverrideValueTablet', '{Long}'+ele.attr('originalWidth'))
    }

    if (setWidth) {
      image.attr('imageOverrideTypeMobile', 'width')
      image.attr('showImageOverRideMobile', 'true')
      image.attr('imageOverrideTypeDesktop', 'width')
      image.attr('showImageOverRideDesktop', 'true')
      image.attr('imageOverrideTypeTablet', 'width')
      image.attr('showImageOverRideTablet', 'true')
    }

    if(ele.hasAttr('href')) {
      image.attr('linkURL', ele.attr('href'))
    }

    if(ele.hasAttr('link')) {
      def linkDam = ele.attr('link')
      // update reference location
      if(allReferences.get(linkDam) != null) {
        linkDam = linkDam.replaceAll(srcAssetsPtn, destAssetsPtn)
      }
      image.attr('linkURL', linkDam)
      image.attr('linkTarget', ele.attr('target'))
    }

    if(ele.hasAttr('draftOnly')) {
      image.attr('draftOnly', ele.attr('draftOnly'))
    }

    ele.before(image)
    image.appendChild(ele)
    copyExtraProps(ele, image)
    targetUtil.unwrap(ele)
  }
}

//horizontallayout(-left, -right), verticallayout(-top)
// run after doText and doImage in case text becomes multiple text nodes
def do_textandimage2col(Document doc) {
  doc.select('[sling:resourceType=beagle/components/textandimage2col]').each { ele ->
    // image on left or right, keep node in original order, migrate to flex
    def par = ele.selectFirst('>par')
    def image = ele.selectFirst('>image')
    def title = ele.attr('title')
    def altText = ele.attr('altText')

    if(par) {
      if(par.select('>*').size() >1 || title) {
        def position = targetUtil.getTgtComponent('position')
        par.before(position)
        if(title) {
          def titleNd = targetUtil.getTgtComponent('text')
          titleNd.tagName('title')
          titleNd.attr('text', '<h2>' + title + '</h2>')
          titleNd.attr('cq:styleIds', '[' + targetUtil.textStyle['text-center'] + ']')
          position.selectFirst('>position-par').appendChild(titleNd)
        }
        position.selectFirst('>position-par').appendChild(par)
        position.tagName('par')
      }

      par.unwrap()
    }

    if(image && altText) {
      ele.selectFirst('>image').attr('alt', altText)
    }

    if(par && image) {

      def flex = targetUtil.getTgtComponent('flex')
      ele.before(flex)
      flex.tagName(ele.tagName())
      flex.attr('mobileItemContentAlignment','AlignItemContentCenter')
      flex.attr('mobilePaddingLeft','{Long}32').attr('mobilePaddingRight','{Long}32')
      flex.attr('mobilePaddingTop','{Long}56').attr('mobilePaddingBottom','{Long}56')

      // prepend and append 8% spacer at both side
      def spacer1 = targetUtil.getTgtComponent('spacer')
      def spacer2 = targetUtil.getTgtComponent('spacer')
      flex.selectFirst('>items').appendChild(spacer1)
      flex.selectFirst('>items').appendChild(ele)
      flex.selectFirst('>items').appendChild(spacer2)


      // 8, 43, 41, 8
      def item0 = flex.selectFirst('>mobileDefinitions>item0')
      item0.attr('widthCustomType','%').attr('width','custom')
      for(int i=0; i<4; i++) {
        if(i>0) {
          item0  = item0.clone()
          item0.tagName('item'+i)
          flex.selectFirst('>mobileDefinitions').appendChild(item0)
        }

        //0, 2, 1, 3
        if(i==0) {
          // space
          item0.attr('widthCustomValue', '8')
          item0.attr('order', '{Long}0')
        }

        if(i==1) {
          // image
          item0.attr('widthCustomValue', '44')
          item0.attr('order', '{Long}1')
          if(ele.attr('horizontallayout').endsWith('-right')) {
            // push image to right
            item0.attr('order', '{Long}2')
            image.attr('mobileMarginLeft', '{Long}36')
          } else {
            image.attr('mobileMarginRight', '{Long}36')
          }
        }

        if(i==2) {
          // par
          item0.attr('widthCustomValue', '40')
          item0.attr('order', '{Long}2')
          ele.selectFirst('>par>mobile').attr('paddingRight', '{Long}16').attr('paddingLeft', '{Long}16')
          if(ele.attr('horizontallayout').endsWith('-right')) {
            // push image to right
            item0.attr('order', '{Long}1')
            ele.selectFirst('>par>mobile').attr('marginRight', '{Long}16')
          } else {
            ele.selectFirst('>par>mobile').attr('marginLeft', '{Long}16')
          }
        }

        if(i==3) {
          // space
          item0.attr('widthCustomValue', '8')
          item0.attr('order', '{Long}3')
        }
      }

      if(ele.attr('horizontallayout').endsWith('-right')) {
        // push image to right
      } // mobileMarginRight={Long}16, pos/mobile/marginLeft={Long}16


    }

    ele.unwrap()
  }
}

// has basicLayout which is also center, use flex
// 3;grid-span-1of3;grid-span-1of3;grid-span-1of3;

// isCenter with offset, use flex, such as: 1-14-1, 1-12-2, 1-10-3, 1-8-4, 1-6-5, 1-4-6, 1;grid-cols-10 grid-offset-3
// 2;grid-cols-4 grid-offset-4;grid-cols-4;
// 3;grid-cols-4 grid-offset-2;grid-cols-4;grid-cols-4;

// total is not 16, use flex of non-center, use spacer on left

//
// columnType
// layout 1;grid-cols-12 grid-offset-2;
// basicLayout  1;grid-cols-12 grid-offset-2;
def recursiveDoColumn(Document doc) {

  def col = doc.selectFirst('[sling:resourceType=beagle/components/parsys/column][preprocessed]')
  if(col == null) {
    return
  }

  def colTagName = col.tagName()
  def isSingleCol = col.select('>pos').size()  == 1
  def layout = col.attr('layout')
  def basicLayout = col.attr('basicLayout')
  List<Integer[]> listWO = srcUtil.parseWidthOffset(layout, basicLayout)
  def isCenter = srcUtil.isCenter(listWO)
  int actualTotal = srcUtil.actualTotal(listWO)

  println 'col: ' + colTagName + ', isCenter: ' + isCenter + ', actualTotal: ' + actualTotal
  println listWO
  //println basicLayout
  if(listWO.size()>0) {
    col.select('>pos').eachWithIndex { pos, posIdx ->

      if(isSingleCol) {
        if(pos.children().size()>1) {
          // need extra pos
          def idx = commonUtil.getInteger(commonUtil.readAttrVal(pos, 'idx'))
          def position = targetUtil.getTgtComponent('position')
          pos.before(position)
          position.selectFirst('>position-par').appendChild(pos)
          position.tagName(colTagName + '_' + idx)
        }
      } else {
        if(posIdx>0 && listWO.get(posIdx)[1]>0) {
          // insert extra spacer for offset
          def spacer = targetUtil.getTgtComponent('spacer')
          pos.before(spacer)
          spacer.tagName('spacer_' + posIdx)
          println '\n.... add extra spacer'
        }
        //if(pos.children().size()>1) {
        // need extra pos
        def idx = commonUtil.getInteger(commonUtil.readAttrVal(pos, 'idx'))
        def position = targetUtil.getTgtComponent('position')
        pos.before(position)
        position.selectFirst('>position-par').appendChild(pos)
        position.tagName(colTagName + '_' + idx)
        //}
      }

      pos.unwrap()
    }

    // process col element
    def showBorders = col.hasAttr('showBorders') && 'true' == col.attr('showBorders')
    // use flex to hold background, multi-column, or single col with responsive
    def flex = isCenter ? targetUtil.getTgtComponent('flex_center_cols') : targetUtil.getTgtComponent('flex_cols')

    //if('t' == col.attr('section')) {
    flex.attr('mobileItemAlignment', 'AlignItemStart')
    //}

    if(col.hasAttr('whitespace') && col.attr('whitespace').trim()) {
      HashMap<String, Integer> padding = srcUtil.calcWhitespace(col.attr('whitespace'))
      flex.attr('mobilePaddingTop', ''+padding.paddingTop)
      flex.attr('mobilePaddingBottom', ''+padding.paddingBottom)
    }

    if(col.hasAttr('bgColor') && col.attr('bgColor').trim()) {
      HashMap<String, String> colors = srcUtil.readColor(col.attr('bgColor'))
      flex.attr('backgroundColorMobile', colors.bcolor)
      flex.attr('foregroundColorMobile', colors.color)
    }

    if(col.hasAttr('bgImgfile')) {
      def fileReference = srcUtil.getRef1(col.attr('bgImgfile'))
      if(allReferences.get(fileReference) != null) {
        fileReference = fileReference.replaceAll(srcAssetsPtn, destAssetsPtn)
      }

      flex.attr('fileReference', fileReference)
      flex.attr('fileReferenceMobile', fileReference)
      flex.attr('fileReferenceTablet', fileReference)
      flex.attr('imageRenditionDesktop', fileReference)
    }

    if(showBorders) {
      def mobile = flex.selectFirst('mobile')
      mobile.attr('borderAllColor', '#000000').attr('borderAllStyle','solid').attr('borderAllWidth','1')
    }

    if(isCenter && isSingleCol) {
      flex.selectFirst('mobileDefinitions>item0').attr('widthCustomValue', ''+Math.round(listWO.get(0)[0]/10))
    } else if(isSingleCol) {
      // offset
      flex.selectFirst('mobileDefinitions>item0').attr('widthCustomValue', ''+Math.round(listWO.get(0)[1]/10))
      // width
      def item1 = flex.selectFirst('mobileDefinitions>item0').clone()
      flex.selectFirst('mobileDefinitions').appendChild(item1)
      item1.tagName('item1')
      item1.attr('widthCustomValue', ''+Math.round(listWO.get(0)[0]/10))
    } else {
      //multiple item

      def widths = []
      listWO.each { item ->
        if(item[1]>0) {
          widths.add(item[1])
        }
        if(item[0]>0) {
          widths.add(item[0])
        }
      }

      def itemx = flex.selectFirst('mobileDefinitions>item0')
      widths.eachWithIndex { wd, idx ->
        if(idx>0) {
          itemx = itemx.clone()
          itemx.tagName('item' + idx)
          flex.selectFirst('mobileDefinitions').appendChild(itemx)
        }

        itemx.attr('widthCustomValue', ''+Math.round(wd/10))
      }
    }

    col.before(flex)
    flex.tagName(colTagName)
    flex.selectFirst('items').appendChild(col)
    if(col.selectFirst('>cq|responsive')) {
      // move hideInDesktop, hideInDesktop, hideInDesktop
      flex.appendChild(col.selectFirst('>cq|responsive'))
    }
    col.unwrap()
  } else {
    col.removeAttr('preprocessed')
  }

  recursiveDoColumn(doc)
}

def do_reimtabs(Document doc) {
  doc.select('[sling:resourceType=beagle/components/reimtabs]').each { ele ->

    ele.remove()
  }
}

// beagle/components/wishform
def do_wishform(Document doc) {

}

def doTabPanel(Document doc) {
  doc.select('[sling:resourceType=beagle/components/tabpanels]').each { ele ->
    def tabs = targetUtil.getTgtComponent('tablist')
    tabs.tagName(ele.tagName())
    ele.before(tabs)

    // "/root/content/navlist"
    def navListPath = commonUtil.getNodePath(tabs)
    navListPath = navListPath.replace('/jcr:content/contentbody', '/root/content')

    // dynamic tab naming
    int nextIdx = 1
    String nextNavListName = 'navlist_' + nextIdx
    while(navListNameUniqueCache.get(nextNavListName) != null) {
      nextNavListName = 'navlist_' + nextIdx++
    }
    tabs.attr('navListName', nextNavListName)
    String toggleId = 'toggle-' + nextNavListName.replace('_', '-').replace(' ', '-')

    def tabTitles = commonUtil.xmlAttr2Array(ele.attr('tabTitle'))
    def panels = ele.select('>[sling:resourceType=beagle/components/parsys]')
    tabTitles.eachWithIndex { tabTitle, idx ->
      def navitem = targetUtil.getTgtComponent('navitem')
      navitem.tagName('item' + idx)
      tabs.selectFirst('items').appendChild(navitem)
      navitem.attr('title', tabTitle)

      // dynamic uuid between item and position
      def navItemUuid = UUID.randomUUID().toString()
      navitem.attr('navItemUuid', navItemUuid)

      def srcPanel = panels.get(idx)
      def npanel = targetUtil.getTgtComponent('tabpanel')
      npanel.tagName(srcPanel.tagName())

      // Add in the navList specific attributes
      npanel.attr('navItemId', navItemUuid)
      npanel.attr('navItemName', navItemUuid)
      npanel.attr('navListName', nextNavListName)
      npanel.attr('navListPath', navListPath)
      npanel.attr('toggleId', toggleId)
      npanel.attr('visibilityComponent', 'navlist')
      ele.before(npanel)

      srcPanel.children().each {
        npanel.selectFirst('position-par').appendChild(it)
      }
    }

    ele.remove()
  }
}

// *8
// top-40px, bottom 40px
// class="grid-cols-14 grid-offset-1 text-center">

// use 87% item to hold 2 pos, one for intro, one for others
def do_features(Document doc) {
  doc.select('[sling:resourceType=beagle/components/features]').each { ele ->

    // class="grid-cols-14 grid-offset-1 text-center" => flex (item 87% center, text-center
    def featureFlex = targetUtil.getTgtComponent('flex')
    ele.before(featureFlex)
    featureFlex.tagName(ele.tagName())
    featureFlex.attr('mobileJustification', 'JustifyCenter')
    featureFlex.attr('mobileItemAlignment', 'AlignItemStart')
    def item0 = featureFlex.selectFirst('mobileDefinitions>item0')
    item0.attr('width', 'custom')
    item0.attr('widthCustomType', '%')
    item0.attr('widthCustomValue', '87')

    def flexPos = targetUtil.getTgtComponent('position')
    featureFlex.selectFirst('>items').appendChild(flexPos)

    // if there is intro
    ele.select('>*').each { intro ->
      if(intro.attr('sling:resourceType') && intro.attr('sling:resourceType') != 'beagle/components/featurewidget') {
        intro.attr('cq:styleIds', '[' + targetUtil.textStyle['text-center'] + ']')
        flexPos.selectFirst('>position-par').appendChild(intro)
      }
    }

    ele.select('>[sling:resourceType=beagle/components/featurewidget]').each { featurewidget ->
      // class="grid-cols-14 grid-offset-1 => flex (item 87% center, text-center
      flexPos.selectFirst('>position-par').appendChild(featurewidget)
    }

    ele.remove()
  }
}

def do_featurewidget(Document doc) {
  doc.select('[sling:resourceType=beagle/components/featurewidget]').each { ele ->
    def h3title = ele.attr('title')
    def h3titleNd = targetUtil.getTgtComponent('text')
    h3titleNd.tagName('widgetH3')
    h3titleNd.attr('text', "<h3>${h3title}</h3>")
    ele.before(h3titleNd)

    if(ele.selectFirst('>fw-content')!=null) {
      def fwContent = ele.selectFirst('>fw-content')

      def pos = targetUtil.getTgtComponent('position')
      pos.tagName('fw-content')
      ele.before(pos)
      pos.selectFirst('>position-par').appendChild(fwContent)
      fwContent.unwrap()
    }

    ele.remove()
  }
}

def doCalendar(Document doc) {
  doc.select('[sling:resourceType=beagle/components/calendar]').each { ele ->

    ele.select('>*').each { intro ->
      if(intro.attr('sling:resourceType') != 'beagle/components/calendar/featuregroup') {
        // grid-cols-6 grid-offset-5 text-center => flex (item 38% center, text-center
        def introFlex = targetUtil.getTgtComponent('flex')
        ele.before(introFlex)
        introFlex.tagName('intro')

        introFlex.attr('mobileJustification', 'JustifyCenter')
        introFlex.attr('mobileItemAlignment', 'AlignItemStart')

        def item0 = introFlex.selectFirst('mobileDefinitions>item0')
        item0.attr('width', 'custom')
        item0.attr('widthCustomType', '%')
        item0.attr('widthCustomValue', '38')


        introFlex.selectFirst('>items').appendChild(intro)
        intro.attr('cq:styleIds', '[' + targetUtil.textStyle['text-center'] + ']')

      }
    }

    ele.select('>[sling:resourceType=beagle/components/calendar/featuregroup]').each { featuregroup ->

      def title = featuregroup.attr('title')
      // "{Date}2019-04-02T21:35:00.000-07:00"
      def date = featuregroup.attr('date')?.substring(6, 16)
      def sd1 = new SimpleDateFormat('yyyy-MM-dd').parse(date)
      // 2019-Apr.-02
      def sd2 = new SimpleDateFormat('yyyy-MMM-dd').format(sd1)
      //println "SD2***********************" +sd2
      def leftTxt = '<p><span>' + sd2.substring(5,8) + '</span><span>-' + sd2.substring(9, 11) + '-</span><span>' + sd2.substring(0, 4) + '</span></p>'
      //println "leftTxt***********************" +leftTxt
      def groupFlex = targetUtil.getTgtComponent('flex')
      ele.before(groupFlex)
      groupFlex.tagName(featuregroup.tagName())
      groupFlex.attr('mobilePaddingTop', '{Long}16')

      def leftTxtNd = targetUtil.getTgtComponent('text')
      leftTxtNd.attr('text', leftTxt)
      groupFlex.selectFirst('>items').appendChild(leftTxtNd)

      def rPos = targetUtil.getTgtComponent('position')
      rPos.tagName('rpos')
      groupFlex.selectFirst('>items').appendChild(rPos)

      def item0 = groupFlex.selectFirst('mobileDefinitions>item0')
      item0.attr('width', 'custom')
      item0.attr('widthCustomType', '%')
      item0.attr('widthCustomValue', '10')

      item0 = item0.clone()
      groupFlex.selectFirst('>mobileDefinitions').appendChild(item0)
      item0.attr('widthCustomValue', '70')
      // adjustment both to center
      groupFlex.attr('mobileJustification', 'JustifyCenter')

      def h3 = targetUtil.getTgtComponent('text')
      h3.tagName('h3')
      h3.attr('text', "<h3>${title}</h3>")
      rPos.selectFirst('>position-par').appendChild(h3)

      def features = targetUtil.getTgtComponent('position')
      rPos.selectFirst('>position-par').appendChild(features)
      features.attr('mobileGap', "{Long}32")

      featuregroup.select('>[sling:resourceType=beagle/components/calendar/feature]').eachWithIndex { feature, idx ->

        def featureTxt = targetUtil.getTgtComponent('text')
        featureTxt.tagName(feature.tagName())
        def fTitle = feature.attr('title')
        if(fTitle) {
          featureTxt.attr('text', "<h5>${fTitle}</h5>" + feature.attr('text'))
        } else {
          featureTxt.attr('text', feature.attr('text'))
        }

        def featureTxtPadding = targetUtil.getTgtComponent('position')
        featureTxtPadding.selectFirst('>position-par').appendChild(featureTxt)
        featureTxtPadding.selectFirst('>mobile').attr('paddingRight', '{Long}24').attr('paddingTop', '{Long}24')
        features.selectFirst('>position-par').appendChild(featureTxtPadding)
        def txtResp = targetUtil.getTgtComponent('cq:responsive')
        featureTxtPadding.appendChild(txtResp)
        txtResp.selectFirst('>default').attr('width', '3').attr('offset', '0')
        txtResp.selectFirst('>tablet').attr('width', '5').attr('offset', '0')
        txtResp.selectFirst('>phone').attr('width', '11').attr('offset', '0')

        if(idx % 2 == 0) {
          txtResp.selectFirst('>tablet').attr('behavior', 'newline')
        }
        if(idx % 4 == 0) {
          txtResp.selectFirst('>default').attr('behavior', 'newline')
        }
      }
    }

    ele.remove()
  }
}

// create a flex to hold the height
// create a position to fold the child (title_article, vide_player)

// image[fileReference, alt]
// hero container properties: image and height

// page properties: hidePersonalizedHero="hide"
def doHero(Document doc) {
  def heightMap = ['hero2-size-1':548,'hero2-size-2':544,'hero2-size-3':400,'hero2-size-4':272,'hero2-size-5':208]
  def bgcolorMap = ['hero2-theme-1':'#000','hero2-theme-2':'#fff']
  def alignMap = ['hero2-align-0':'JustifyCenter', 'hero2-align-1':'JustifyStart', 'hero2-align-2':'JustifyCenter', 'hero2-align-3':'JustifyEnd']
  def valignMap = [ 'hero2-avoid-3':'AlignItemStart', 'hero2-avoid-2':'AlignItemCenter', 'hero2-avoid-1':'AlignItemEnd']
  def widthMap =['hero2-basis-0':'512', 'hero2-basis-1':'720', 'hero2-basis-2':'512']

  // push to flex bottom: mobileItemAlignment="AlignItemEnd", or top: mobileItemAlignment="AlignItemStart", mobileItemAlignment="AlignItemCenter"
  // left, center, right of flex: mobileJustification="JustifyCenter", mobileJustification="JustifyEnd", mobileJustification="JustifyStart"
  doc.select('[sling:resourceType=beagle/components/hero]').each { hero ->

    // height = hero.attr('height')
    // bgcolor = hero.attr('color')
    // valign = hero.attr('avoid')
    // pos_width = hero.attr('pos_width')
    // creditsAlignment = hero.attr('creditsAlignment')
    // articleAlignment = hero.attr('articleAlignment')

    def heroFlex = targetUtil.getTgtComponent('flex')
    hero.before(heroFlex)
    heroFlex.tagName(hero.tagName())

    if(hero.hasAttr('color')) {
      heroFlex.attr('backgroundColorMobile', 'hero2-theme-1' == hero.attr('color')? '#000000':'#ffffff')
      heroFlex.attr('foregroundColorMobile', 'hero2-theme-1' == hero.attr('color')? '#ffffff':'#000000')
    }

    if(hero.hasAttr('fileReference')) {
      def fileReference = hero.attr('fileReference')
      fileReference = srcUtil.getRef1(fileReference)
      if(allReferences.get(fileReference) != null) {
        fileReference = fileReference.replaceAll(srcAssetsPtn, destAssetsPtn)
      }

      heroFlex.attr('fileReference', fileReference)
      heroFlex.attr('fileReferenceMobile', fileReference)
      heroFlex.attr('fileReferenceTablet', fileReference)
      heroFlex.attr('imageRenditionDesktop', fileReference)
      heroFlex.attr('alt', hero.attr('alt'))
    }

    if(hero.hasAttr('height') && heightMap.get(hero.attr('height'))) {
      heroFlex.attr('mobileMinHeight', '{Long}' + heightMap.get(hero.attr('height')))
    }

    // default hero alignment center and middle
    heroFlex.attr('mobileJustification', 'JustifyCenter')
    heroFlex.attr('mobileItemContentAlignment', 'AlignItemContentCenter')

    if(hero.hasAttr('articleAlignment')) {
      // mobileJustification left, center, right
      heroFlex.attr('mobileJustification', alignMap.get(hero.attr('articleAlignment')))
    }
    if(hero.hasAttr('creditsAlignment')) {
      // mobileJustification top, center, bottom
      heroFlex.attr('mobileJustification', alignMap.get(hero.attr('creditsAlignment')))
    }
    if(hero.hasAttr('avoid')) {
      // mobileItemAlignment left, center, right
      heroFlex.attr('mobileItemAlignment', valignMap.get(hero.attr('avoid')))
    }

    if(hero.hasAttr('pos_width')) {
      def item0 = heroFlex.selectFirst('mobileDefinitions>item0')
      item0.attr('width', 'custom')
      item0.attr('widthCustomType', 'px')
      item0.attr('widthCustomValue', widthMap.get(hero.attr('pos_width')))
    } else {
      // hard code width
      def item0 = heroFlex.selectFirst('mobileDefinitions>item0')
      item0.attr('width', 'custom')
      item0.attr('widthCustomType', 'px')
      item0.attr('widthCustomValue', '720')
    }

    // embed pos to hold title, text and other
    def heroPos = targetUtil.getTgtComponent('position')
    heroFlex.selectFirst('>items').appendChild(heroPos)
    heroPos.selectFirst('>position-par').appendChild(hero)
    heroPos.selectFirst('>mobile').attr('paddingTop', '{Long}16').attr('paddingBottom', '{Long}16').attr('paddingLeft', '{Long}16').attr('paddingRight', '{Long}16')
    hero.unwrap()
  }
}

// must be invoked after doColumn, append a full-width text component into transformed column if hidePhone=false
// 1. convert bar to a position, with/without purchasePhoneText, numberDisp when false
// 1. unwrap first-level parsys which is useless
def doMerchandisingbar(Document doc) {
  doc.select('[sling:resourceType=beagle/components/merchandisingbar]').each { mbar ->
    def tagName = mbar.tagName()
    def par = mbar.selectFirst('>merchandising[sling:resourceType=beagle/components/parsys]')

    def posx = targetUtil.getTgtComponent('position')
    posx.tagName(tagName)
    mbar.before(posx)
    // use style align-center
    //posx.attr('cq:styleIds', '['+DexterUtil.styleMapping.'positionStyle'.'doccloud-Position-center'+']')
    def position_par = posx.selectFirst('>position-par')
    def mobile = posx.select('>mobile')
    mobile.attr('marginTop', '{Long}16')
    mobile.attr('marginBottom', '{Long}16')
    position_par.appendChild(mbar)

    if(par) {
      targetUtil.unwrap(par)
    }
    targetUtil.unwrap(mbar)
  }
}

// if openInModal=true
// else !insideModal
def do_faasform(Document doc) {
  doc.select('[sling:resourceType=beagle/components/faasform]').each { ele ->

    def faasform = targetUtil.getTgtComponent('faasform')
    faasform.tagName(ele.tagName())

    def template = faasform.selectFirst('>template')
    // url mapping for thankyou page
    template.attr('destinationUrl', ele.attr('destinationURL'))

    if(ele.hasAttr('modalTitle') && 'false' != ele.attr('modalID')) {
      template.attr('faasFormTitle', ele.attr('modalTitle'))
    }

    if(ele.hasAttr('formLocale')) {
      template.attr('formLanguage', ele.attr('formLocale'))
    }

    if(ele.hasAttr('formSubType')) {
      template.attr('formSubType', ele.attr('formSubType'))
    }

    if(ele.hasAttr('formTemplate')) {
      template.attr('formTemplate', ele.attr('formTemplate'))
    }

    if(ele.hasAttr('formType')) {
      template.attr('formType', ele.attr('formType'))
    }

    if(ele.hasAttr('intCampaignId')) {
      template.attr('internalCampaignId', ele.attr('intCampaignId'))
    }

    if(ele.hasAttr('sku')) {
      template.attr('sku', ele.attr('sku'))
    }

    def openInModal = ele.attr('openInModal')

    if(openInModal && 'true' == openInModal) {
      // create a modal
      def modalID = ele.attr('modalID')
      def modalTagname = 'faas' + modalID.replace('[','').replace(']','')
      println modalTagname

      def size = ele.attr('modalSize')?.split('x') ?: ['480', '360']
      def modalTpl = createModal(doc, modalID, size[0], size[1])
      modalTpl.tagName(modalTagname)

      // use modal to hold faasForm
      modalTpl.selectFirst('>responsive-grid').appendChild(faasform)
    } else {
      ele.before(faasform)
    }

    ele.remove()
  }
}

def createSimpleTable(tblBody) {
  // unwrap simple nested table
  tblBody.select('td>table').each { nestTbl ->
    if(nestTbl.select('th,td').size() == 1) {
      //td.appendChild(td)
      def nestCell = nestTbl.selectFirst('th,td')
      nestTbl.parent().appendChild(nestCell)
      nestCell.unwrap()
      nestTbl.remove()
    } else {
      // nested table
      // throw new Exception('nested table: ')
      def txtTbl = targetUtil.getTgtComponent('text')
      nestTbl.before(txtTbl)
      txtTbl.tagName('tblNestedTxt')
      txtTbl.attr('text', nestTbl.toString())
      nestTbl.remove()
    }
  }

  // remove empty thead
  tblBody.select('thead:not(:has(*))').each { it.remove() }

  // calculate columns
  def columns = 0
  def columnWidths = [:]
  def colWidthType = '%'
  tblBody.select('table>tr,thead>tr,tbody>tr').each { tr ->
    columns = columns?:tr.select('th,td').size()
    tr.select('th,td').each { td ->
      def colIdx = 'c' + td.elementSiblingIndex()
      def tdW = td.attr('width') ?: commonUtil.getStyleMap(td).get('width')
      if(tdW) {
        // content/help/en/after-effects/using/expression-basics: NaN%
        colWidthType = tdW.contains('%')? '%' : 'px'
        customWidth = (tdW - '%').isNumber() ? (tdW - '%').toDouble() : 0
        def width_i = columnWidths.get(colIdx)? columnWidths.get(colIdx) : 0
        if(width_i < customWidth) {
          columnWidths.put(colIdx, customWidth)
        }
      } else if(columnWidths.get(colIdx) == null) {
        // in case only one column has width
        columnWidths.put(colIdx, 0)
      }
    }
  }

  // if width in %, recalculated
  if('%' == colWidthType) {
    def totalW = 0
    // when not all column width are specified
    def hasZero = false
    columnWidths.each { key, val ->
      if(val == 0 || hasZero) {
        totalW = 100
        hasZero = true
      } else {
        totalW += val
      }
    }

    if(totalW>0) {
      def columnWidthsClone = columnWidths.clone()
      columnWidthsClone.each { key, val ->
        def x = val * 100 / totalW
        x = Math.round(x)
        columnWidths.put(key, x)
      }
    }
  }

  def rows = tblBody.select('thead>tr,tbody>tr,table>tr').size()

  if(rows == 0 || columns == 0) {
    return null
  }

  // create target table
  def table = targetUtil.getTgtComponent('table')
  // remove border by default,  borderColor, borderStyle, borderWidth
  //table.removeAttr('borderColor').removeAttr('borderStyle').removeAttr('borderWidth')
  table.removeAttr('tableCaptionText').removeAttr('captionLocation')
  if(tblBody.select('thead>tr').size()>0) {
    table.attr('hasRowHeader', '{Boolean}true')
  }
  def srcTbl = tblBody.selectFirst('table')
  if(srcTbl.hasAttr('cellpadding')) {
    table.attr('cellPadding', '{Long}' + srcTbl.attr('cellpadding'))
  }

  if(srcTbl.hasAttr('cellspacing')) {
    table.attr('cellSpacing', '{Long}' + srcTbl.attr('cellspacing'))
  }

  if(srcTbl.hasAttr('border')) {
    // table.attr('borderColor', '#696969').attr('borderStyle', 'solid').attr('borderWidth', '{Long}1')
    table.attr('borderColor', '#bdbdbd').attr('borderStyle', 'solid').attr('borderWidth', '{Long}1')
  }

  if(srcTbl.hasAttr('width') && '100%' != srcTbl.attr('width')) {
    table.attr('width', 'custom').attr('customWidth', '{Long}'+srcTbl.attr('width')).attr('customWidthType', 'px')
  }

  if(srcTbl.selectFirst('caption')) {
  }

  def cellNd = table.selectFirst('>[sling:resourceType=dexter/components/super/parsys]')
  def config_columns = table.selectFirst('>config>columns')
  def config_columns_ci = table.selectFirst('>config>columns>*')
  def config_rows = table.selectFirst('>config>rows')
  def config_rows_ri = table.selectFirst('>config>rows>*')

  // empty sample data
  config_rows.empty()
  config_columns.empty()
  table.select('[sling:resourceType=dexter/components/super/parsys],rows>*,columns>*').each {
    it.remove()
  }

  // appendChild rowIds, colIds
  def rowIds = table.selectFirst('>rows')
  for(int i=0; i<rows; i++) {
    rowIds.append("<item${i} jcr:primaryType=\"nt:unstructured\" id=\"r${i}\"/>")
  }

  def colIds = table.selectFirst('>columns')
  for(int i=0; i<columns; i++) {
    colIds.append("<item${i} jcr:primaryType=\"nt:unstructured\" id=\"c${i}\"/>")
  }

  // appendChild configNd width
  def width = 100 / columns
  (0 .. columns-1).each { idx ->
    def cfgx = commonUtil.clone(config_columns_ci)
    cfgx.tagName('c' + idx)
    //cfgx.attr('width', ''+width)

    if('%' == colWidthType) {
      cfgx.attr('widthType', '%')
      if(columnWidths.get(cfgx.tagName())) {
        cfgx.attr('width', ''+columnWidths.get(cfgx.tagName()))
      }
    } else {
      cfgx.attr('widthType', 'px')
      if(columnWidths.get(cfgx.tagName())) {
        cfgx.attr('width', ''+columnWidths.get(cfgx.tagName()))
      }
    }

    config_columns.appendChild(cfgx)
  }

  //add rows
  def rowidx = 0
  tblBody.select('thead>tr,tbody>tr, table>tr').each { srcRow ->
    addTableRow(srcRow, rowidx, table, cellNd)
    def rowi = commonUtil.clone(config_rows_ri)
    rowi.tagName('r'+rowidx)
    config_rows.appendChild(rowi)
    rowidx++
  }

  return table
}

def preprocessTextTable(htmlBody) {

  if(htmlBody.select('[rowspan],[colspan]').size() == 0) {
    //println htmlBody.html()
    htmlBody.select('[rowspan]').each { td ->
      // copy into next rows
      def rows = td.attr('rowspan').toInteger()
      def tdIdx = td.elementSiblingIndex()

      td.removeAttr('rowspan')
      def tr = td.parent()
      for(int i=1; i<rows; i++) {
        tr = tr.nextElementSibling()
        def ntds = []
        ntds.add(commonUtil.clone(td))
        println 'insertChildren rowspan: ' + td.text() + ', ' + (tdIdx+1)
        tr.insertChildren(tdIdx+1, ntds)
      }
    }

    htmlBody.select('[colspan]').each { td ->
      // copy into same row
      def cols = td.attr('colspan').toInteger()
      td.removeAttr('colspan')
      for(int i=1; i<cols; i++) {
        def tdCopy = commonUtil.clone(td)
        td.after(tdCopy)
      }
    }
  }

  // remove empty td, tr
  //htmlBody.select('td:matches(^$)').each { it.remove() }
  //htmlBody.select('tr:matches(^$)').each { it.remove() }

  // unwrap fake table, which has only one cell
  //  htmlBody.select('tr:only-child>td:only-child').each { td ->
  //    def el = td
  //    while(el.parent().tagName() != 'table') {
  //      el = el.parent()
  //    }
  //    el.before(td)
  //    el.remove()
  //    td.unwrap()
  //  }
}

def postChartTable(Document doc) {
  doc.select('[sling:resourceType=beagle/components/comparisonchart][tableDataHTML]').each { ele ->
    def tableHTML = ele.attr('tableDataHTML')
    def htmlBody = Jsoup.parseBodyFragment(tableHTML).body()

    def table = table = createSimpleTable(htmlBody)
    table.tagName(ele.tagName())
    ele.before(table)
    table.attr('borderColor','#bdbdbd').attr('borderStyle','solid').attr('borderWidth','{Long}1')
    ele.remove()
  }
}

def doTextTable(Document doc) {
  doc.select('[sling:resourceType=beagle/components/table][tableDataHTML]').each { ele ->

    def tableHTML = ele.attr('tableDataHTML')
    def htmlBody = Jsoup.parseBodyFragment(tableHTML).body()

    if(htmlBody.select('table').size()>0) {

      // println commonUtil.getNodePath(ele)
      preprocessTextTable(htmlBody)

      def table = null
      if(htmlBody.select('[rowspan],[colspan]').size() == 0) {
        // only do simple table
        table = createSimpleTable(htmlBody)
      }

      if(table) {
        table.tagName(ele.tagName())

        def showBorder = (ele.hasAttr('showBorder') && 'true' == ele.attr('showBorder'))

        if(ele.hasAttr('draftOnly')) {
          table.attr('draftOnly', ele.attr('draftOnly'))
        }

        ele.before(table)
        if(ele.hasAttr('hasHeader')) {

          table.attr('hasRowHeader','{Boolean}' + ele.attr('hasHeader'))
        }
        // remove border if needed: borderStyle
        if(htmlBody.select('[style*=border: none],[style*=border:none]').size()>0) {
          table.removeAttr('borderStyle')
        }
        if(showBorder) {
          table.attr('borderColor','#bdbdbd').attr('borderStyle','solid').attr('borderWidth','{Long}1')
        }

        //remove old text_table
        table.appendChild(ele)

        targetUtil.unwrap(ele)
      } else {
        // do Text
        def text = targetUtil.getTgtComponent('text')
        ele.before(text)
        if('true' == ele.attr('hasHeader')) {
          htmlBody.select('tr:first-child>*').each {
            it.tagName('th')
          }
          text.attr('text', htmlBody.toString())
        } else {
          text.attr('text', ele.attr('text'))
        }

        if(ele.hasAttr('draftOnly')) {
          text.attr('draftOnly', ele.attr('draftOnly'))
        }

        text.tagName(ele.tagName())
        text.appendChild(ele)

        ele.unwrap()
        postDoText(text)
      }
    }
  }
}

// invoked by doTable
def addTableRow(Element srcRow, int rowidx, final Element table, final Element cellTpl) {
  srcRow.select('>th,td').each {
    // wrap with p tag when it is plain text
    if(it.html() == it.text()) {
      it.html('<p>'+it.html()+'</p>')
    }
  }

  srcRow.select('th,td').eachWithIndex { cell, idx ->
    def destCell = commonUtil.clone(cellTpl)
    destCell.tagName("row-r${rowidx}-column-c${idx}")
    // use text as cell component
    def text = targetUtil.getTgtComponent('text')
    text.tagName('cellTxt')

    // use cell html directly without extra
    def css='', style=''
    cell.select('>[style]').each {
      style = it.attr('style')
      it.removeAttr('style')
    }
    cell.select('>[class]').each {
      css = it.attr('class')
      it.removeAttr('class')
    }

    text.attr('text', cell.html())
    if(cell.tagName() == 'th') {
      text.attr('text', '<b>'+cell.html()+'</b>')
    }
    def styles = []
    if(style.contains('text-align: left')) {
      styles.add(targetUtil.textStyle['text-left'])
    } else if(style.contains('text-align: center') || cell.tagName() == 'th') {
      styles.add(targetUtil.textStyle['text-center'])
    } else if(style.contains('text-align: right')) {
      styles.add(targetUtil.textStyle['text-right'])
    }

    //      text-0-block --> heading-XXL
    //      text-1-block --> heading-XXL
    //      text-2-block --> heading-XL
    //      text-3-block --> heading-L / body-XXL
    //      text-4-block --> heading-M
    //      text-5-block --> heading-S / body-XL
    //      text-6-block --> heading-XXS / body-S
    //      text-7-block --> paragraph

    if(css.contains('text-0-block') || css.contains('text-1-block')) {
      styles.add(targetUtil.textStyle['heading-XXL'])
    } else if(css.contains('text-2-block')) {
      styles.add(targetUtil.textStyle['heading-XL'])
    } else if(css.contains('text-3-block')) {
      styles.add(targetUtil.textStyle['heading-L'])
    } else if(css.contains('text-4-block')) {
      styles.add(targetUtil.textStyle['heading-M'])
    } else if(css.contains('text-5-block')) {
      styles.add(targetUtil.textStyle['heading-S'])
    } else if(css.contains('text-6-block')) {
      styles.add(targetUtil.textStyle['body-S'])
    }
    def styleIts = styles.iterator()
    styleIts.each { styleIt ->
      if(styleIt == null) {
        styleIts.remove()
      }
    }

    text.attr('cq:styleIds', styles.toString())

    destCell.appendChild(text)
    postDoText(text)
    table.appendChild(destCell)
  }
}

def transferPageProperties(Document doc, Document docDest) {


  def jcrNode = doc.selectFirst('jcr|root>jcr|content')
  def destJcrNode = docDest.selectFirst('jcr|root>jcr|content')

  def attrs = jcrNode.attributes()
  attrs.each { attr ->

    if(!attr.key.contains(':') || attr.key.startsWith('jcr:') || attr.key.startsWith('cq:') || attr.key.startsWith('mix:') || attr.key.startsWith('sling:')) {
      if(attr.key != 'cq:template' && attr.key != 'sling:resourceType') {
        destJcrNode.attr(attr.key, attr.value)
      }

      if('jcr:title' == attr.key) {
        destJcrNode.attr('pageTitle', attr.value)
      }
    }
  }

  if(destJcrNode.hasAttr("browserTitle") && destJcrNode.attr("browserTitle")) {
    destJcrNode.attr('pageTitle', destJcrNode.attr("browserTitle"))
  }
}

// jcr:mixinTypes, cq:isCancelledForChildren, cq:propertyInheritanceCancelled
def copyExtraProps(src_ele, tgt_ele) {
  [
    'jcr:mixinTypes',
    'cq:isCancelledForChildren',
    'cq:propertyInheritanceCancelled',
    'cq:LiveRelationship',
    'cq:LiveSyncCancelled',
    'cq:PropertyLiveSyncCancelled',
    'mix:versionable'
  ].each { key ->
    if(src_ele.hasAttr(key)) {
      tgt_ele.attr(key, src_ele.attr(key))
    }
  }
}

// according to langMaster rules
def addExtraNodeProperties(Document doc, Document docDest, String srcPagePath) {

  // 1. add jcr:mixinTypes=[cq:LiveRelationship] into existing, or language sites
  // is country site, and not en, and not langmaster-replication
  if(srcPagePath.contains('/langmaster/') || (! srcPagePath.contains('content/acom/en/') && ! srcPagePath.contains('/langmaster-replication/'))) {
    docDest.selectFirst('jcr|root>jcr|content').select('*').each { node ->
      def mixes = node.attr('jcr:mixinTypes') ?: '[cq:LiveRelationship]'

      if(!mixes.contains('cq:LiveRelationship')) {
        // in case missing
        if(mixes.length()>5) {
          mixes = mixes - ']' + ',cq:LiveRelationship]'
        } else {
          mixes = '[cq:LiveRelationship]'
        }
      }

      node.attr('jcr:mixinTypes', mixes)
    }
  }

  // 2. add jcr:language on country sites pages only
  if(! srcPagePath.contains('/langmaster/') && ! srcPagePath.contains('content/acom/en/') && ! srcPagePath.contains('/langmaster-replication/')) {
    def langVal = docDest.selectFirst('jcr|root>jcr|content').attr('jcr:language')
    if(langVal) {
      docDest.selectFirst('jcr|root>jcr|content').select('*').each { node ->
        if(node.hasAttr('jcr:language')) {
          // only set when missing
        } else {
          node.attr('jcr:language', langVal)
        }
      }
    }
  }
}
