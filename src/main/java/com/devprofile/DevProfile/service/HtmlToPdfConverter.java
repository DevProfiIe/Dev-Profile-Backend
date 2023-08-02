package com.devprofile.DevProfile.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class HtmlToPdfConverter {

    @Value("${download.folder}")
    private String downloadFolder;

    public List<String> convertHtmlToPdf(String userName, List<String> htmlPaths) {
        String userDownloadFolder = downloadFolder + File.separator + userName;
        File userFolder = new File(userDownloadFolder);
        List<String> pdfUrls = new ArrayList<>();
        try {
            if (!userFolder.exists()) {
                boolean success = userFolder.mkdir();
                if (success) {
                    userFolder.setWritable(true, false);
                } else {
                    throw new IOException("Failed to create directory " + userDownloadFolder);
                }
            }

            for (String htmlPath : htmlPaths) {
                pdfUrls.add(convertSingleHtmlToPdf(userDownloadFolder, htmlPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pdfUrls;
    }

    private String convertSingleHtmlToPdf(String userDownloadFolder, String htmlPath) throws IOException {
        File htmlFile = new File(htmlPath);
        String pdfName = htmlFile.getName().replace(".html", ".pdf");
        String pdfPath = userDownloadFolder + File.separator + pdfName;

        PdfWriter writer = new PdfWriter(pdfPath);
        PdfDocument pdfDocument = new PdfDocument(writer);

        ConverterProperties properties = new ConverterProperties();
        HtmlConverter.convertToPdf(new FileInputStream(htmlFile), pdfDocument, properties);
        pdfDocument.close();

        return "http://devprofile.store/downloads" + pdfPath.split(downloadFolder)[1].replace(File.separator, "/");
    }
}