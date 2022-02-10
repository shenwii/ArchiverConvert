package com.github.shenwii.ArchiverConvert.dto;

public class ArchiverCommandDto {
    /** 压缩命令 */
    private String packCommand;
    /** 解压命令 */
    private String unpackCommand;

    public String getPackCommand() {
        return packCommand;
    }

    public void setPackCommand(String packCommand) {
        this.packCommand = packCommand;
    }

    public String getUnpackCommand() {
        return unpackCommand;
    }

    public void setUnpackCommand(String unpackCommand) {
        this.unpackCommand = unpackCommand;
    }
}
