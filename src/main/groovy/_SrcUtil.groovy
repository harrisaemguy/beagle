import java.nio.charset.Charset;
import java.util.regex.*
import groovy.io.FileType
import org.jsoup.Jsoup
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
class SrcUtil {

  def commonUtil
  def targetUtil
  def adobeUtil

  def fontSizeMap = [
    'text-0-block':'heading-XXL',
    'text-1-block':'heading-XXL',
    'text-2-block':'heading-XL',
    'text-3-block':'heading-L / body-XXL',
    'text-4-block':'heading-M',
    'text-5-block':'heading-S / body-XL',
    'text-6-block':'heading-XXS / body-S',
    'text-7-block':'paragraph']

  def cleanupSrcDom(Document doc, pagePath, pageFile) {

    doc.select('[hide=true]').each {
      println '...remove hidden node: ' + it.tagName()
      it.remove()
    }

    doc.select('[sling:resourceType=beagle/trials/components/hiddenfield]').each {
      it.remove()
    }

    doc.select('[sling:resourceType=beagle/components/wishform],[sling:resourceType^=beagle/trials/components]').each {
      it.remove()
    }

    doc.select('[sling:resourceType=beagle/components/standalonemodal]:not([type])').each {
      it.remove()
    }

    doc.select('[sling:resourceType=beagle/components/text]:not([text])').each {
      println '...remove text node: ' + it.tagName()
      it.remove()
    }

    doc.select('[sling:resourceType=beagle/components/image]:not([fileReference])').each {
      println '...remove image node: ' + it.tagName()
      it.remove()
    }

    //beagle/components/textandimage
    doc.select('[sling:resourceType=beagle/components/textandimage][text]').each {
      def txt = it.attr('text').trim()
      if(txt) {
        it.removeAttr('text')
        def txtX = new Element('txt')
        txtX = commonUtil.clone(txtX)
        txtX.attr('text', txt);
        txtX.attr('jcr:primaryType', 'xx');
        txtX.attr('sling:resourceType', 'beagle/components/text');
        it.prependChild(txtX)
      }
    }

    // see beagle/components/textandimage2col
    doc.select('image:not([fileReference])').each {
      println '...remove image node under: ' + it.parent().tagName()
      it.remove()
    }

    doc.select('[sling:resourceType=beagle/components/table]:not([tableDataHTML])').each {
      println '...remove table node: ' + it.tagName()
      it.remove()
    }

    // when embedded binary file exists
    doc.select('jcr|content *:not([jcr:primaryType])').each { fileNode ->
      throw new Exception('node without primaryType:' + fileNode.tagName())
      def fileNodePath = commonUtil.getNodePath(fileNode)
      def file = new File(pageFile.parentFile, fileNodePath.replace('jcr:content/', '_jcr_content/'))
    }

    doc.select('[sling:resourceType$=/video]>[sling:resourceType$=image][fileReference]').each { authorImg ->
      //authorImg.parent().attr('fileReference', authorImg.attr('fileReference'))
    }

    doc.select('[sling:resourceType=beagle/components/title]').each {
      def title = it.attr('title')?.trim()
      if(!title) {
        it.remove()
      }
    }

    // imagelayer/image[fileReference], remove if fileReference is empty
    doc.select('imagelayer:not(:has(image[fileReference]))').each { it.remove() }

    // no need child under fragment
    doc.select('[fragmentReference][enableFragmentReference=true]').each { it.empty() }

    // step 0: merge image into inlineVideo
    doc.select('[sling:resourceType=beagle/components/inlinevideo]>image[fileReference]').each { image ->
      image.parent().attr('fileReference', image.attr('fileReference'))
      image.parent().attr('alt', image.attr('alt'))
      image.remove()
    }
    // remove empty inlinevideo which has not been configured
    doc.select('[sling:resourceType=beagle/components/inlinevideo]:not(:has(*))').each {
      if(!it.hasAttr('fileReference')) {
        it.remove()
      }
    }

    doc.select('[sling:resourceType=beagle/components/calendar]>intro[text]').each { intro ->
      // move before featuregroup
      // intro.parent().prependChild(intro)
    }

    // check hero component (./image/fileReference,alt,  remove featuredvideo if showFeaturedVideo not true)
    // cleanup hero component
    doc.select('[sling:resourceType=beagle/components/hero/featuredvideo]').each { ele ->
      if('true' != ele.parent().attr('showFeaturedVideo')) {
        ele.remove()
      } else if(ele.hasAttr('creditsAlignment')) {
        ele.parent().attr('creditsAlignment', ele.attr('creditsAlignment'))
      }
    }
    doc.select('[sling:resourceType=beagle/components/hero/credits]').each { ele ->
      if('true' == ele.parent().attr('hideCredits')) {
        ele.remove()
      } else {
        if(ele.hasAttr('creditsAlignment')) {
          ele.parent().attr('creditsAlignment', ele.attr('creditsAlignment'))
        }

        if(ele.hasAttr('width')) {
          ele.parent().attr('pos_width', ele.attr('width'))
        }
      }
    }
    doc.select('[sling:resourceType=beagle/components/hero/link]').each { ele ->
      if('true' == ele.parent().attr('hideLink')) {
        ele.remove()
      }
    }
    doc.select('[sling:resourceType=beagle/components/hero/article]').each { ele ->
      if('true' == ele.parent().attr('hideArticle')) {
        ele.remove()
      } else {
        if(ele.hasAttr('articleAlignment')) {
          ele.parent().attr('articleAlignment', ele.attr('articleAlignment'))
        }
        if(ele.hasAttr('width')) {
          ele.parent().attr('pos_width', ele.attr('width'))
        }
      }
    }
    doc.select('[sling:resourceType=beagle/components/hero]>image[fileReference]').each { image ->
      // merge background image into hero element
      image.parent().attr('fileReference', image.attr('fileReference'))
      image.parent().attr('alt', image.attr('alt'))
      image.remove()
    }
    // remove empty hero which has not been configured
    doc.select('[sling:resourceType=beagle/components/hero]:not(:has(*))').each {
      if(!it.hasAttr('fileReference')) {
        it.remove()
      }
    }
    // remove hero element (article, link, credit) if hide
    doc.select('[sling:resourceType=beagle/components/hero]').each { hero ->
      if('true' == hero.attr('hideArticle')) {
        hero.select('>article').each { it.remove() }
      }

      if('true' == hero.attr('hideLink')) {
        hero.select('links').each { it.remove() }
        hero.select('link').each { it.remove() }
      }

      if('true' == hero.attr('hideCredits')) {
        hero.select('>credits').each { it.remove() }
      }

      hero.select('[hide=true]').each {
        // sling:resourceType=beagle/components/modalcontent/socialsharemobilepage
        it.remove()
      }
    }

    // pre-process sling:resourceType="beagle/components/comparisonchart/chartrow"
    precomparisonchart(doc)

    // step 1: merge imagelayer into column element
    doc.select('[sling:resourceType=beagle/components/parsys/column]>imagelayer>image').each { image ->
      def imagelayer = image.parent()
      def column = imagelayer.parent()
      column.attr('bgImgfile', image.attr('fileReference'))
      imagelayer.remove()
    }

    // step 2.1: update column hierarchy, add pos elements
    recursivePreCol(doc)
    // step 2.2: remove extra column break, column end
    doc.select('[sling:resourceType=beagle/components/parsys/column][columnType]').each { it.remove() }
    // step 2.3:remove empty column
    doc.select('[sling:resourceType=beagle/components/parsys/column]:not(:has(*))').each { it.remove() }

    // hidePersonalizedHero, hideMerchandisingBar, hidePromoComponent
    // step 2.4 after cleanup column, remove empty merchandisingbar which has not been configured
    doc.select('[sling:resourceType=beagle/components/merchandisingbar]>[sling:resourceType=beagle/components/parsys]:not(:has(*))').each { it.remove() }
    doc.select('[sling:resourceType=beagle/components/merchandisingbar]:not(:has(*))').each { it.remove() }
    // remove beagle/components/merchandisingbar when page property 'hideMerchandisingBar=hide
    doc.select('[hideMerchandisingBar=hide]>[sling:resourceType=beagle/components/merchandisingbar]').each { it.remove() }
    // remove nested beagle/components/merchandising
    doc.select('[sling:resourceType=beagle/components/merchandising]>[sling:resourceType=beagle/components/merchandising]').each { it.remove() }
    // remove promobar when page property 'hidePromoComponent=hide
    doc.select('[hidePromoComponent=hide]>promobar').each { it.remove() }


    // step 3: move tophero into contentbody
    doc.select('jcr|root>jcr|content>tophero[sling:resourceType=beagle/components/hero]').each { hero ->
      if(doc.select('jcr|root>jcr|content[marqueeStyle=newMarquee]').size()==1) {

        if(hero.select('>image[fileReference]').size() == 1
            || hero.hasAttr('avoid')
            || hero.hasAttr('clickableMarquee')
            || hero.hasAttr('color')
            || hero.hasAttr('effect')
            || hero.hasAttr('focalBlur')
            || hero.hasAttr('fixedHeight')
            || hero.hasAttr('height')
            || hero.hasAttr('stackMode')
            ) {
          doc.selectFirst('jcr|root>jcr|content>contentbody').prependChild(hero)
        }
      } else {
        hero.remove()
      }
    }

    doc.select('[sling:resourceType=beagle/components/hero/article]>titleImage:not(:has([fileReference]))').each {
      //it.remove()
    }

    // move image into parent
    doc.select('[sling:resourceType=beagle/components/hero/featuredvideo]>[sling:resourceType=beagle/components/image][fileReference]').each { ele ->
      def video = ele.parent()
      video.attr('fileReference', ele.attr('fileReference'))
      video.attr('alt', ele.attr('alt'))
      ele.remove()
    }

    // step 4: hero>(image+link), move youtube to image
    doc.select('[sling:resourceType=beagle/components/image]+[youTubeUrl]').each { link ->
      def image = link.previousElementSibling()

      String modalId = adobeUtil.generateModalId(link.attr('youTubeUrl'))

      image.attr('modalId', modalId)
      image.attr('type', link.attr('type'))
      image.attr('size', link.attr('size'))
      //link.remove()
    }

    // step 5: move merchandisingbar into contentbody
    doc.select('jcr|root>jcr|content>merchandisingbar[sling:resourceType=beagle/components/merchandisingbar]').each { mbar ->
      def contentbody = doc.selectFirst('jcr|root>jcr|content>contentbody[sling:resourceType=beagle/components/parsys]')
      if(contentbody && mbar){
        mbar.tagName('bottom_' + mbar.tagName())
        contentbody.appendChild(mbar)
      }
    }

    // step 6: hideInTablet, hideInDesktop, hideInMobile
    doc.select('[hideInTablet],[hideInDesktop],[hideInMobile]').each { ele ->
      // get/create responsive for element
      def responsive = ele.selectFirst('>cq|responsive')?.remove()
      responsive =  targetUtil.getTgtComponent('cq:responsive')
      ele.appendChild(responsive)

      // add 'hide' to selected device
      ['hideInDesktop':'default', 'hideInTablet':'tablet', 'hideInMobile':'phone'].each { attrx, device ->
        if(ele.hasAttr(attrx) && 'true' == ele.attr(attrx)) {
          responsive.selectFirst(device).attr('behavior', 'hide')
        }
      }
    }

    // last step: remove empty parsys again
    doc.select('[sling:resourceType$=/parsys]:not(:has(*))').each { it.remove() }


  }

