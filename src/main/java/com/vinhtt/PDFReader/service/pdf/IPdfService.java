package com.vinhtt.PDFReader.service.pdf;

import com.vinhtt.PDFReader.model.Paragraph;
import java.io.File;
import java.util.List;

public interface IPdfService {
    List<Paragraph> parsePdf(File file) throws Exception;
    String calculateFileHash(File file);
}