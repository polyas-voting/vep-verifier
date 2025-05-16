package de.polyas.core3.vep.verif.input

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

fun File.pdfToText(): String {
    val doc = Loader.loadPDF(this)
    val t = PDFTextStripper().getText(doc)
    doc.close()
    return t
}
