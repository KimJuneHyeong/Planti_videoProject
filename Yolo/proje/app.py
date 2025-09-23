from flask import Flask, request, jsonify
from ultralytics import YOLO
import os

# Flask 애플리케이션 생성
app = Flask(__name__)

# ❗❗❗ [수정 필요] 학습된 모델('best.pt')의 실제 경로를 지정해주세요.
# 아래 경로 예시는 testing.py 파일을 참고했습니다.
model_path = '/home/hyunjun/yoloTest/cherry tomato.v6i.yolov11/train_result/weights/best.pt'
model = YOLO(model_path)

# '/analyze' 주소로 POST 요청을 처리할 API 엔드포인트
@app.route('/analyze', methods=['POST'])
def analyze_image():
    # Spring Boot로부터 받은 JSON 데이터에서 'filePath' 추출
    data = request.get_json()
    if not data or 'filePath' not in data:
        return jsonify({'error': '"filePath"가 필요합니다.'}), 400

    image_path = data['filePath']

    # 파일이 존재하는지 확인
    if not os.path.exists(image_path):
        return jsonify({'error': f'파일을 찾을 수 없습니다: {image_path}'}), 404

    try:
        # YOLO 모델로 이미지 분석 수행
        results = model(image_path)
        
        # 분석 결과 중 가장 신뢰도 높은 것 하나만 선택
        best_result = {}
        highest_confidence = 0.0

        names = results[0].names
        for box in results[0].boxes:
            confidence = float(box.conf[0])
            if confidence > highest_confidence:
                highest_confidence = confidence
                class_id = int(box.cls[0])
                best_result = {
                    'objectName': names[class_id],
                    'confidence': round(highest_confidence, 4)
                }

        print(f"✅ 분석 완료: {image_path} -> {best_result}")

        # 가장 신뢰도 높은 결과를 JSON 형태로 Spring Boot에 반환
        return jsonify(best_result)

    except Exception as e:
        print(f"❌ 분석 중 오류 발생: {e}")
        return jsonify({'error': '이미지 분석 중 오류 발생', 'details': str(e)}), 500

if __name__ == '__main__':
    # 서버 실행 (IP는 모든 곳에서 접근 가능하도록 '0.0.0.0'으로 설정)
    app.run(host='0.0.0.0', port=5000, debug=True)
