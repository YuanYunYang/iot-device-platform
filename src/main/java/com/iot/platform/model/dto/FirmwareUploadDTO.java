package com.iot.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 固件上传 DTO
 * <p>
 * 描述固件包元信息，由前端在上传固件文件后提交。
 * 实际文件建议先上传至对象存储，再将返回的下载地址与校验码填入本 DTO。
 *
 * @author iot-platform
 */
@Data
public class FirmwareUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联产品ID */
    private Long productId;

    /** 固件版本号 */
    @NotBlank(message = "固件版本号不能为空")
    private String version;

    /** 固件文件下载地址 */
    @NotBlank(message = "固件文件下载地址不能为空")
    private String fileUrl;

    /** 固件文件大小（字节） */
    private Long fileSize;

    /** MD5 校验码 */
    private String checksumMd5;

    /** SHA256 校验码 */
    private String checksumSha256;

    /** 固件描述 */
    private String description;
}
