from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import joblib
import pandas as pd
import numpy as np
from binance.client import Client
from datetime import datetime, timedelta
from pydantic import BaseModel
import yfinance as yf
from news_analyzer import CryptoNewsAnalyzer

# FastAPI 앱 생성
app = FastAPI(title="Bitcoin Predictor API")

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Binance 클라이언트
client = Client()

# 뉴스 분석기
news_analyzer = CryptoNewsAnalyzer()

# 모델과 스케일러 로드
print("🔄 모델 로딩 중...")
models = {}
scalers = {}
configs = {}

for hours in [1, 6, 24]:
    try:
        models[hours] = joblib.load(f'bitcoin_model_{hours}h.pkl')
        scalers[hours] = joblib.load(f'scaler_{hours}h.pkl')
        configs[hours] = joblib.load(f'model_config_{hours}h.pkl')
        print(f"✅ {hours}시간 모델 로드 완료")
    except Exception as e:
        print(f"❌ {hours}시간 모델 로드 실패: {e}")

print("✅ 모든 모델 로드 완료!\n")

def get_latest_data():
    """
    최신 비트코인 데이터 가져오기 및 기술적 지표 계산
    """
    # 최근 200시간 데이터 가져오기
    klines = client.get_klines(
        symbol='BTCUSDT',
        interval=Client.KLINE_INTERVAL_1HOUR,
        limit=200
    )
    
    df = pd.DataFrame(klines, columns=[
        'timestamp', 'open', 'high', 'low', 'close', 'volume',
        'close_time', 'quote_volume', 'trades', 'taker_buy_base',
        'taker_buy_quote', 'ignore'
    ])
    
    df = df[['timestamp', 'open', 'high', 'low', 'close', 'volume']]
    df['timestamp'] = pd.to_datetime(df['timestamp'], unit='ms')
    
    for col in ['open', 'high', 'low', 'close', 'volume']:
        df[col] = df[col].astype(float)
    
    df.set_index('timestamp', inplace=True)
    
    # 기술적 지표 계산
    df['MA7'] = df['close'].rolling(window=7).mean()
    df['MA30'] = df['close'].rolling(window=30).mean()
    df['MA90'] = df['close'].rolling(window=90).mean()
    df['price_change'] = df['close'].pct_change()
    df['volume_ma'] = df['volume'].rolling(window=7).mean()
    df['volume_change'] = df['volume'].pct_change()
    
    # RSI
    delta = df['close'].diff()
    gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
    loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
    rs = gain / loss
    df['RSI'] = 100 - (100 / (1 + rs))
    
    # MACD
    exp1 = df['close'].ewm(span=12, adjust=False).mean()
    exp2 = df['close'].ewm(span=26, adjust=False).mean()
    df['MACD'] = exp1 - exp2
    df['Signal_Line'] = df['MACD'].ewm(span=9, adjust=False).mean()
    
    # 볼린저 밴드
    df['BB_middle'] = df['close'].rolling(window=20).mean()
    bb_std = df['close'].rolling(window=20).std()
    df['BB_upper'] = df['BB_middle'] + (bb_std * 2)
    df['BB_lower'] = df['BB_middle'] - (bb_std * 2)
    
    # 변동성
    df['volatility'] = df['close'].rolling(window=24).std()
    
    # 나스닥 데이터 추가
    try:
        nasdaq_start = df.index.min() - timedelta(days=7)
        nasdaq = yf.download('^IXIC', start=nasdaq_start, end=df.index.max(), progress=False)
        nasdaq_hourly = nasdaq['Close'].resample('1H').ffill()
        
        df['nasdaq_close'] = nasdaq_hourly
        df['nasdaq_close'] = df['nasdaq_close'].ffill()
        df['nasdaq_change'] = df['nasdaq_close'].pct_change()
    except Exception as e:
        print(f"⚠️ 나스닥 데이터 가져오기 실패: {e}")
        df['nasdaq_close'] = 0
        df['nasdaq_change'] = 0
    
    # 시간 특성
    df['hour'] = df.index.hour
    df['is_us_trading_hours'] = df['hour'].apply(lambda x: 1 if (x >= 23 or x < 6) else 0)
    df['day_of_week'] = df.index.dayofweek
    df['is_weekend'] = df['day_of_week'].apply(lambda x: 1 if x >= 5 else 0)
    df['month'] = df.index.month
    
    # 뉴스 감성 추가 ⭐ NEW!
    try:
        print("📰 실시간 뉴스 감성 분석 중...")
        news_summary = news_analyzer.get_news_summary(hours_ago=24)
        news_sentiment = news_summary['sentiment_score']
        news_count = news_summary['news_count']
        
        print(f"✅ 뉴스 감성: {news_sentiment:.3f} ({news_count}개 뉴스)")
        
        # 뉴스 정보 저장 (나중에 UI에 표시용)
        df.attrs['news_summary'] = news_summary
        
    except Exception as e:
        print(f"⚠️ 뉴스 분석 실패: {e}")
        news_sentiment = 0.0
        news_count = 0
        df.attrs['news_summary'] = None
    
    # 모든 행에 뉴스 감성 추가 (현재 시점 기준)
    df['news_sentiment'] = news_sentiment
    df['news_count'] = news_count
    
    df.dropna(inplace=True)
    
    return df

