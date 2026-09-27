package com.example.loot.Security;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class ImageValidation {
    private ImageValidation() {}
    public static void validate(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an image up to 5 MB");
        try {
            byte[] b; try(var stream=image.getInputStream()) { b=stream.readNBytes(32); }
            boolean png=b.length>=24 && Arrays.equals(Arrays.copyOf(b,8), new byte[]{(byte)137,80,78,71,13,10,26,10});
            boolean jpg=b.length>=3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255;
            boolean webp=b.length>=16 && new String(b,0,4,StandardCharsets.US_ASCII).equals("RIFF") && new String(b,8,4,StandardCharsets.US_ASCII).equals("WEBP") && new String(b,12,3,StandardCharsets.US_ASCII).equals("VP8");
            if (!(png && "image/png".equals(image.getContentType()) || jpg && "image/jpeg".equals(image.getContentType()) || webp && "image/webp".equals(image.getContentType())))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image must be a JPEG, PNG or WEBP file");
        } catch(IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Could not read image"); }
    }
}
