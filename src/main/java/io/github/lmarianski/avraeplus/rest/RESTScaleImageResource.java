package io.github.lmarianski.avraeplus.rest;

import com.google.common.graph.Graph;
import javafx.scene.transform.Scale;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Path("/scale")
@Produces("image/png")
public class RESTScaleImageResource {

    public RESTScaleImageResource() {
    }

    @GET
    public BufferedImage scaleImage(@QueryParam("s") Optional<String> size, @QueryParam("i") Optional<String> src) {
//        byte[] data;
        AtomicReference<Image> image = new AtomicReference<>();
        src.ifPresent(iSrc -> {
            try {
//                InputStream inputStream = new URL(i).openStream();

                image.set(ImageIO.read(GET(new URL(iSrc))));

                size.ifPresent(s -> {
                    int w = image.get().getWidth(null), h = image.get().getHeight(null);
                    if (s.contains("x")) {
                        String[] tmp = s.split("x");
                        w = Integer.parseInt(tmp[0]);
                        h = Integer.parseInt(tmp[1]);
                    }

                    int finalW = w, finalH = h;
                    image.getAndUpdate(img -> img.getScaledInstance(finalW, finalH, Image.SCALE_SMOOTH));
                });


            } catch (IOException e) {
                e.printStackTrace();
            }
        });
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        ImageIO.write(image.get(), "png", output);

//        return output.toByteArray();
        return toBufImag(image.get());
    }

    public static ByteArrayOutputStream toBytes(Image img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(toBufImag(img), "png", baos);
        return baos;
    }

    public static BufferedImage toBufImag(Image img) {
        if (img instanceof BufferedImage)
            return (BufferedImage) img;

        int w = img.getWidth(null), h = img.getHeight(null);
        final BufferedImage newImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g = newImg.createGraphics();

        g.drawImage(img, null, null);
        g.dispose();

        return newImg;
    }


    public static InputStream GET(URL url) throws IOException {
        List<String> opt = Arrays.asList("curl", url.toString(), "-o", "-");
//        opt.addAll(Arrays.asList(opts));

        Process p = new ProcessBuilder(
                opt
        ).start();

        return p.getInputStream();
    }


}
