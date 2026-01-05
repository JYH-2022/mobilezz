import requests
from datetime import datetime, timedelta
from textblob import TextBlob
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
import feedparser
import time

class CryptoNewsAnalyzer:
    def __init__(self):
        self.vader = SentimentIntensityAnalyzer()
        
    def get_crypto_news(self):
        """
        여러 RSS 피드에서 비트코인 뉴스 가져오기
        """
        news_list = []
        
        # RSS 피드 목록
        rss_feeds = [
            'https://www.coindesk.com/arc/outboundfeeds/rss/',
            'https://cointelegraph.com/rss',
            'https://decrypt.co/feed',
        ]
        
        for feed_url in rss_feeds:
            try:
                print(f"📡 {feed_url.split('/')[2]} 에서 뉴스 가져오는 중...")
                feed = feedparser.parse(feed_url)
                
                for entry in feed.entries[:10]:  # 각 소스당 10개
                    try:
                        title = entry.get('title', '')
                        published = entry.get('published', '')
                        link = entry.get('link', '')
                        summary = entry.get('summary', '')
                        
                        # 비트코인 관련 뉴스만 필터링
                        if 'bitcoin' in title.lower() or 'btc' in title.lower():
                            news_list.append({
                                'title': title,
                                'summary': summary[:200],
                                'url': link,
                                'published_at': published
                            })
                    except:
                        continue
                        
                print(f"   ✅ {len([n for n in news_list])}개 수집")
                time.sleep(0.5)  # API 부담 줄이기
                
            except Exception as e:
                print(f"   ⚠️ 실패: {e}")
                continue
        
        return news_list
    
    def analyze_sentiment(self, text):
        """
        뉴스 텍스트의 감성 분석
        VADER: -1 (매우 부정) ~ +1 (매우 긍정)
        """
        try:
            # VADER 감성 분석
            scores = self.vader.polarity_scores(text)
            return scores['compound']
        except:
            return 0.0
    
    def get_news_summary(self, hours_ago=24):
        """
        최근 N시간의 뉴스 요약 및 감성 분석
        """
        print(f"\n📰 최근 {hours_ago}시간 비트코인 뉴스 분석 시작")
        print("="*60)
        
        # 뉴스 가져오기
        news_list = self.get_crypto_news()
        
        if not news_list:
            print("⚠️ 뉴스를 가져올 수 없어 기본값 사용")
            return {
                'sentiment_score': 0.0,
                'news_count': 0,
                'positive_count': 0,
                'negative_count': 0,
                'neutral_count': 0,
                'top_news': []
            }
        
        print(f"\n🔍 총 {len(news_list)}개 뉴스 감성 분석 중...")
        
        sentiments = []
        positive_count = 0
        negative_count = 0
        neutral_count = 0
        analyzed_news = []
        
        for news in news_list:
            try:
                title = news.get('title', '')
                
                # 제목 + 요약으로 감성 분석
                full_text = title + ' ' + news.get('summary', '')
                sentiment = self.analyze_sentiment(full_text)
                sentiments.append(sentiment)
                
                # 분류
                if sentiment > 0.1:
                    positive_count += 1
                    sentiment_label = '긍정'
                elif sentiment < -0.1:
                    negative_count += 1
                    sentiment_label = '부정'
                else:
                    neutral_count += 1
                    sentiment_label = '중립'
                
                analyzed_news.append({
                    'title': title,
                    'sentiment': sentiment,
                    'sentiment_label': sentiment_label,
                    'url': news.get('url', ''),
                    'published_at': news.get('published_at', '')
                })
                
            except Exception as e:
                continue
        
        # 평균 감성 점수
        avg_sentiment = sum(sentiments) / len(sentiments) if sentiments else 0.0
        
        # 감성 점수 절댓값 기준으로 정렬 (영향력 큰 뉴스)
        top_news = sorted(analyzed_news, key=lambda x: abs(x['sentiment']), reverse=True)[:5]
        
        print(f"\n✅ 뉴스 분석 완료!")
        print("="*60)
        print(f"📊 분석 결과:")
        print(f"   - 총 뉴스: {len(analyzed_news)}개")
        print(f"   - 평균 감성: {avg_sentiment:.3f} ({self.get_sentiment_text(avg_sentiment)})")
        print(f"   - 긍정 뉴스: {positive_count}개")
        print(f"   - 중립 뉴스: {neutral_count}개")
        print(f"   - 부정 뉴스: {negative_count}개")
        
        return {
            'sentiment_score': round(avg_sentiment, 4),
            'news_count': len(analyzed_news),
            'positive_count': positive_count,
            'negative_count': negative_count,
            'neutral_count': neutral_count,
            'top_news': top_news
        }
    
    def get_sentiment_text(self, score):
        """감성 점수를 텍스트로 변환"""
        if score > 0.5:
            return "매우 긍정적"
        elif score > 0.2:
            return "긍정적"
        elif score > -0.2:
            return "중립"
        elif score > -0.5:
            return "부정적"
        else:
            return "매우 부정적"

# 테스트 함수
if __name__ == "__main__":
    print("\n" + "="*60)
    print("🚀 암호화폐 뉴스 감성 분석 테스트")
    print("="*60)
    
    analyzer = CryptoNewsAnalyzer()
    summary = analyzer.get_news_summary(hours_ago=24)
    
    print("\n" + "="*60)
    print("📰 주요 뉴스 TOP 5:")
    print("="*60)
    for i, news in enumerate(summary['top_news'], 1):
        print(f"\n{i}. [{news['sentiment_label']}] 점수: {news['sentiment']:.3f}")
        print(f"   {news['title'][:100]}")