def generate_analysis_text(hours, current_price, predicted_price, change_percent, 
                          latest, rsi_value, rsi_signal, macd_signal, nasdaq_signal, 
                          top_features, feature_names_kr, news_summary):
    """
    서술형 분석 텍스트 생성
    """
    direction_text = "상승" if change_percent > 0 else "하락"
    abs_change = abs(predicted_price - current_price)
    
    # 시간대별 텍스트
    time_text = {
        1: "1시간",
        6: "6시간", 
        24: "24시간"
    }[hours]
    
    # 나스닥 상태
    nasdaq_price = latest['nasdaq_close']
    nasdaq_change_pct = latest['nasdaq_change'] * 100
    nasdaq_trend = "상승" if nasdaq_change_pct > 0 else "하락" if nasdaq_change_pct < 0 else "보합"
    
    # 미국 시장 상태
    us_market_status = "개장" if latest['is_us_trading_hours'] == 1 else "마감"
    us_market_impact = "활발한 거래가 예상됩니다" if latest['is_us_trading_hours'] == 1 else "변동성이 상대적으로 낮은 시간대입니다"
    
    # 변동성 평가
    volatility = latest['volatility']
    vol_assessment = "높은" if volatility > 2000 else "보통" if volatility > 1000 else "낮은"
    
    # 뉴스 감성 분석 ⭐ NEW!
    news_sentiment = latest['news_sentiment']
    news_count = int(latest['news_count'])
    
    if news_sentiment > 0.3:
        news_assessment = f"매우 긍정적(+{news_sentiment:.2f})"
        news_impact = "시장 심리가 호전되어 상승 압력이 강한 상태"
    elif news_sentiment > 0.1:
        news_assessment = f"긍정적(+{news_sentiment:.2f})"
        news_impact = "시장 분위기가 다소 긍정적인 편"
    elif news_sentiment < -0.3:
        news_assessment = f"매우 부정적({news_sentiment:.2f})"
        news_impact = "시장 심리가 악화되어 하락 압력이 존재"
    elif news_sentiment < -0.1:
        news_assessment = f"부정적({news_sentiment:.2f})"
        news_impact = "시장 분위기가 다소 부정적인 편"
    else:
        news_assessment = f"중립({news_sentiment:.2f})"
        news_impact = "시장 심리는 중립적"
    
    # 주요 뉴스 언급
    news_detail = ""
    if news_summary and news_count > 0:
        top_news = news_summary.get('top_news', [])[:2]
        if top_news:
            news_detail = "\n\n**주요 뉴스:**"
            for i, news in enumerate(top_news, 1):
                sentiment_emoji = "📈" if news['sentiment'] > 0.1 else "📉" if news['sentiment'] < -0.1 else "➡️"
                news_detail += f"\n{i}. {sentiment_emoji} {news['title'][:100]}"
    
    # RSI 상세 분석
    rsi_detail = ""
    if rsi_value > 70:
        rsi_detail = f"RSI가 {rsi_value:.1f}로 과매수 구간에 진입하여 단기 조정 가능성이 있습니다."
    elif rsi_value < 30:
        rsi_detail = f"RSI가 {rsi_value:.1f}로 과매도 구간에 있어 반등 가능성이 높습니다."
    else:
        rsi_detail = f"RSI가 {rsi_value:.1f}로 중립 구간에 있어 안정적인 흐름을 보이고 있습니다."
    
    # MACD 상세 분석
    macd_detail = ""
    if macd_signal == "상승 추세":
        macd_detail = "MACD 지표가 시그널선을 상향 돌파하며 상승 모멘텀을 시사하고 있습니다."
    else:
        macd_detail = "MACD 지표가 시그널선 하단에 위치하며 하락 압력이 존재합니다."
    
    # 주요 영향 요인 설명
    top_factor = feature_names_kr.get(top_features[0][0], top_features[0][0])
    top_importance = top_features[0][1] * 100
    
    # 최종 분석 텍스트 조합
    analysis = f"""현재 비트코인은 ${current_price:,.2f}에 거래되고 있습니다. 

**시장 환경:** 나스닥 지수는 {nasdaq_price:,.0f}포인트로 전일 대비 {abs(nasdaq_change_pct):.2f}% {nasdaq_trend}하며 {nasdaq_signal} 신호를 보내고 있습니다. 미국 증시는 현재 {us_market_status} 상태이며, {us_market_impact}. 

**뉴스 분석:** 최근 24시간 동안 수집된 {news_count}개의 비트코인 관련 뉴스를 분석한 결과, 뉴스 감성은 {news_assessment}입니다. {news_impact}입니다.{news_detail}

**기술적 분석:** {rsi_detail} {macd_detail} 현재 시장의 변동성은 {vol_assessment} 수준(${volatility:,.0f})을 기록하고 있습니다.

**AI 모델 분석:** 본 예측 모델은 {top_factor}을(를) 가장 중요한 요인({top_importance:.1f}%)으로 판단하고 있습니다. 과거 유사한 시장 조건에서의 패턴과 현재 뉴스 감성을 종합적으로 학습한 결과, {time_text} 후 비트코인 가격은 현재 대비 약 {abs(change_percent):.2f}% {direction_text}한 ${predicted_price:,.2f} 수준(±${abs_change:,.0f})에 도달할 확률이 높습니다.

**투자 유의사항:** 본 예측은 과거 데이터 및 뉴스 감성 기반 통계 모델의 분석 결과이며, 실제 가격은 예기치 못한 뉴스나 시장 이벤트에 따라 크게 달라질 수 있습니다. 투자 판단 시 참고 자료로만 활용하시기 바랍니다."""
    
    return analysis

