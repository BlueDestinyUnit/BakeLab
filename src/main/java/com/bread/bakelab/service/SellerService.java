package com.bread.bakelab.service;

import com.bread.bakelab.config.properties.FilePathProperties;
import com.bread.bakelab.domains.dto.ImagesDTO;
import com.bread.bakelab.domains.dto.ProductDTO;
import com.bread.bakelab.domains.vo.ImagesVO;
import com.bread.bakelab.domains.vo.ProductRegisVO;
import com.bread.bakelab.domains.vo.ProductVO;
import com.bread.bakelab.domains.vo.StockVO;
import com.bread.bakelab.mapper.SellerMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Log4j2
@Service
public class SellerService {
    private final FilePathProperties filePathProperties;

    private String SAVE_PATH;

    private String IMAGE_FILE_PATH;

    @Autowired
    private SellerMapper sellerMapper;
    @Autowired
    private ProductService productService;

    @Autowired
    public SellerService(FilePathProperties filePathProperties) {
        this.filePathProperties = filePathProperties;
        this.IMAGE_FILE_PATH = filePathProperties.getImageFilePath();
    }

    // 이미지 저장
    public boolean insert_image(ImagesDTO imagesDTO,String product_name){
        log.info(imagesDTO);

        List<MultipartFile> files = imagesDTO.getImages();
        List<ImagesVO> imagesVOS;
        // 파일 형식 체크
        if(!check_files(files)) return false;
        // 파일을 로컬에 저장 + 이미지 리스트 받아오기
        try {
            imagesVOS = save_files(files);
        }catch (IOException e){
            return false;
        }

        log.info(imagesVOS);
        // DB에 저장 - bread_images 테이블에 insert
        for (ImagesVO image : imagesVOS) {
            image.setProduct_name(product_name); // product_name 설정
            log.info(image.getProduct_name());
        }
        log.info(imagesVOS);
        sellerMapper.post_images(imagesVOS);
        return true;
    }

    public void update_product(ProductRegisVO productRegisVO){
        ProductVO productVO = new ProductVO.Builder()
                .withProductName(productRegisVO.getProduct_name())
                .withPrice(productRegisVO.getPrice())
                .withContext(productRegisVO.getContext())
                .withNutrition(productRegisVO.getNutrition())
                .withAllergy(productRegisVO.getAllergy())
                .withCategory(productRegisVO.getCategory())
                .withStock(productRegisVO.getStock())
                .build();
        productService.update_product(productVO);
        if(productRegisVO.getImages().get(0).isEmpty() == false){
            String product_name = productVO.getProduct_name();
            ProductDTO productDTO = productService.get_product(product_name);
            List<ImagesVO> imagesVOS = productDTO.getImagesVO();
            imagesVOS.forEach( imagesVO -> {
                String FILE_PATH = IMAGE_FILE_PATH + imagesVO.getImage();
                File file = new File(FILE_PATH);
                boolean deleteFile = file.delete();
            });
            // ProductRegisVO에 필요한 정보 설정
            ImagesDTO imagesDTO = new ImagesDTO();
            imagesDTO.setImages(productRegisVO.getImages());
            productService.delete_images(product_name);
            insert_image(imagesDTO,product_name);
        }
    }

    // 게시물 작성하기
    public boolean insert_product(ProductRegisVO productRegisVO){
        log.info(productRegisVO);

        List<MultipartFile> files = productRegisVO.getImages();
        List<ImagesVO> imagesVOS;
        // 파일 형식 체크
        if(!check_files(files)) return false;
        // 파일을 로컬에 저장 + 이미지 리스트 받아오기
        try {
            imagesVOS = save_files(files);
        }catch (IOException e){
            return false;
        }

        log.info(imagesVOS);

        // DB에 저장 - bread_list insert
        sellerMapper.post_product(productRegisVO);
        sellerMapper.post_stock(productRegisVO.getProduct_name());

        // DB에 저장 - bread_images 테이블에 insert
        for (ImagesVO image : imagesVOS) {
            image.setProduct_name(productRegisVO.getProduct_name()); // product_name 설정
            log.info(image.getProduct_name());
        }
        log.info(imagesVOS);
        sellerMapper.post_images(imagesVOS);


        return true;
    }

    // 파일 형식 체크
    private boolean check_files(List<MultipartFile> files){

        if (files == null) {
            log.info("Files list is null.");
            return false;
        }

        log.info(files);

        for (MultipartFile file: files) {
            String contentType = file.getContentType();
            if(!contentType.startsWith("image/")){
                return false;
            }
        }
        return true;
    }

    // 파일 저장 후 이름들을 저장하는 LIST 받아옴
    private List<ImagesVO> save_files(List<MultipartFile> files) throws IOException {
        // 이미지들 객체 LIST
        List<ImagesVO> imagesVOS = new ArrayList<>();
        // 모든 파일을 순회하면서 이미지 명을 설정하고 저장, DB에 저장할 객체도 생성
        for (MultipartFile file: files) {
            // 파일에서 원본 이미지 파일명을 토대로 새 이름을 결정
            String originalFileName = file.getOriginalFilename();
            String saveFileName = UUID.randomUUID() + "_" + originalFileName;
            File saveFile = new File(SAVE_PATH, saveFileName);
            // 로컬에 파일 저장
            file.transferTo(saveFile);
            // DB에 저장할 파일 객체 생성 후 이미지명 설정
            ImagesVO imagesVO = new ImagesVO();
            imagesVO.setImage(saveFileName);

            // 리스트로 넣음
            imagesVOS.add(imagesVO);
        }
        // 이미지들 객체 LIST 반환
        return  imagesVOS;
    }

    // 판매수 업데이트
    public void update_sell_stock(int count,String product_name){
        sellerMapper.update_sell_stock(count,product_name);
    }

    // 판매된 수 목록
    public List<StockVO> find_stock(){
        return sellerMapper.find_stock();
    }


}