  def precomparisonchart(Document doc) {
    doc.select('[sling:resourceType=beagle/components/comparisonchart]').each { chart ->

      // chart compchartheader -> text
      def txt = chart.selectFirst('>compchartheader[text]')
      txt.attr('sling:resourceType','beagle/components/text')
      chart.before(txt)

      def cols = 0
      def headTr = '<tr>'
      chart.select('>*').each { col ->
        if(col.tagName().startsWith('col')) {
          def idx = (col.tagName() - 'col').toInteger()
          cols = cols < idx ? idx : cols

          def text = col.attr('text')
          def ctaButtonText = col.attr('ctaButtonText')
          def ctaButtonOverride = col.attr('ctaButtonOverride')
          headTr += '<td>'

          if(text) {
            def htmlBody = Jsoup.parseBodyFragment(text).body()
            htmlBody.select('>div').each {
              it.tagName('p')
            }
            headTr += htmlBody.html()
          }
          if(ctaButtonText && ctaButtonOverride) {
            // a>span.button for postText
            headTr += "<p style=\"text-align: center;\"><a href=\"${ctaButtonOverride}\"><span class=\"button\">${ctaButtonText}</span></a></p>"
          }
          headTr += '</td>'
        }
      }
      headTr += '</tr>'

      def nextsibling = chart.nextElementSibling()
      def tr = '';
      def tobeDeleted = []
      while(nextsibling != null) {
        if('beagle/components/comparisonchart/chartrow' == nextsibling.attr('sling:resourceType')) {
          def heading = nextsibling.attr('heading')
          def description = nextsibling.attr('description')
          tr += '<tr><td>'
          if(heading) {
            tr += '<p>' + heading + '</p>'
          }
          if(description) {
            tr += '<p>' + description + '</p>'
          }
          tr += '</td>'

          for(int x = 2; x <= cols; x++) {
            if(nextsibling.hasAttr("col${x}AppAvailability") && 'true' == nextsibling.attr("col${x}AppAvailability")) {
              tr += "<td><p style=\"text-align: center\">⚪</p></td>"
            } else {
              tr += "<td>&nbsp;</td>"
            }
          }
          tr += '</tr>'
        } else if('beagle/components/table' == nextsibling.attr('sling:resourceType')) {
          def tableDataHTML = Jsoup.parseBodyFragment(nextsibling.attr('tableDataHTML')).body()
          def widths = []
          tableDataHTML.select('td').each { td ->
            def style = td.attr('style')

            def matcher = style =~ /(width:)[\s]*(\d+%)/
            if(matcher.size() == 1) {
              widths.push(matcher[0][2])
            }
          }
          tableDataHTML.selectFirst('tbody')?.prepend(tr)
          tableDataHTML.selectFirst('tbody')?.prepend(headTr)
          chart.attr('tableDataHTML', tableDataHTML.html())
          chart.attr('widths', widths.toString())
        } else {
          break;
        }

        tobeDeleted.add(nextsibling)
        nextsibling = nextsibling.nextElementSibling()
      }

      // delete siblings
      tobeDeleted.each {
        it.remove()
      }
    }
  }

