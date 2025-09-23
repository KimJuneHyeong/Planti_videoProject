from flask import Flask, request, jsonify
from ultralytics import YOLO
import os
from werkzeug.utils import secure_filename
import tempfile

# Flask 애플리케이션 생성
app = Flask(__name__)

# 업로드 허용 확장자
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}

# ❗❗❗ [수정 필요] 학습된 모델('best.pt')의 실제 경로를 지정해주세요.
# 아래 경로 예시는 testing.py 파일을 참고했습니다.
model_path = '/home/hyunjunoh/yoloTest/cherry tomato.v6i.yolov11/train_result/weights/best.pt'
model = YOLO(model_path)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# '/analyze' 주소로 POST 요청을 처리할 API 엔드포인트 (파일 업로드 방식)
@app.route('/analyze', methods=['POST'])
def analyze_image():
    # 모델이 로드되지 않은 경우 오류 반환
    if model is None:
        return jsonify({'error': 'YOLO 모델이 서버에 로드되지 않았습니다.'}), 500
    
    # 파일이 요청에 포함되어 있는지 확인
    if 'file' not in request.files:
        return jsonify({'error': '파일이 요청에 포함되지 않았습니다.'}), 400
    
    file = request.files['file']
    
    # 파일이 실제로 선택되었는지 확인
    if file.filename == '':
        return jsonify({'error': '파일이 선택되지 않았습니다.'}), 400
    
    if file and allowed_file(file.filename):
        try:
            # 임시 파일로 저장
            with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as temp_file:
                file.save(temp_file.name)
                temp_path = temp_file.name
            
            # YOLO 모델로 이미지 분석 수행
            results = model(temp_path)
            
            # 분석 결과 중 가장 신뢰도 높은 것 하나만 선택
            best_result = {}
            highest_confidence = 0.0

            names = results[0].names
            
            # 검출된 객체가 있는지 확인
            if results[0].boxes is not None and len(results[0].boxes) > 0:
                for box in results[0].boxes:
                    confidence = float(box.conf[0])
                    if confidence > highest_confidence:
                        highest_confidence = confidence
                        class_id = int(box.cls[0])
                        best_result = {
                            'objectName': names[class_id],
                            'confidence': round(highest_confidence, 4)
                        }
            else:
                # 검출된 객체가 없을 때
                best_result = {
                    'objectName': 'no_detection',
                    'confidence': 0.0
                }

            print(f"✅ 분석 완료: {file.filename} -> {best_result}")

            # 임시 파일 삭제
            os.unlink(temp_path)

            # 가장 신뢰도 높은 결과를 JSON 형태로 Spring Boot에 반환
            return jsonify(best_result)

        except Exception as e:
            print(f"❌ 분석 중 오류 발생: {e}")
            # 임시 파일 삭제 (오류 발생 시에도)
            if 'temp_path' in locals():
                try:
                    os.unlink(temp_path)
                except:
                    pass
            return jsonify({'error': '이미지 분석 중 오류 발생', 'details': str(e)}), 500
    
    else:
        return jsonify({'error': '허용되지 않는 파일 형식입니다. (png, jpg, jpeg, gif, bmp만 허용)'}), 400

# 기존 방식도 유지 (호환성을 위해)
@app.route('/analyze_path', methods=['POST'])
def analyze_image_by_path():
    # 모델이 로드되지 않은 경우 오류 반환
    if model is None:
        return jsonify({'error': 'YOLO 모델이 서버에 로드되지 않았습니다.'}), 500
    
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
        
        # 검출된 객체가 있는지 확인
        if results[0].boxes is not None and len(results[0].boxes) > 0:
            for box in results[0].boxes:
                confidence = float(box.conf[0])
                if confidence > highest_confidence:
                    highest_confidence = confidence
                    class_id = int(box.cls[0])
                    best_result = {
                        'objectName': names[class_id],
                        'confidence': round(highest_confidence, 4)
                    }
        else:
            # 검출된 객체가 없을 때
            best_result = {
                'objectName': 'no_detection',
                'confidence': 0.0
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