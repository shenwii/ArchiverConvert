package com.github.shenwii.ArchiverConvert.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
public interface ConvertService {
    /**
     * 转换压缩包
     * @param file 转换前的压缩文件
     * @param to 需要转换的类型
     * @param response 响应
     * @throws IOException
     */
    void convert(MultipartFile file, String to, HttpServletResponse response) throws IOException;
}