  // update column into a tree, process one per run
  def recursivePreCol(Document doc) {
    def colStart = doc.selectFirst('[sling:resourceType=beagle/components/parsys/column]:not([columnType]):not([preprocessed])')
    if(colStart == null) {
      return
    }

    def startName = colStart.tagName()
    // collect column siblings till the column-end
    Elements nextsiblings = new Elements()
    def nextsibling = colStart.nextElementSibling()
    while(nextsibling != null) {
      if('end' == nextsibling.attr('columnType')) {
        def correspondingStart = nextsibling.attr('correspondingStart')
        correspondingStart = correspondingStart.replaceAll(' ', '_x0020_')
        if(correspondingStart == startName) {
          nextsibling.remove()
          break
        }
      }
      nextsiblings.add(nextsibling)
      nextsibling = nextsibling.nextElementSibling()
    }

    // insert position component
    def currentPosition = null, posIdx = 0, breakEmpty = true
    nextsiblings.eachWithIndex { item, idx ->
      if('break' == item.attr('columnType') && item.attr('correspondingStart') == startName) {
        //if(breakEmpty == false && idx < nextsiblings.size()-1) {
        currentPosition = colStart.append('<pos></pos>').select('>pos:last-of-type').get(0)
        currentPosition.attr('idx', ''+posIdx++)
        //} else {
        //  breakEmpty = true
        //}
        item.remove()
      } else {
        if(currentPosition == null) {
          currentPosition = colStart.append('<pos></pos>').select('>pos:last-of-type').get(0)
          currentPosition.attr('idx', ''+posIdx++)
        }
        //breakEmpty = false
        currentPosition.appendChild(item)
      }
    }

    // adjustment after
    if(colStart.hasAttr('layout')) {
      def cols = colStart.attr('layout').substring(0,1).toInteger()
      if(cols < colStart.select('>pos').size()) {
        colStart.select('>pos:not(:has(*))').each {
          // remove extra empty pos
          it.remove()
        }
      }
    }

    colStart.attr('preprocessed', 'yes')
    recursivePreCol(doc)
  }

