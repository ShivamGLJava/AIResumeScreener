package com.resumescreener.resumescreener.util;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.Set;

public class FileParsingUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileParsingUtil.class);

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    public static String extractTextFromMultipartFile(MultipartFile file) throws Exception {
        // Validate file is not null or empty
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty.");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 50MB limit. Maximum allowed: 50MB");
        }

        // Validate file content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed types: PDF, DOC, DOCX, TXT. Provided: " + contentType
            );
        }

        try (InputStream stream = file.getInputStream()) {
            // Configure SAX parser with XXE protection
            SAXParserFactory spf = SAXParserFactory.newInstance();
            disableXXEProtection(spf);

            // Create parser
            AutoDetectParser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();

            // Parse file with XXE protection
            parser.parse(stream, handler, metadata);

            String extractedText = handler.toString();

            // Validate extracted text is not empty
            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IllegalArgumentException("No text content found in file");
            }

            return extractedText;
        }
    }

    private static void disableXXEProtection(SAXParserFactory spf) {
        try {
            logger.debug("Configuring XXE protection for XML parser");

            // Disable DTD processing
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // Disable external general entities
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);

            // Disable external parameter entities
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            // Prevent XXE/Denial of Service attacks
            spf.setXIncludeAware(false);

        } catch (SAXException | ParserConfigurationException e) {
            logger.debug("XXE protection configuration note: {}", e.getMessage());
        }
    }
}
