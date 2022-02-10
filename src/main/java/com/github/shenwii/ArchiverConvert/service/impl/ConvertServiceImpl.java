package com.github.shenwii.ArchiverConvert.service.impl;

import com.github.shenwii.ArchiverConvert.dto.ArchiverCommandDto;
import com.github.shenwii.ArchiverConvert.service.ConvertService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;

@Service
@Configuration
@ConfigurationProperties(prefix = "convert")
public class ConvertServiceImpl implements ConvertService {
    private Map<String, ArchiverCommandDto> archiverCommandMap;

    public void setArchiverCommandMap(Map<String, ArchiverCommandDto> archiverCommandMap) {
        this.archiverCommandMap = archiverCommandMap;
    }

    /**
     * 递归删除文件夹
     * @param dir 要删除的文件夹
     * @return 删除是否成功
     * @throws IOException
     */
    private boolean deleteDir(File dir) throws IOException {
        for(File f: dir.listFiles()) {
            if(f.isFile()) {
                if(!f.delete())
                    return false;
            } else {
                deleteDir(f);
            }
        }
        if(!dir.delete())
            return false;
        return true;
    }

    /**
     * 获取文件的后缀
     * @param fileName 文件名
     * @param depth 后缀深度
     * @return
     */
    private String getFileExtensions(String fileName, int depth) {
        String[] splitArray = fileName.split("\\.");
        int start = splitArray.length - depth;
        if(start < 1) start = 1;
        StringBuilder sb = new StringBuilder();
        for(int i = start; i < splitArray.length; i++)
            sb.append("." + splitArray[i]);
        return sb.toString();
    }

    /**
     * 获取文件的后缀
     * @param fileName 文件名
     * @return
     */
    private String getFileExtensions(String fileName) {
        return getFileExtensions(fileName, 1);
    }

    @Override
    public void convert(MultipartFile file, String to, HttpServletResponse response) throws IOException {
        //获取原始文件名
        String originalFilename = file.getOriginalFilename();
        //获取需转换格式的命令DTO
        ArchiverCommandDto convertArchiverCommandDto = archiverCommandMap.get(to);
        //如果不存在需转换格式的命令DTO则报错
        if(convertArchiverCommandDto == null) {
            response.setStatus(430);
            response.getOutputStream().write("转换格式不支持".getBytes("UTF-8"));
            return;
        }
        ArchiverCommandDto archiverCommandDto = null;
        //首先获取深度为2的后缀，比如.tar.gz这种
        String fileExtensions = getFileExtensions(originalFilename, 2);
        //获取当前格式的命令DTO
        archiverCommandDto = archiverCommandMap.get(fileExtensions);
        if(archiverCommandDto == null) {
            //如果没有的话，再获取深度为1的后缀，比如.rar这种
            fileExtensions = getFileExtensions(originalFilename);
            //再次获取当前格式的命令DTO
            archiverCommandDto = archiverCommandMap.get(fileExtensions);
        }
        //如果不存在当前格式的命令DTO则报错
        if(archiverCommandDto == null) {
            response.setStatus(430);
            response.getOutputStream().write("转换格式不支持".getBytes("UTF-8"));
            return;
        }
        //创建一个临时文件夹
        File tmpDir = Files.createTempDirectory("").toFile();
        //原始压缩包文件
        File archiverFile = new File(tmpDir, originalFilename);
        //转换后的压缩包文件
        File convertArchiverFile = new File(tmpDir, originalFilename.substring(0, originalFilename.length() - fileExtensions.length()) + to);
        //保存到原始压缩包文件
        file.transferTo(archiverFile);
        //定义一个works的文件夹
        File workDir = new File(tmpDir, "works");
        //创建works文件夹，用于存放解压后的文件
        workDir.mkdir();
        Process process = null;
        //判断系统，来执行不同的shell
        //执行解压命令
        if(System.getProperties().getProperty("os.name").contains("windows")) {
            process = Runtime.getRuntime().exec(new String[] {"cmd", "/c", archiverCommandDto.getUnpackCommand().replace("$archive_name", "\"" + archiverFile.getAbsolutePath() + "\"")}, null, workDir);
        } else {
            process = Runtime.getRuntime().exec(new String[] {"sh", "-c", archiverCommandDto.getUnpackCommand().replace("$archive_name", "\"" + archiverFile.getAbsolutePath() + "\"")}, null, workDir);
        }
        process.getInputStream().transferTo(System.out);
        process.getErrorStream().transferTo(System.err);
        int returnCode = 1;
        try {
            returnCode = process.waitFor();
        } catch (InterruptedException ignore) {}
        //判断命令是否执行成功
        if(returnCode != 0) {
            response.setStatus(430);
            response.getOutputStream().write("解压失败".getBytes("UTF-8"));
            deleteDir(tmpDir);
            return;
        }
        //判断系统，来执行不同的shell
        //执行压缩命令
        if(System.getProperties().getProperty("os.name").contains("windows")) {
            process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", convertArchiverCommandDto.getPackCommand().replace("$archive_name", "\"" + convertArchiverFile.getAbsolutePath() + "\"").replace("$file_names", "*")}, null, workDir);
        } else {
            process = Runtime.getRuntime().exec(new String[]{"sh", "-c", convertArchiverCommandDto.getPackCommand().replace("$archive_name", "\"" + convertArchiverFile.getAbsolutePath() + "\"").replace("$file_names", "*")}, null, workDir);
        }
        process.getInputStream().transferTo(System.out);
        process.getErrorStream().transferTo(System.err);
        returnCode = 1;
        try {
            returnCode = process.waitFor();
        } catch (InterruptedException ignore) {}
        //判断命令是否执行成功
        if(returnCode != 0) {
            response.setStatus(430);
            response.getOutputStream().write("压缩失败".getBytes("UTF-8"));
            deleteDir(tmpDir);
            return;
        }
        //将转换后的压缩文件，写入到流给用户下载
        try(FileInputStream fileInputStream = new FileInputStream(convertArchiverFile)) {
            response.setStatus(200);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(convertArchiverFile.getName(), "utf-8"));
            response.setHeader("Content-Length", String.valueOf(fileInputStream.available()));
            byte[] buf = new byte[40960];
            int read;
            while((read = fileInputStream.read(buf)) > 0) {
                response.getOutputStream().write(buf, 0, read);
            }
        } finally {
            deleteDir(tmpDir);
        }
    }
}