  static def bgColorPat = Pattern.compile('background-color:([^;]+);color:(.*)')
  static HashMap<String, String> readColor(String bgColor) {
    Matcher digitMatcher = bgColorPat.matcher(bgColor)
    while (digitMatcher.find()) {
      def bcolor = digitMatcher.group(1)
      def color = digitMatcher.group(2)
      return ['bcolor':bcolor, 'color':color] as HashMap<String, String>
    }
  }

  static def isCenter(List<Integer[]> parsedWidthOffset) {

    int total = 0, offset0 = 0, len = parsedWidthOffset.size()

    if(len ==1) {
      total = parsedWidthOffset.get(0)[0] + parsedWidthOffset.get(0)[1] * 2
    } else {
      for(int i=0; i<len; i++) {
        Integer[] item = parsedWidthOffset.get(i)

        total += item[0]
        total += item[1]

        if(i==0) {
          // first col
          offset0 = item[1]
        }

        if(i==len-1 && item[1] == 0) {
          // last col without offset
          total += offset0
        }
      }
    }

    return total > 950 && total <1010
  }

  static def actualTotal(List<Integer[]> parsedWidthOffset) {

    int total = 0
    parsedWidthOffset.each { item ->
      total += item[0]
      total += item[1]
    }
    return total
  }

