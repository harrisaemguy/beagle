
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Hyperlink
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ReportUtil {

  // lines of "|" separated data
  // manually remove sheet before override
  def printReport(String absFilename, String sheetname, String headline, Collection<String> contentlines) throws Exception {

    println 'create excel file: ' + absFilename
    println 'create excel sheet: ' + sheetname
    if(contentlines.isEmpty()) {
      return
    }

    XSSFWorkbook workbook = null
    XSSFSheet sheet = null
    FileInputStream fileInputStream = null
    if(new File(absFilename).exists()) {
      fileInputStream = new FileInputStream(new File(absFilename))
      workbook = new XSSFWorkbook(fileInputStream)
    } else {
      // Create a Workbook
      workbook = new XSSFWorkbook()
    }

    /*
     * CreationHelper helps us create instances of various things like DataFormat,
     * Hyperlink, RichTextString etc, in a format (HSSF, XSSF) independent way
     */
    CreationHelper createHelper = workbook.getCreationHelper()
    // Create Cell Style for formatting Date
    CellStyle dateCellStyle = workbook.createCellStyle()
    dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"))

    CellStyle intCellStyle = workbook.createCellStyle()
    intCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("###0.0"))

    CellStyle backgroundStyle = workbook.createCellStyle()
    backgroundStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex())
    backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
    XSSFFont font = workbook.createFont()
    font.setBold(true)
    backgroundStyle.setFont(font)

    // remove existing sheet
    sheet = workbook.getSheet(sheetname)
    if(sheet != null) {
      workbook.removeSheetAt(workbook.getSheetIndex(sheet))
    }

    sheet = workbook.createSheet(sheetname)

    // create headline
    Row row = sheet.createRow(0)
    String[] cols = headline.split("\\|")
    for (int j = 0; j < cols.length; j++) {
      Cell cell = row.createCell(j)
      cell.setCellValue(cols[j])
      cell.setCellStyle(backgroundStyle)
    }
    sheet.createFreezePane(0, 1)

    // create contentlines
    int idx = 1
    contentlines.each { contentline ->
      if(contentline) {
        row = sheet.createRow(idx++)
        cols = contentline.split("\\|")
        for (int j = 0; j < cols.length; j++) {
          Cell cell = row.createCell(j)
          if(cols[j].matches('^[0-9]+$')) {
            cell.setCellValue(Long.parseLong(cols[j]))
          } else {
            cell.setCellValue(cols[j])

            if(cols[j].matches('(?ui)^(http|https)(:\\/\\/.+)$')) {

              try {
                Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL)
                link.setAddress(URLEncoder.encode(cols[j], 'UTF-8'))
                cell.setHyperlink(link)
              } catch (MalformedURLException malformedURLException) {
              }
            }
          }
        }
      }
    }

    // Resize all columns to fit the content size
    int len = headline.split("\\|").length
    for (int i = 0; i < len; i++) {
      sheet.autoSizeColumn(i)
    }

    // Write the output to a file
    FileOutputStream fileOutputStream = new FileOutputStream(absFilename)
    workbook.write(fileOutputStream)
    fileOutputStream.flush()
    fileOutputStream.close()
    // Closing the workbook


    if(fileInputStream != null) {
      fileInputStream.close()
    }
  }
}

return new ReportUtil()
