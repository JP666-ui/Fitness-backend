package com.fitness.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class BaiduDishUtil {

    @Value("${baidu.ocr.access-key}")
    private String apiKey;
    @Value("${baidu.ocr.secret-key}")
    private String secretKey;
    @Value("${baidu.ocr.dish-url:https://aip.baidubce.com/rest/2.0/image-classify/v2/dish}")
    private String dishUrl;

    // 使用Spring注入的RestTemplate
    private final RestTemplate restTemplate;
    public BaiduDishUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String accessToken;
    private long expireTime = 0;

    /**
     * 获取百度access_token
     */
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < expireTime) {
            return accessToken;
        }
        String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;
        String result = restTemplate.getForObject(tokenUrl, String.class);
        JSONObject json = JSON.parseObject(result);
        accessToken = json.getString("access_token");
        long expiresIn = json.getLongValue("expires_in");
        // 提前60秒过期
        expireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000;
        return accessToken;
    }

    /**
     * 菜品识别
     * @param imgBase64 前端传来带data:image/jpeg;base64,前缀
     * @return Map foodName,calorie,confidence
     */
    public Map<String, Object> recognizeDish(String imgBase64) {
        String base64Pure = imgBase64.split(",")[1];
        byte[] bytes = Base64.getDecoder().decode(base64Pure);
        if (bytes.length > 4 * 1024 * 1024) {
            throw new RuntimeException("图片不能大于4M");
        }
        String token = getAccessToken();
        String url = dishUrl + "?access_token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("image", base64Pure);
        bodyJson.put("top_num",1);
        bodyJson.put("filter_threshold",0.7);

        HttpEntity<String> request = new HttpEntity<>(bodyJson.toString(), headers);
        String respStr = restTemplate.postForObject(url, request, String.class);
        JSONObject respJson = JSON.parseObject(respStr);
        log.info("百度菜品识别返回:{}", respStr);
        if (respJson.containsKey("error_code")) {
            throw new RuntimeException("百度识别失败:" + respJson.getString("error_msg"));
        }
        JSONObject resultItem = respJson.getJSONArray("result").getJSONObject(0);
        String foodName = resultItem.getString("name");
        Integer calorie = resultItem.getInteger("calorie");
        Double confidence = resultItem.getDouble("probability");
        return Map.of(
                "foodName", foodName,
                "calorie", calorie,
                "confidence", confidence
        );
    }
}