  static def gridSpanPat = Pattern.compile('grid-span-(\\d)of(\\d+)')
  static def gridColPat = Pattern.compile('grid-cols-(\\d+)')
  static def gridOffPat = Pattern.compile('grid-offset-(\\d+)')
  static List<Integer[]> parseWidthOffset(String layout, String basicLayout) {

    if('1;grid-span-0;' == layout) {
      layout = '1;grid-span-8of8;'
    }

    if(layout.contains('cols--')) {
      // layout="2;grid-cols--1      ;grid-cols--1      ;"
      def cols = layout.substring(0,1).toInteger()
      layout = "$cols;" + "grid-span-1of$cols;" * cols
    }

    def layoutArr = layout?.split(';')
    //override by basicLayout if provides same cols
    def cols = layout.substring(0,1).toInteger()
    def basicCols = basicLayout ? basicLayout.substring(0,1).toInteger() : 0
    if(basicCols>=cols) {
      layoutArr = basicLayout?.split(';')
    }

    if(!layoutArr) {
      throw new Exception('Missing layout')
    }

    def matrix = []
    int offset0 = -1
    layoutArr.eachWithIndex { groupx, idx ->
      if(idx>0) {
        groupx = groupx.trim()
        if(groupx.contains('grid-span-')) {
          Matcher digitMatcher = gridSpanPat.matcher(groupx)
          while (digitMatcher.find()) {
            def g1 = digitMatcher.group(1).toInteger()
            def g2 = digitMatcher.group(2).toInteger()
            def width = Math.round(g1/g2*1000)
            matrix.add([width, 0])
          }
        } else if(groupx.contains('grid-cols-')) {
          def width=0, offset=0
          Matcher digitMatcher = gridColPat.matcher(groupx)
          while (digitMatcher.find()) {
            width = digitMatcher.group(1).toInteger()
          }
          digitMatcher = gridOffPat.matcher(groupx)
          while (digitMatcher.find()) {
            offset = digitMatcher.group(1).toInteger()
          }
          matrix.add([width, offset])
          if(idx==1) {
            offset0 = offset
          }
        }
      }
    }

    if(offset0 != -1) {
      // divide again
      int total = 0
      matrix.each { item ->
        total += item[0]
        total += item[1]
      }

      if(total < 16 && offset0 > 0) {
        total += offset0
      }

      def matrix1 = []
      matrix.each { item ->
        def width = Math.round(item[0]/total * 1000)
        def off = Math.round(item[1]/total * 1000)
        matrix1.add([width, off])
      }

      return matrix1
    }

    return matrix
  }