def make_prediction(hours):
    """
    특정 시간대 예측
    """
    # 최신 데이터 가져오기
    df = get_latest_data()
    
    # 뉴스 정보 저장
    news_summary = df.attrs.get('news_summary', None)
    
    # 마지막 행 (최신 데이터) 추출
    latest = df.iloc[-1]
    
    # 특성 준비
    feature_columns = configs[hours]['feature_columns']
    features = latest[feature_columns].values.reshape(1, -1)
    
    # 스케일링
    features_scaled = scalers[hours].transform(features)
    
    # 예측
    raw_prediction = models[hours].predict(features_scaled)[0]
    
    # 현재 가격
    current_price = float(latest['close'])
    
    # 예측값을 현실적인 범위로 제한
    max_change = {
        1: 0.02,
        6: 0.04,
        24: 0.08
    }
    
    # 변화율 계산
    raw_change = (raw_prediction - current_price) / current_price
    
    # 변화율 제한 적용
    if abs(raw_change) > max_change[hours]:
        limited_change = max_change[hours] if raw_change > 0 else -max_change[hours]
        prediction = current_price * (1 + limited_change)
        print(f"⚠️ {hours}시간 예측값 조정: {raw_change*100:.2f}% → {limited_change*100:.2f}%")
    else:
        prediction = raw_prediction
    
    change_percent = ((prediction - current_price) / current_price) * 100
    
    # 신뢰도 계산
    confidence = configs[hours]['metrics']['r2'] * 100
    
    # Feature Importance
    feature_importance = models[hours].feature_importances_
    importance_dict = dict(zip(feature_columns, feature_importance))
    top_features = sorted(importance_dict.items(), key=lambda x: x[1], reverse=True)[:5]
    
    # 한글 이름 매핑
    feature_names_kr = {
        'close': '현재가',
        'open': '시가',
        'high': '고가',
        'low': '저가',
        'volume': '거래량',
        'RSI': 'RSI 지표',
        'MACD': 'MACD',
        'Signal_Line': 'MACD 시그널',
        'MA7': '7시간 이평선',
        'MA30': '30시간 이평선',
        'MA90': '90시간 이평선',
        'nasdaq_close': '나스닥 지수',
        'nasdaq_change': '나스닥 변화율',
        'is_us_trading_hours': '미국장 시간',
        'volatility': '변동성',
        'volume_ma': '거래량 평균',
        'volume_change': '거래량 변화',
        'BB_upper': '볼린저 상단',
        'BB_middle': '볼린저 중간',
        'BB_lower': '볼린저 하단',
        'hour': '시간대',
        'day_of_week': '요일',
        'is_weekend': '주말여부',
        'month': '월',
        'price_change': '가격 변화율',
        'news_sentiment': '뉴스 감성',  # ⭐ NEW
        'news_count': '뉴스 개수'  # ⭐ NEW
    }
    
    # 주요 지표 현재 값
    key_indicators = {
        'RSI': float(round(latest['RSI'], 2)),
        '나스닥': float(round(latest['nasdaq_close'], 2)),
        '변동성': float(round(latest['volatility'], 2)),
        '거래량': float(round(latest['volume'], 2)),
        '미국장': '개장' if latest['is_us_trading_hours'] == 1 else '마감',
        '뉴스감성': float(round(latest['news_sentiment'], 3)),  # ⭐ NEW
        '뉴스개수': int(latest['news_count'])  # ⭐ NEW
    }
    
    # 예측 근거 생성
    reasoning = []
    for feature, importance in top_features[:3]:
        feature_kr = feature_names_kr.get(feature, feature)
        feature_value = latest[feature]
        
        if hasattr(feature_value, 'item'):
            feature_value = feature_value.item()
        
        reasoning.append({
            'indicator': feature_kr,
            'importance': float(round(importance * 100, 1)),
            'value': float(round(feature_value, 2))
        })
    
    # RSI 분석
    rsi_value = latest['RSI']
    if rsi_value > 70:
        rsi_signal = "과매수 (조정 가능성)"
    elif rsi_value < 30:
        rsi_signal = "과매도 (반등 가능성)"
    else:
        rsi_signal = "중립"
    
    # MACD 분석
    macd_signal = "상승 추세" if latest['MACD'] > latest['Signal_Line'] else "하락 추세"
    
    # 나스닥 영향
    nasdaq_change = latest['nasdaq_change']
    if nasdaq_change > 0.01:
        nasdaq_signal = "긍정적"
    elif nasdaq_change < -0.01:
        nasdaq_signal = "부정적"
    else:
        nasdaq_signal = "중립"
    
    # 서술형 분석 텍스트 생성
    analysis_text = generate_analysis_text(
        hours=hours,
        current_price=current_price,
        predicted_price=prediction,
        change_percent=change_percent,
        latest=latest,
        rsi_value=rsi_value,
        rsi_signal=rsi_signal,
        macd_signal=macd_signal,
        nasdaq_signal=nasdaq_signal,
        top_features=top_features[:3],
        feature_names_kr=feature_names_kr,
        news_summary=news_summary  # ⭐ NEW
    )
    
    return {
        'prediction_hours': int(hours),
        'current_price': float(round(current_price, 2)),
        'predicted_price': float(round(prediction, 2)),
        'change_percent': float(round(change_percent, 2)),
        'direction': 'up' if change_percent > 0 else 'down',
        'confidence': float(round(confidence, 1)),
        'timestamp': datetime.now().isoformat(),
        'analysis': {
            'top_factors': reasoning,
            'indicators': key_indicators,
            'signals': {
                'rsi': {'value': float(round(rsi_value, 2)), 'signal': rsi_signal},
                'macd': macd_signal,
                'nasdaq': nasdaq_signal,
                'us_market': '개장 중' if latest['is_us_trading_hours'] == 1 else '마감'
            },
            'detailed_text': analysis_text,
            'news_summary': news_summary  # ⭐ NEW - UI에서 뉴스 표시용
        }
    }

