import pandas as pd
import numpy as np
from binance.client import Client
from datetime import datetime, timedelta
import yfinance as yf
from news_analyzer import CryptoNewsAnalyzer

# Binance 클라이언트 생성
client = Client()

def collect_bitcoin_data(days=365):
    """
    과거 비트코인 데이터 수집
    """
    print(f"📊 과거 {days}일의 비트코인 데이터 수집 중...")
    
    end_date = datetime.now()
    start_date = end_date - timedelta(days=days)
    
    start_str = str(int(start_date.timestamp() * 1000))
    end_str = str(int(end_date.timestamp() * 1000))
    
    print("⏳ Binance API 호출 중...")
    klines = client.get_historical_klines(
        "BTCUSDT",
        Client.KLINE_INTERVAL_1HOUR,
        start_str,
        end_str
    )
    
    print(f"✅ {len(klines)}개의 데이터 포인트 수집 완료!")
    
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
    
    print("\n📈 데이터 미리보기:")
    print(df.head())
    
    return df

def add_nasdaq_data(df):
    """
    나스닥 지수 데이터 추가
    """
    print("\n📊 나스닥 지수 데이터 가져오는 중...")
    
    start_date = df.index.min()
    end_date = df.index.max()
    
    try:
        nasdaq = yf.download('^IXIC', start=start_date, end=end_date, progress=False)
        nasdaq_hourly = nasdaq['Close'].resample('1H').ffill()
        
        df['nasdaq_close'] = nasdaq_hourly
        df['nasdaq_close'] = df['nasdaq_close'].ffill()
        df['nasdaq_change'] = df['nasdaq_close'].pct_change()
        
        print(f"✅ 나스닥 데이터 추가 완료!")
        
    except Exception as e:
        print(f"⚠️ 나스닥 데이터 가져오기 실패: {e}")
        df['nasdaq_close'] = 0
        df['nasdaq_change'] = 0
    
    return df

def add_news_sentiment(df):
    """
    뉴스 감성 데이터 추가
    """
    print("\n📰 뉴스 감성 데이터 추가 중...")
    
    # 현재 뉴스 감성 가져오기
    try:
        analyzer = CryptoNewsAnalyzer()
        current_news = analyzer.get_news_summary(hours_ago=24)
        current_sentiment = current_news['sentiment_score']
        
        print(f"✅ 현재 뉴스 감성: {current_sentiment:.3f}")
        print(f"   - 긍정 뉴스: {current_news['positive_count']}개")
        print(f"   - 부정 뉴스: {current_news['negative_count']}개")
        
    except Exception as e:
        print(f"⚠️ 뉴스 감성 분석 실패: {e}")
        current_sentiment = 0.0
    
    # 전략: 과거 데이터는 중립(0), 최근 24시간은 실제 값
    cutoff_time = df.index.max() - timedelta(hours=24)
    
    df['news_sentiment'] = 0.0  # 기본값: 중립
    
    # 최근 24시간 데이터에는 실제 감성 값 적용
    recent_mask = df.index > cutoff_time
    df.loc[recent_mask, 'news_sentiment'] = current_sentiment
    
    # 뉴스 개수 (최근만)
    df['news_count'] = 0
    df.loc[recent_mask, 'news_count'] = current_news['news_count']
    
    print(f"✅ 뉴스 감성 특성 추가 완료!")
    print(f"   - 과거 데이터: 중립(0.0)")
    print(f"   - 최근 24시간: 실제 감성({current_sentiment:.3f})")
    
    return df

def add_time_features(df):
    """
    시간 기반 특성 추가
    """
    print("\n🕐 시간 특성 추가 중...")
    
    df['hour'] = df.index.hour
    df['is_us_trading_hours'] = df['hour'].apply(
        lambda x: 1 if (x >= 23 or x < 6) else 0
    )
    df['day_of_week'] = df.index.dayofweek
    df['is_weekend'] = df['day_of_week'].apply(lambda x: 1 if x >= 5 else 0)
    df['month'] = df.index.month
    
    print(f"✅ 시간 특성 추가 완료!")
    
    return df

def add_technical_indicators(df):
    """
    기술적 지표 추가
    """
    print("\n🔧 기술적 지표 계산 중...")
    
    # 이동평균선
    df['MA7'] = df['close'].rolling(window=7).mean()
    df['MA30'] = df['close'].rolling(window=30).mean()
    df['MA90'] = df['close'].rolling(window=90).mean()
    
    # 가격 변화율
    df['price_change'] = df['close'].pct_change()
    
    # 거래량
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
    
    # NaN 제거
    df.dropna(inplace=True)
    
    print(f"✅ 기술적 지표 추가 완료! (남은 데이터: {len(df)}개)")
    
    return df

def save_data(df, filename='bitcoin_data.csv'):
    """데이터 저장"""
    df.to_csv(filename)
    print(f"\n💾 데이터 저장 완료: {filename}")
    print(f"📁 총 특성 개수: {len(df.columns)}개")
    print(f"📁 총 데이터 포인트: {len(df)}개")
    
    print("\n📋 특성 목록:")
    for i, col in enumerate(df.columns, 1):
        print(f"   {i}. {col}")

if __name__ == "__main__":
    print("="*60)
    print("🚀 비트코인 AI 예측 - 뉴스 감성 포함 데이터 수집")
    print("="*60)
    
    try:
        # 1. 비트코인 데이터 수집
        df = collect_bitcoin_data(days=365)
        
        # 2. 나스닥 데이터 추가
        df = add_nasdaq_data(df)
        
        # 3. 뉴스 감성 추가 ⭐ NEW!
        df = add_news_sentiment(df)
        
        # 4. 시간 특성 추가
        df = add_time_features(df)
        
        # 5. 기술적 지표 추가
        df = add_technical_indicators(df)
        
        # 6. 데이터 저장
        save_data(df)
        
        print("\n" + "="*60)
        print("✅ 모든 작업 완료!")
        print("="*60)
        print("\n🎯 추가된 새로운 특성:")
        print("  📰 news_sentiment (뉴스 감성 점수: -1 ~ +1)")
        print("  📊 news_count (뉴스 개수)")
        print("\n다음 단계: train_model.py 실행")
        
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()