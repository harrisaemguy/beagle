
/*
 1. collect source template, components, and content attributes
 */
import java.util.regex.*
import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.util.regex.*

import groovy.io.FileType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

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
}catch(any) {

  //any.printStackTrace()
  scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
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
commonUtil.projectDir = projectDir
targetUtil.projectDir = projectDir
targetUtil._init()
println projectDir
// ==================== end: resolve projectDir and shared library =========

// Specify srcPkg location
srcPkgDir = projectDir + '/srcPkgs'
allPkgsDir = projectDir + '/srcPkgs'
dxTarget = 'destPkg'

// clean targetTemplate
String inputFolder = projectDir + '/targetTemplate'
LinkedList<String> xmlFiles = commonUtil.findRelativePaths(new File(inputFolder), FileType.FILES, '.*\\.xml')
xmlFiles.each { xmlFilename ->
  String inputFile = inputFolder + '/' + xmlFilename
  commonUtil.cleanXML(inputFile)
}

if(! new File(srcPkgDir).exists()) {
  throw new Exception('please unzip source content into ' + srcPkgDir)
}

// clean destination folder before migration
FileUtils.deleteDirectory(new File(projectDir + '/' + dxTarget))

// start migration process
def start = System.currentTimeMillis()
srcComponents = [:]
srcAttrs = [:]

if(true) {
  collectSrcContent(new File(allPkgsDir))

  def refLines = []
  srcComponents.each { ref, type ->
    refLines.add("${ref}|${type}")
  }
  reportUtil.printReport("srcComponents.xlsx", 'srcComponents', 'ref|type', refLines)

  srcAttrs.each { ref, attrValues ->

    refLines = []
    attrValues.each { attr, attrValue ->
      refLines.add("${attr}|${attrValue}")
    }
    reportUtil.printReport('tmp/'+ref.replace('/', '_') + ".xlsx", 'srcAttrs', 'attr|value', refLines)
  }
}

def collectSrcContent(final File root_dir) {

  root_dir.eachFileRecurse (FileType.FILES) { file ->
    if(file.name == '.content.xml') {

      def pagePath = file.absolutePath - '/.content.xml'
      pagePath = pagePath.substring(pagePath.indexOf('/jcr_root/') + 9)
      println pagePath

      String xml =  commonUtil.file2xml(file)
      Document doc = commonUtil.xml2dom(xml)

      commonUtil.cleanXmlAttributes(doc)

      if(doc.select('[sling:resourceType=beagle/components/fullwidthcontentpage]').size() == 1) {
        doc.select('[sling:resourceType]').each { ele ->
          def resourceType = ele.attr('sling:resourceType')
          if(ele.tagName() == 'jcr:content') {
            resourceType = resourceType + '_p'
          }
          if(srcComponents.get(resourceType)) {
            def count = srcComponents.get(resourceType) +1
            srcComponents.put(resourceType, count)
          } else {
            srcComponents.put(resourceType, 1)
          }

          // other attrs
          def attrIts = ele.attributes.iterator()
          while(attrIts.hasNext()) {
            def attr = attrIts.next()
            def key = attr.key;
            if(attr.value && key != 'sling:resourceType' && key != 'jcr:primaryType') {

              if(srcAttrs.get(resourceType)) {
                def exists = srcAttrs.get(resourceType)
                if(!exists.get(key)) {
                  exists.put(key, attr.value)
                }
              } else {
                def values = [:]
                values.put(key, attr.value)
                srcAttrs.put(resourceType, values)
              }
            }
          }
        }
      }
    }
  }
}
