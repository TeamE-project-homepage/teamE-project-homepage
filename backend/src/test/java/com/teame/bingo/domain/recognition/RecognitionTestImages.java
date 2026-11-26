package com.teame.bingo.domain.recognition;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class RecognitionTestImages {

    private RecognitionTestImages() {
    }

    public static byte[] image(String format) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            if (!ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output)) {
                throw new IllegalArgumentException("No image writer for " + format);
            }
        }
        return bytes.toByteArray();
    }
}
