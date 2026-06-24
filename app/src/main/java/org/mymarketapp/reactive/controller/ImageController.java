package org.mymarketapp.reactive.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
public class ImageController {

    @GetMapping("/images/{filename:.+}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable String filename) {
        Resource resource = new ClassPathResource("static/images/" + filename);
        if (!resource.exists()) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        String contentType = filename.endsWith(".svg") ? "image/svg+xml" : MediaType.IMAGE_JPEG_VALUE;
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource));
    }
}