# API 엔드포인트들

@app.get("/")
def read_root():
    """API 상태 확인"""
    return {
        "status": "running",
        "message": "Bitcoin Predictor API with News Sentiment",
        "available_models": [1, 6, 24],
        "features": ["Real-time price", "Technical indicators", "Nasdaq correlation", "News sentiment"]
    }

@app.get("/predict/all")
def predict_all():
    """
    모든 시간대 예측 (1시간, 6시간, 24시간)
    """
    try:
        predictions = {}
        for hours in [1, 6, 24]:
            predictions[f'{hours}h'] = make_prediction(hours)
        
        return {
            "success": True,
            "predictions": predictions
        }
    except Exception as e:
        print(f"에러 발생: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/predict/{hours}")
def predict_single(hours: int):
    """
    특정 시간대 예측
    """
    if hours not in [1, 6, 24]:
        raise HTTPException(status_code=400, detail="Only 1, 6, or 24 hours supported")
    
    try:
        prediction = make_prediction(hours)
        return {
            "success": True,
            "prediction": prediction
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/current-price")
def get_current_price():
    """
    현재 비트코인 가격만 반환
    """
    try:
        ticker = client.get_ticker(symbol='BTCUSDT')
        return {
            "success": True,
            "price": float(ticker['lastPrice']),
            "change_24h": float(ticker['priceChangePercent']),
            "timestamp": datetime.now().isoformat()
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/model-info")
def get_model_info():
    """
    모델 성능 정보 반환
    """
    try:
        info = {}
        for hours in [1, 6, 24]:
            info[f'{hours}h'] = {
                'rmse': float(round(configs[hours]['metrics']['rmse'], 2)),
                'mae': float(round(configs[hours]['metrics']['mae'], 2)),
                'r2_score': float(round(configs[hours]['metrics']['r2'], 4)),
                'direction_accuracy': float(round(configs[hours]['metrics']['direction_accuracy'], 2))
            }
        return {
            "success": True,
            "models": info
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    print("\n" + "="*60)
    print("🚀 Bitcoin Predictor API 서버 시작 (뉴스 감성 포함)")
    print("="*60)
    print("📡 API 주소: http://localhost:8000")
    print("📚 API 문서: http://localhost:8000/docs")
    print("📰 뉴스 감성 분석: 활성화")
    print("="*60 + "\n")
    
    uvicorn.run(app, host="0.0.0.0", port=8000)