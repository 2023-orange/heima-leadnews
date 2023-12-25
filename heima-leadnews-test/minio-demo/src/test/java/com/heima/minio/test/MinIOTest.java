package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import com.heima.minio.MinIOApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinIOTest {

    @Autowired
    private FileStorageService fileStorageService;

//    /**
//     * 把list.html文件上传到minio,并且可以在浏览器访问
//     */
//    @Test
//    public void test() throws FileNotFoundException {
//        FileInputStream fileInputStream = new FileInputStream("D:/Desktop/leadnews/list.html");
//        String path = fileStorageService.uploadHtmlFile("", "list.html", fileInputStream);
//        System.out.println(path);
//    }

    /**
     * 把list.html文件上传到minio,并且可以在浏览器访问
     * @param args
     */
    public static void main(String[] args) {

        FileInputStream fileInputStream = null;
        try {

            fileInputStream = new FileInputStream("D:/Desktop/leadnews/资料/模板文件/plugins/js/index.js");

            //1.创建minio链接客户端
            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123")
                    .endpoint("http://192.168.200.130:9000").build();
            //2.上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("plugins/js/index.js")//文件名
                    .contentType("text/js")//文件类型
                    .bucket("leadnewsliuxing")//桶名词  与minio创建的名词一致
                    .stream(fileInputStream, fileInputStream.available(), -1) //文件流
                    .build();
            minioClient.putObject(putObjectArgs);

//            System.out.println("http://192.168.200.130:9000/leadnewsliuxing/list.html");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
