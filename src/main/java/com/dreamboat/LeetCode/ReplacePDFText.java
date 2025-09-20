package com.dreamboat.LeetCode;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.File;
import java.io.IOException;

public class ReplacePDFText {
    public static void main(String[] args) {
        String inputPath = "input.pdf";
        String outputPath = "output_replaced.pdf";

        // Define the text you want to replace and the new text
        String textToReplace = "OldText";
        String newText = "NewText";

        // Define coordinates manually (x, y). You must find them from PDF manually or visually.
        float x = 100;
        float y = 700;

        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            PDPage page = document.getPage(0);
            PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true
            );

            // Draw white rectangle to "erase" the old text
            contentStream.setNonStrokingColor(1.0f); // white
            contentStream.addRect(x, y - 5, 100, 15); // (x, y-offset, width, height)
            contentStream.fill();

            // Write new text in place
            contentStream.beginText();
            contentStream.setNonStrokingColor(0f); // black
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(newText);
            contentStream.endText();

            contentStream.close();

            document.save(outputPath);
            System.out.println("Replaced text and saved to: " + outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