  // calc Width and Offset
  // input ex: 2;grid-cols-6     grid-padded-t2x  grid-padded-b2x ;grid-cols-6     grid-padded-t2x  grid-padded-b2x ;
  // input ex: 2;grid-span-1of5;grid-span-4of5;
  // index starts with 0
  // return ex: ['5','1']

  static HashMap<String, Integer> calcWidthOffset(String layout, int index, String basicLayout) {
    if(!layout || layout == '1;grid-span-0;') {
      // missing layout
      layout = '1;grid-span-9of9;'
    }
    def idx = index + 1
    def layoutArr = layout?.split(';')
    if(layoutArr == null) {
      layoutArr = basicLayout?.split(';')
    }

    if(layoutArr && layoutArr.length>idx) {
      def groupx = layoutArr[idx]
      int cols = layoutArr[0].toInteger()
      if(groupx.contains('grid-span-')) {
        Matcher digitMatcher = gridSpanPat.matcher(groupx)
        while (digitMatcher.find()) {
          def g1 = digitMatcher.group(1).toInteger()
          def g2 = digitMatcher.group(2).toInteger()
          def x = (g1-0.01) * 12/g2
          def width = Math.round(x)
          return ['width':width, 'offset':0] as HashMap<String, Integer>
        }
      } else if(groupx.contains('grid-cols-')) {
        def srcWidthOffsetMapLst = []
        def destWidthOffsetMapLst = []
        for(int i=1; i<layoutArr.length; i++) {
          groupx = layoutArr[i]
          def srcWidth=0, srcOffset=0, destWidth=0, destOffset=0
          Matcher digitMatcher = gridColPat.matcher(groupx)
          while (digitMatcher.find()) {
            srcWidth = digitMatcher.group(1).toInteger()
            def x = (srcWidth-0.001) * 12/16
            destWidth = Math.round(x)
          }
          digitMatcher = gridOffPat.matcher(groupx)
          while (digitMatcher.find()) {
            srcOffset = digitMatcher.group(1).toInteger()
            def x = (srcOffset-0.001) * 12/16
            destOffset = Math.round(x)
          }

          srcWidthOffsetMapLst.add([srcWidth, srcOffset] as LinkedList<Integer>)
          destWidthOffsetMapLst.add([destWidth, destOffset] as LinkedList<Integer>)
        }
        def isCenter = isSrcCentralized(srcWidthOffsetMapLst)
        return getAdjustedWidths(destWidthOffsetMapLst, isCenter, idx)
      }
    }
    throw new Exception('Invalid layout: '+ layout + ', idx=' + idx)
  }

