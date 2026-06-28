package org.mymarketapp.reactive.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ImageController {

    @GetMapping("/images/{filename:.+}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable String filename) {
        Resource resource = new ClassPathResource("static/images/" + filename);
        String contentType = filename.endsWith(".svg") ? "image/svg+xml" : MediaType.IMAGE_JPEG_VALUE;
        return Mono.fromCallable(resource::exists)
                .subscribeOn(Schedulers.boundedElastic())
                .map(exists -> exists
                        ? ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .body(resource)
                        : ResponseEntity.notFound().build());
    }
}
