from flask import Flask, request, jsonify
from ultralytics import YOLO
import os
from werkzeug.utils import secure_filename
import tempfile
from collections import defaultdict

# Flask 애플리케이션 생성
app = Flask(__name__)

# 업로드 허용 확장자
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}

# 신뢰도 임계값 설정 (70% 이상만 유효한 검출로 간주)
CONFIDENCE_THRESHOLD = 0.7

# ❗❗❗ [수정 필요] 학습된 모델('best.pt')의 실제 경로를 지정해주세요.
model_path = '/home/hyunjunoh/yoloTest/cherry tomato.v6i.yolov11/train_result/weights/best.pt'
model = YOLO(model_path)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def process_detections(results):
    """검출 결과를 처리하여 임계값 이상인 것들만 반환"""
    names = results[0].names
    valid_detections = []
    class_counts = defaultdict(int)
    confidences = []
    
    # 검출된 객체가 있는지 확인
    if results[0].boxes is not None and len(results[0].boxes) > 0:
        for box in results[0].boxes:
            confidence = float(box.conf[0])
            
            # 임계값 이상인 검출만 처리
            if confidence >= CONFIDENCE_THRESHOLD:
                class_id = int(box.cls[0])
                class_name = names[class_id]
                
                valid_detections.append({
                    'className': class_name,
                    'confidence': round(confidence, 4)
                })
                
                class_counts[class_name] += 1
                confidences.append(confidence)
    
    # 결과 구성
    if valid_detections:
        # 평균 신뢰도 계산
        avg_confidence = sum(confidences) / len(confidences)
        
        # 가장 높은 신뢰도의 클래스 찾기
        best_detection = max(valid_detections, key=lambda x: x['confidence'])
        
        return {
            'totalDetected': len(valid_detections),
            'bestResult': best_detection['className'],
            'avgConfidence': round(avg_confidence, 4),
            'classSummary': dict(class_counts),
            'detections': valid_detections
        }
    else:
        # 유효한 검출이 없을 때
        return {
            'totalDetected': 0,
            'bestResult': 'no_detection',
            'avgConfidence': 0.0,
            'classSummary': {},
            'detections': []
        }

# '/analyze' 주소로 POST 요청을 처리할 API 엔드포인트 (상세 분석용)
@app.route('/analyze', methods=['POST'])
def analyze_image_detailed():
    """상세 분석 - 모든 유효한 검출 결과 반환"""
    if model is None:
        return jsonify({'error': 'YOLO 모델이 서버에 로드되지 않았습니다.'}), 500
    
    if 'file' not in request.files:
        return jsonify({'error': '파일이 요청에 포함되지 않았습니다.'}), 400
    
    file = request.files['file']
    
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
            
            # 검출 결과 처리
            analysis_result = process_detections(results)
            
            print(f"✅ 상세 분석 완료: {file.filename}")
            print(f"   - 총 검출: {analysis_result['totalDetected']}개")
            print(f"   - 최고 클래스: {analysis_result['bestResult']}")
            print(f"   - 평균 신뢰도: {analysis_result['avgConfidence']}")
            
            # 임시 파일 삭제
            os.unlink(temp_path)
            
            return jsonify(analysis_result)

        except Exception as e:
            print(f"❌ 분석 중 오류 발생: {e}")
            if 'temp_path' in locals():
                try:
                    os.unlink(temp_path)
                except:
                    pass
            return jsonify({'error': '이미지 분석 중 오류 발생', 'details': str(e)}), 500
    
    else:
        return jsonify({'error': '허용되지 않는 파일 형식입니다. (png, jpg, jpeg, gif, bmp만 허용)'}), 400

# 기존 단순 분석 API도 유지 (호환성을 위해)
@app.route('/analyze_simple', methods=['POST'])
def analyze_image_simple():
    """단순 분석 - 가장 높은 신뢰도 결과만 반환"""
    if model is None:
        return jsonify({'error': 'YOLO 모델이 서버에 로드되지 않았습니다.'}), 500
    
    if 'file' not in request.files:
        return jsonify({'error': '파일이 요청에 포함되지 않았습니다.'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': '파일이 선택되지 않았습니다.'}), 400
    
    if file and allowed_file(file.filename):
        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(file.filename)[1]) as temp_file:
                file.save(temp_file.name)
                temp_path = temp_file.name
            
            results = model(temp_path)
            analysis_result = process_detections(results)
            
            # 단순한 형태로 반환
            simple_result = {
                'objectName': analysis_result['bestResult'],
                'confidence': analysis_result['avgConfidence']
            }
            
            print(f"✅ 단순 분석 완료: {file.filename} -> {simple_result}")
            
            os.unlink(temp_path)
            return jsonify(simple_result)

        except Exception as e:
            print(f"❌ 분석 중 오류 발생: {e}")
            if 'temp_path' in locals():
                try:
                    os.unlink(temp_path)
                except:
                    pass
            return jsonify({'error': '이미지 분석 중 오류 발생', 'details': str(e)}), 500
    
    else:
        return jsonify({'error': '허용되지 않는 파일 형식입니다.'}), 400

# 파일 경로 방식 분석 (기존 호환성 유지)
@app.route('/analyze_path', methods=['POST'])
def analyze_image_by_path():
    if model is None:
        return jsonify({'error': 'YOLO 모델이 서버에 로드되지 않았습니다.'}), 500
    
    data = request.get_json()
    if not data or 'filePath' not in data:
        return jsonify({'error': '"filePath"가 필요합니다.'}), 400

    image_path = data['filePath']

    if not os.path.exists(image_path):
        return jsonify({'error': f'파일을 찾을 수 없습니다: {image_path}'}), 404

    try:
        results = model(image_path)
        analysis_result = process_detections(results)
        
        print(f"✅ 경로 분석 완료: {image_path}")
        print(f"   - 총 검출: {analysis_result['totalDetected']}개")
        print(f"   - 최고 클래스: {analysis_result['bestResult']}")
        
        return jsonify(analysis_result)

    # 'as' 키워드 추가
    except Exception as e:
        print(f"❌ 분석 중 오류 발생: {e}")
        return jsonify({'error': '이미지 분석 중 오류 발생', 'details': str(e)}), 500

if __name__ == '__main__':
    print(f"🚀 YOLO AI 서버 시작")
    print(f"📊 신뢰도 임계값: {CONFIDENCE_THRESHOLD} ({CONFIDENCE_THRESHOLD*100}%)")
    print(f"🔗 엔드포인트:")
    print(f"   - POST /analyze (상세 분석)")
    print(f"   - POST /analyze_simple (단순 분석)")
    print(f"   - POST /analyze_path (파일 경로 분석)")
    
    app.run(host='0.0.0.0', port=5000, debug=True)