  // adjust to 12 cols
  static HashMap<String, Integer> getAdjustedWidths(List<List<Integer>> widthoffsets, boolean isCenter, int index) {
    def offset0=0, total = 0, width=0, offset=0, cols = widthoffsets.size()

    widthoffsets.eachWithIndex { lst, idx ->
      if(idx==0) {
        offset0 = lst[1]
      }
      total += lst[0]
      total += lst[1]
      if(idx==index-1) {
        width = lst[0]
        offset = lst[1]
      }
    }

    def extra = 12-total-offset0
    def canDivd = extra % cols == 0
    def remain = extra / cols
    if(isCenter && canDivd && extra>0) {
      width += remain
    }

    return ['width':width, 'offset':offset] as HashMap<String, Integer>
  }

  static boolean isSrcCentralized(List<List<Integer>> widthoffsets) {
    def offset0=0, total = 0

    widthoffsets.eachWithIndex { lst, idx ->
      if(idx==0) {
        offset0 = lst[1]
      }
      total += lst[0]
      total += lst[1]
    }
    return 16-total==offset0
  }

  def gridPaddedPat = Pattern.compile('grid-padded-(.\\d+)x')
  def gridLRPadPat = Pattern.compile('text-cell-(.\\d+)x')
  // whitespace ex: grid-padded-v6x (v,t,b)
  // layout ex: 2;grid-cols-7 grid-offset-1  text-start  text-center-tablet  text-end-phone  grid-padded-t1x  grid-padded-b2x  text-cell-4x;grid-cols-8      ;
  def HashMap<String, Integer> calcWhitespace(String whitespace) {

    def paddings = [
      'paddingTop':0,
      'paddingBottom':0,
      'paddingLeft':0,
      'paddingRight':0
    ] as HashMap<String, Integer>

    if(whitespace && whitespace.startsWith('grid-padded-')) {
      def padded = commonUtil.getInteger(whitespace) * 4
      if(whitespace.startsWith('grid-padded-v')) {
        paddings.put('paddingTop', padded)
        paddings.put('paddingBottom', padded)
      } else if(whitespace.startsWith('grid-padded-t')) {
        paddings.put('paddingTop', padded)
      } else if(whitespace.startsWith('grid-padded-b')) {
        paddings.put('paddingBottom', padded)
      }
    }

    return paddings
  }
  def HashMap<String, Integer> calcPadding(String layout, int index) {
    def idx = index + 1
    def paddings = [
      'paddingTop':0,
      'paddingBottom':0,
      'paddingLeft':0,
      'paddingRight':0
    ] as HashMap<String, Integer>

    def layoutArr = layout?.split(';')
    if(layoutArr && layoutArr.length>idx) {
      def groupx = layoutArr[idx]
      Matcher digitMatcher = gridPaddedPat.matcher(groupx)
      while (digitMatcher.find()) {
        def g1 = digitMatcher.group(1)
        def x = commonUtil.getInteger(g1) * 8
        if(g1.startsWith('t')) {
          paddings.put('paddingTop', x)
        } else if(g1.startsWith('b')) {
          paddings.put('paddingBottom', x)
        }
      }

      digitMatcher = gridLRPadPat.matcher(groupx)
      while (digitMatcher.find()) {
        def g1 = digitMatcher.group(1)
        def x = commonUtil.getInteger(g1) * 16
        paddings.put('paddingLeft', x)
        paddings.put('paddingRight', x)
      }
    }
    return paddings
  }

  // fileReference="[/content/dam/acom/en/products/creativecloud/business/enterprise/2d-3d-photoshop/cce-how-to-2d-3d-ps-riverflow1-520x240.png]"
  def getRef1(String fileReference) {
    def ref1 = fileReference.trim().replace('[','').replace(']','')
    if(ref1.contains(',')) {
      ref1 = ref1.split(',')[0]
    }
    return ref1
  }

  def checkException(Document doc) {
    doc.select('[sling:resourceType=beagle/components/features]>intro[text]').each { intro ->
      // throw new Exception('[sling:resourceType=beagle/components/features]>intro not implemented')
    }
  }

