package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.FileUpload;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface FileUploadMapper {

    @Insert("insert into file_upload (biz_type, patient_id, uploader_user_id, original_name, file_name, file_ext, mime_type, file_size, storage_path, url, sha1, status, created_at, updated_at, ext1, ext2, ext3, ext4, ext5) values (#{bizType}, #{patientId}, #{uploaderUserId}, #{originalName}, #{fileName}, #{fileExt}, #{mimeType}, #{fileSize}, #{storagePath}, #{url}, #{sha1}, #{status}, now(), now(), #{ext1}, #{ext2}, #{ext3}, #{ext4}, #{ext5})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileUpload f);
}
