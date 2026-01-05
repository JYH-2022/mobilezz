import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from xgboost import XGBRegressor
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import joblib
import matplotlib.pyplot as plt

def load_data(filename='bitcoin_data.csv'):
    """데이터 로드"""
    print("📂 데이터 로딩 중...")
    df = pd.read_csv(filename, index_col=0, parse_dates=True)
    print(f"✅ {len(df)}개의 데이터 로드 완료!")
    return df

def prepare_features(df, prediction_hours):
    """
    특성(Feature)과 타겟(Target) 준비
    """
    print(f"\n🔧 [{prediction_hours}시간 후] 특성 준비 중...")
    
    # 타겟: prediction_hours 시간 후의 가격
    df_copy = df.copy()
    df_copy['target'] = df_copy['close'].shift(-prediction_hours)
    
    # 특성 선택 (예측에 사용할 데이터)
    feature_columns = [
    'open', 'high', 'low', 'close', 'volume',
    'MA7', 'MA30', 'MA90',
    'price_change', 'volume_ma', 'volume_change',
    'RSI', 'MACD', 'Signal_Line',
    'BB_middle', 'BB_upper', 'BB_lower',
    'volatility',
    'nasdaq_close', 'nasdaq_change',
    'hour', 'is_us_trading_hours', 'day_of_week', 'is_weekend', 'month',
    'news_sentiment', 'news_count'  # ⭐ 추가!
]
    
    # NaN 제거
    df_clean = df_copy.dropna()
    
    X = df_clean[feature_columns]
    y = df_clean['target']
    
    print(f"✅ 특성 준비 완료! 데이터 포인트: {len(X)}개")
    
    return X, y, feature_columns

def train_single_model(X, y, prediction_hours):
    """
    단일 시간대 모델 훈련
    """
    print(f"\n{'='*60}")
    print(f"🤖 [{prediction_hours}시간 후] 모델 훈련 시작")
    print(f"{'='*60}")
    
    # 데이터 분할
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, shuffle=False
    )
    
    print(f"   - 훈련: {len(X_train)}개, 테스트: {len(X_test)}개")
    
    # 스케일링
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    # 모델 훈련 (보수적 파라미터)
    print("   - XGBoost 학습 중...")
    model = XGBRegressor(
        n_estimators=200,
        max_depth=5,              # 더 단순하게 (과적합 방지)
        learning_rate=0.05,       # 더 천천히 학습
        subsample=0.8,
        colsample_bytree=0.8,
        reg_alpha=0.1,            # L1 규제 추가
        reg_lambda=1.0,           # L2 규제 추가
        random_state=42,
        n_jobs=-1
    )
    
    model.fit(
        X_train_scaled, 
        y_train,
        eval_set=[(X_test_scaled, y_test)],
        verbose=0
    )
    
    # 평가
    y_pred = model.predict(X_test_scaled)
    
    mse = mean_squared_error(y_test, y_pred)
    rmse = np.sqrt(mse)
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    
    # 방향 정확도
    actual_direction = np.diff(y_test) > 0
    pred_direction = np.diff(y_pred) > 0
    direction_accuracy = np.mean(actual_direction == pred_direction) * 100
    
    print(f"\n📊 [{prediction_hours}시간 후] 성능 결과:")
    print(f"   - RMSE: ${rmse:,.2f}")
    print(f"   - MAE: ${mae:,.2f}")
    print(f"   - R² Score: {r2:.4f}")
    print(f"   - 방향 정확도: {direction_accuracy:.2f}%")
    
    metrics = {
        'rmse': rmse,
        'mae': mae,
        'r2': r2,
        'direction_accuracy': direction_accuracy,
        'prediction_hours': prediction_hours
    }
    
    return model, scaler, metrics, y_test, y_pred

def save_model(model, scaler, feature_columns, metrics, prediction_hours):
    """
    모델 저장
    """
    model_name = f'bitcoin_model_{prediction_hours}h.pkl'
    scaler_name = f'scaler_{prediction_hours}h.pkl'
    config_name = f'model_config_{prediction_hours}h.pkl'
    
    joblib.dump(model, model_name)
    joblib.dump(scaler, scaler_name)
    
    config = {
        'feature_columns': feature_columns,
        'metrics': metrics,
        'prediction_hours': prediction_hours
    }
    joblib.dump(config, config_name)
    
    print(f"   ✅ 저장: {model_name}")

