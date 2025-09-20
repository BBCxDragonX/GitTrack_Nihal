package com.dreamboat.LeetCode;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;

public class PDFEditor {
    public static void main(String[] args) {
        String inputPath = "input.pdf";
        String outputPath = "output_edited.pdf";

        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            PDPage page = document.getPage(1); // Edit first page

            // Open content stream to write
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true);

            // Set font and position
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.newLineAtOffset(100, 700); // X=100, Y=700
            contentStream.showText("This is added text!");
            contentStream.endText();

            contentStream.close();

            // Save to new file
            document.save(outputPath);
            System.out.println("PDF edited and saved as: " + outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

