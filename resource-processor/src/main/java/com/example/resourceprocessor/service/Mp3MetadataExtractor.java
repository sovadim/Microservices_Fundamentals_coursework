package com.example.resourceprocessor.service;

import com.example.resourceprocessor.dto.SongMetadataDto;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class Mp3MetadataExtractor {

    public SongMetadataDto extract(byte[] data) {
        try {
            var parser = new AutoDetectParser();
            var handler = new BodyContentHandler(-1);
            var metadata = new Metadata();
            var context = new ParseContext();

            try (var is = new ByteArrayInputStream(data)) {
                parser.parse(is, handler, metadata, context);
            }

            var dto = new SongMetadataDto();
            dto.setName(metadata.get("dc:title"));
            dto.setArtist(metadata.get("xmpDM:artist"));
            dto.setAlbum(metadata.get("xmpDM:album"));
            dto.setDuration(parseDuration(metadata.get("xmpDM:duration")));
            dto.setYear(parseYear(metadata.get("xmpDM:releaseDate")));
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract MP3 metadata", e);
        }
    }

    private String parseDuration(String rawDuration) {
        if (rawDuration == null) {
            return null;
        }
        try {
            double seconds = Double.parseDouble(rawDuration);
            long total = Math.round(seconds);
            return String.format("%02d:%02d", total / 60, total % 60);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseYear(String rawYear) {
        if (rawYear == null) {
            return null;
        }
        return rawYear.length() > 4 ? rawYear.substring(0, 4) : rawYear;
    }
}
