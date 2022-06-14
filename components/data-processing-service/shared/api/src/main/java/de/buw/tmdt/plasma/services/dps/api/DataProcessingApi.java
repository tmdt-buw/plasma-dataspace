package de.buw.tmdt.plasma.services.dps.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RequestMapping(value = "/api/files")
public interface DataProcessingApi {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    SampleDTO uploadFile(@Nullable @RequestParam(value = "dataId", required = false) String dataId, @NotNull @RequestParam("file") MultipartFile file);

    @GetMapping(value = "/{dataId}", produces = MediaType.APPLICATION_JSON_VALUE)
    List<String> listFiles(@NotNull @PathVariable("dataId") String dataId);

}
