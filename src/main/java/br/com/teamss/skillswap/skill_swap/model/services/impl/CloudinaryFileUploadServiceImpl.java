package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryFileUploadServiceImpl implements FileUploadService {

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Faz o upload para o Cloudinary
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        // Retorna a URL segura do arquivo
        return uploadResult.get("secure_url").toString();
    }
}