package com.example.resourceservice.service;

import com.example.resourceservice.dto.SongMetadataDto;
import com.example.resourceservice.exception.InvalidMp3Exception;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
public class Mp3MetadataExtractor {

    private static final String AUDIO_MPEG = "audio/mpeg";

    private final Tika tika = new Tika();

    public boolean isValidMp3(byte[] data) {
        try {
            return AUDIO_MPEG.equals(tika.detect(data));
        } catch (Exception e) {
            return false;
        }
    }

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
            throw new InvalidMp3Exception("Failed to extract MP3 metadata");
        }
    }

    private String parseDuration(String rawDuration) {
        if (rawDuration == null) return null;
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
