package com.vinhtt.PDFReader.service.pdf;

import com.vinhtt.PDFReader.model.Paragraph;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public interface IPdfService {
    List<Paragraph> parsePdf(File file) throws Exception;
    List<BufferedImage> renderPdfPages(File file) throws Exception; // New method
    String calculateFileHash(File file);
}