  // collect to be migrated pages (assets and fragment)
  HashMap<String, String> findAllReferenceList(final File root_dir, String pageSelector) {

    def refPaths = [:]
    def refAssets = [:]
    def refFrags = [:]
    def assetLst = []
    def fragLst = []

    // scan everything provided
    root_dir.eachFileRecurse (FileType.FILES) { file ->

      if(file.name == '.content.xml') {

        def pagePath = file.absolutePath - '/.content.xml'
        // remove folder path
        pagePath = pagePath.substring(pagePath.indexOf('/jcr_root/') + 9)

        String xml =  commonUtil.file2xml(file)
        Document doc = commonUtil.xml2dom(xml)

        // do nothing
        println file.absolutePath
        checkException(doc)

        if(doc.select(pageSelector).size()==1) {

          refPaths.put(pagePath, 'src_page')

          //gather DamAsset
          doc.select('[fileReference]').each {
            if(it.attr('fileReference')) {
              def srcAsset = it.attr('fileReference')
              srcAsset = getRef1(srcAsset)
              refAssets.put(srcAsset, '')
            }
          }

          doc.select('[sling:resourceType=beagle/components/text][text]').each { ele ->
            def body = Jsoup.parseBodyFragment(ele.attr('text')).body()
            body.select('[href^=/content/dam/]').each { href ->
              def damPath = href.attr('href')
              refAssets.put(damPath, '')
            }
          }

          doc.select('[sling:resourceType=beagle/components/image][link]').each { ele ->
            if(ele.attr('link').startsWith('/content/dam')) {
              def damPath = ele.attr('link')
              refAssets.put(damPath, '')
            }
          }

          //gatherFragment
          doc.select('[fragmentReference][enableFragmentReference]').each { ele ->
            def fragmentReference = ele.attr('fragmentReference')
            if(fragmentReference) {
              def fragmentPath = groovy.xml.XmlUtil.escapeXml(fragmentReference)
              fragmentPath = URLDecoder.decode(fragmentPath)
              refFrags.put(fragmentPath, '')
            }
          }
        } else if(doc.select('[sling:resourceType=beagle/components/fragmentpage]').size()==1) {
          fragLst.add(pagePath)
        }
      } else if(file.parentFile.name == 'renditions') {
        // parent of /_jcr_content/renditions/original
        def assetPath = file.parentFile.parentFile.parentFile.absolutePath
        assetPath = assetPath.substring(assetPath.indexOf('/jcr_root/') + 9)
        assetLst.add(assetPath)
      } else {
        // println file.absolutePath
      }
    }

    // combine both refAssets and assetLst
    assetLst.each {
      if(refAssets.get(it) != null) {
        refPaths.put(it, '_asset')
      } else {
        refPaths.put(it, 'extra_asset')
      }
    }
    // combine both refFrags and fragLst
    fragLst.each {
      if(refFrags.get(it) != null) {
        refPaths.put(it, '_frag')
      } else {
        refPaths.put(it, 'extra_frag')
      }
    }

    return refPaths
  }

  //String path = "/content/acom/us/en/careers/why-adobe/jcr:content/modalcontent/standalonemodal_fa78";
  def String generateModalId(final String nodePath) {

    String mid = "m" + DigestUtils.md5Hex(getEscapedPathForId(nodePath).getBytes(Charset.forName("UTF-8")))
    return mid
  }

  def String getEscapedPathForId(final String path) {
    Pattern PATH_REPLACEMENT_PATTERN = Pattern.compile("[^a-zA-Z0-9]")
    Pattern PATH_MULTIPLE_UNDERSCORE_REPLACEMENT_PATTERN = Pattern.compile("__+")
    String tmp = PATH_REPLACEMENT_PATTERN.matcher(path).replaceAll("_")
    tmp = PATH_MULTIPLE_UNDERSCORE_REPLACEMENT_PATTERN.matcher(tmp).replaceAll("_")
    return tmp
  }
}

return new SrcUtil()
