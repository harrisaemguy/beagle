import java.util.regex.*
import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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
packageName = 'loc'
packageVersion = '1.0'
def ciFile = null // if null, then migrate all article-3 pages

// provide ci list to migrate from above srcPkg location
ciFile = new File(projectDir + '/test.txt')

srcPkgDir = projectDir + '/srcPkgs'

allPkgsDir = projectDir + '/srcPkgs'
srcSitename = 'help'
dxTarget = 'destPkg'
doDeploy = false
doAsset = true
doPage = true
doReference = true

// global variables
fragmentpages         = new LinkedList<String>()

// src -> tgt
assets                = new TreeMap<String, String>()

// report collection
InvalidResourceTypes  = new HashMap<String, List<String>>()
missingSrc            = new HashMap<String, String>()
srcTargetPages        = new HashMap<String, String>()
otherPages            = new HashMap<String, String>()
accordItemDraft       = new HashMap<String, String>()
// append /jcr:content to be the package filter
targetPages           = new TreeMap<String, Integer>()
// broken links found during migration
brokenlinks = [:]

// \$\(.div.accordion-item-content.\)\.first\(\)\.css\(.display., .block.\);
firstAccordionSelectedPat = Pattern.compile('\\$\\(.div.accordion-item-content.\\)\\.first\\(\\)\\.css\\(.display., .block.\\);')

// regex mapping src to dest
srcPagesPtn = 'content/'+srcSitename+'/(.*?)'
destPagesPtn = 'content/'+dxTarget+'/$1'
srcFragPtn = 'content/'+srcSitename+'/(.*?)/fragments/(.*)$'
destFragPtn = 'content/experience-fragments/'+dxTarget+'/$1/$2/master'
srcAssetsPtn = 'content/dam/'+srcSitename+'/(.*?)'
destAssetsPtn = 'content/dam/'+srcSitename+'/$1'

if(! new File(srcPkgDir).exists()) {
  throw new Exception('please unzip source content into ' + srcPkgDir)
}

// clean destination folder before migration
FileUtils.deleteDirectory(new File(projectDir + '/' + dxTarget))

// start migration process
def start = System.currentTimeMillis()
srcpageMap = [:]
allReferences = [:]
skippedReferences = [:]
if(doReference) {
  allReferences = srcUtil.findAllReferenceList(new File(allPkgsDir), '[sling:resourceType=help/components/pages/article-3]', '[sling:resourceType=help/components/reference][path]')

  def refLines = []
  allReferences.each { ref, type ->
    refLines.add("${ref}|${type}")
  }
  reportUtil.printReport("all_references.xlsx", 'all references', 'ref|type', refLines)
}

// create a txt file for reuse
new File(projectDir+'/all_references.csv').withWriter { out ->
  allReferences.each { ref, type ->
    out.println ref + ',' + type
  }
}
