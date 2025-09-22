#include "esp_camera.h"
#include "WiFi.h"
#include "HTTPClient.h"

// --- 🔧 사용자가 수정해야 할 부분 ---
const char* ssid = "makerland";
const char* password = "24132413";
const char* server_url = "http://192.168.0.116:8080/api/photos";
const char* deviceSerialNumber = "111-222-333";
// ------------------------------------

// ESP32-CAM 핀 설정 (AI-THINKER 모델 기준)
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

// 카메라 초기화 함수
bool initCamera() {
  Serial.println("카메라 초기화 시작...");
 
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = FRAMESIZE_VGA;
  config.jpeg_quality = 20;
  config.fb_count = 1;

  // 카메라 초기화
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("❌ 카메라 초기화 실패, 오류 코드: 0x%x\n", err);
    return false;
  }
  Serial.println("✅ 카메라 초기화 성공!");
 
  // ⭐⭐⭐ 180도 회전 설정 ⭐⭐⭐
  sensor_t * s = esp_camera_sensor_get();
  s->set_vflip(s, 1);   // 상하 반전
  s->set_hmirror(s, 1); // 좌우 반전
  Serial.println("✅ 카메라 180도 회전 설정 완료");
  
  return true;
}

// 📤 서버로 사진 업로드 함수
bool uploadPhoto() {
  Serial.println("📤 서버 업로드 시작...");
  
  camera_fb_t * fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("❌ 사진 촬영 실패!");
    return false;
  }
  
  Serial.printf("📷 사진 촬영 완료: %u bytes\n", fb->len);
  
  HTTPClient http;
  http.begin(server_url);
  http.setTimeout(15000);
  
  String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
  String formDataStart = "";
  
  // 디바이스 시리얼 넘버 필드
  formDataStart += "--" + boundary + "\r\n";
  formDataStart += "Content-Disposition: form-data; name=\"serialNumber\"\r\n\r\n";
  formDataStart += deviceSerialNumber;
  formDataStart += "\r\n";
  
  // 이미지 파일 헤더
  formDataStart += "--" + boundary + "\r\n";
  formDataStart += "Content-Disposition: form-data; name=\"imageFile\"; filename=\"camera.jpg\"\r\n";
  formDataStart += "Content-Type: image/jpeg\r\n\r\n";
  
  String formDataEnd = "\r\n--" + boundary + "--\r\n";
  
  size_t totalSize = formDataStart.length() + fb->len + formDataEnd.length();
  
  http.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
  
  uint8_t* postData = (uint8_t*)malloc(totalSize);
  if (!postData) {
    Serial.println("❌ 메모리 할당 실패!");
    esp_camera_fb_return(fb);
    return false;
  }
  
  size_t offset = 0;
  memcpy(postData + offset, formDataStart.c_str(), formDataStart.length());
  offset += formDataStart.length();
  memcpy(postData + offset, fb->buf, fb->len);
  offset += fb->len;
  memcpy(postData + offset, formDataEnd.c_str(), formDataEnd.length());
  
  Serial.println("🚀 서버 전송 중...");
  int httpResponseCode = http.POST(postData, totalSize);
  
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.printf("✅ HTTP 응답 코드: %d\n", httpResponseCode);
    Serial.printf("📄 서버 응답: %s\n", response.c_str());
    
    if (httpResponseCode == 201) {
      Serial.println("🎉 업로드 완료!");
    }
  } else {
    Serial.printf("❌ 업로드 실패! 오류 코드: %d\n", httpResponseCode);
    Serial.printf("❌ 오류 상세: %s\n", http.errorToString(httpResponseCode).c_str());
  }
  
  free(postData);
  esp_camera_fb_return(fb);
  http.end();
  
  Serial.printf("🧹 메모리 정리 완료, 현재 힙: %d bytes\n", ESP.getFreeHeap());
  return (httpResponseCode == 201);
}

void setup() {
  Serial.begin(115200);
  delay(2000);
  
  Serial.println("=== ESP32-CAM 사진 업로드 시스템 (회전 적용) ===");
  
  if (!initCamera()) {
    Serial.println("❌ 카메라 초기화 실패! 재시작합니다...");
    delay(5000);
    ESP.restart();
  }
  
  Serial.println("\nWiFi 연결 시작...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("\n✅ WiFi 연결 성공!");
  Serial.printf("IP 주소: %s\n", WiFi.localIP().toString().c_str());
 
  Serial.println("\n=== 초기화 완료 ===");
}

void loop() {
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n--- 📸 사진 촬영 및 업로드 ---");
    uploadPhoto();
  } else {
    Serial.println("❌ WiFi 연결 끊어짐, 재연결 시도...");
    WiFi.reconnect();
  }
  
  Serial.printf("⏰ 30초 대기 중... (현재 힙: %d bytes)\n", ESP.getFreeHeap());
  delay(30000);
}