def create_comparison_plot(results):
    """
    3개 모델 비교 그래프
    """
    print("\n📊 모델 비교 그래프 생성 중...")
    
    fig, axes = plt.subplots(2, 2, figsize=(16, 10))
    
    # 방향 정확도 비교
    hours = [r['metrics']['prediction_hours'] for r in results]
    accuracies = [r['metrics']['direction_accuracy'] for r in results]
    
    axes[0, 0].bar(hours, accuracies, color=['#a855f7', '#8b5cf6', '#7c3aed'])
    axes[0, 0].set_xlabel('예측 시간 (시간)', fontsize=12)
    axes[0, 0].set_ylabel('방향 정확도 (%)', fontsize=12)
    axes[0, 0].set_title('방향 정확도 비교', fontsize=14, fontweight='bold')
    axes[0, 0].axhline(y=50, color='r', linestyle='--', label='랜덤 (50%)')
    axes[0, 0].legend()
    axes[0, 0].grid(True, alpha=0.3)
    
    # RMSE 비교
    rmses = [r['metrics']['rmse'] for r in results]
    axes[0, 1].bar(hours, rmses, color=['#a855f7', '#8b5cf6', '#7c3aed'])
    axes[0, 1].set_xlabel('예측 시간 (시간)', fontsize=12)
    axes[0, 1].set_ylabel('RMSE ($)', fontsize=12)
    axes[0, 1].set_title('오차 비교 (낮을수록 좋음)', fontsize=14, fontweight='bold')
    axes[0, 1].grid(True, alpha=0.3)
    
    # R² Score 비교
    r2s = [r['metrics']['r2'] for r in results]
    axes[1, 0].bar(hours, r2s, color=['#a855f7', '#8b5cf6', '#7c3aed'])
    axes[1, 0].set_xlabel('예측 시간 (시간)', fontsize=12)
    axes[1, 0].set_ylabel('R² Score', fontsize=12)
    axes[1, 0].set_title('R² Score 비교 (높을수록 좋음)', fontsize=14, fontweight='bold')
    axes[1, 0].axhline(y=1.0, color='g', linestyle='--', label='완벽 (1.0)')
    axes[1, 0].legend()
    axes[1, 0].grid(True, alpha=0.3)
    
    # 예측 예시 (1시간 모델)
    y_test = results[0]['y_test']
    y_pred = results[0]['y_pred']
    axes[1, 1].plot(y_test.values[:50], label='실제 가격', marker='o', markersize=4)
    axes[1, 1].plot(y_pred[:50], label='예측 가격', marker='x', markersize=4)
    axes[1, 1].set_xlabel('시간', fontsize=12)
    axes[1, 1].set_ylabel('가격 ($)', fontsize=12)
    axes[1, 1].set_title('1시간 예측 예시 (최근 50시간)', fontsize=14, fontweight='bold')
    axes[1, 1].legend()
    axes[1, 1].grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig('model_comparison.png', dpi=150, bbox_inches='tight')
    print("✅ 비교 그래프 저장: model_comparison.png")

if __name__ == "__main__":
    print("="*60)
    print("🚀 비트코인 AI 예측 - 다중 시간대 모델 훈련 (개선 버전)")
    print("="*60)
    
    try:
        # 데이터 로드
        df = load_data()
        
        # 3개 시간대 모델 훈련
        prediction_times = [1, 6, 24]
        results = []
        
        for hours in prediction_times:
            # 특성 준비
            X, y, feature_columns = prepare_features(df, prediction_hours=hours)
            
            # 모델 훈련
            model, scaler, metrics, y_test, y_pred = train_single_model(X, y, hours)
            
            # 모델 저장
            save_model(model, scaler, feature_columns, metrics, hours)
            
            # 결과 저장
            results.append({
                'metrics': metrics,
                'y_test': y_test,
                'y_pred': y_pred
            })
        
        # 비교 그래프 생성
        create_comparison_plot(results)
        
        print("\n" + "="*60)
        print("✅ 모든 모델 훈련 완료!")
        print("="*60)
        print("\n생성된 파일:")
        print("   - bitcoin_model_1h.pkl (1시간 후 예측)")
        print("   - bitcoin_model_6h.pkl (6시간 후 예측)")
        print("   - bitcoin_model_24h.pkl (24시간 후 예측)")
        print("   - model_comparison.png (비교 그래프)")
        print("\n다음 단계: API 서버 재시작")
        
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()