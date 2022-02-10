package com.github.shenwii.ArchiverConvert.controller;

import com.github.shenwii.ArchiverConvert.service.ConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@RestController
public class ConvertController {
    @Autowired
    private ConvertService convertService;

    @PostMapping("convert")
    public void convert(MultipartFile file, String to, HttpServletResponse response) throws IOException {
        Objects.requireNonNull(file);
        //to不传则默认zip
        if(to == null)
            to = ".zip";
        convertService.convert(file, to, response);
    }
}
