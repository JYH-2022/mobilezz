import { useState, useEffect } from 'react';
import { TrendingUp, AlertCircle, BarChart3, RefreshCw } from 'lucide-react';
import PriceChart from './PriceChart';
import AIPrediction from './AIPrediction';

export default function BitcoinPredictorApp() {
  const [showDisclaimer, setShowDisclaimer] = useState(true);
  const [currentPrice, setCurrentPrice] = useState('로딩중...');
  const [priceChange, setPriceChange] = useState('0.00');
  const [loading, setLoading] = useState(false);
  const [timeframe, setTimeframe] = useState('1h');

  // 비트코인 가격 가져오기 함수
  const fetchBitcoinPrice = async () => {
    setLoading(true);
    try {
      const response = await fetch('https://api.binance.com/api/v3/ticker/24hr?symbol=BTCUSDT');
      const data = await response.json();
      
      const price = parseFloat(data.lastPrice).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      });
      
      const change = parseFloat(data.priceChangePercent).toFixed(2);
      
      setCurrentPrice(price);
      setPriceChange(change);
    } catch (error) {
      console.error('가격 가져오기 실패:', error);
      setCurrentPrice('오류 발생');
      setPriceChange('0.00');
    }
    setLoading(false);
  };

  useEffect(() => {
    if (!showDisclaimer) {
      fetchBitcoinPrice();
      const interval = setInterval(fetchBitcoinPrice, 10000);
      return () => clearInterval(interval);
    }
  }, [showDisclaimer]);

  if (showDisclaimer) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full p-8">
          <div className="flex items-center justify-center mb-6">
            <AlertCircle className="w-16 h-16 text-amber-500" />
          </div>
          
          <h1 className="text-3xl font-bold text-center mb-4 text-gray-800">
            투자 유의사항
          </h1>
          
          <div className="space-y-4 text-gray-700 mb-8">
            <p className="text-lg leading-relaxed">
              본 앱은 <span className="font-semibold text-purple-600">정보 제공 및 트렌드 분석 도구</span>입니다.
            </p>
            
            <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded">
              <p className="font-semibold text-amber-800 mb-2">⚠️ 중요한 안내</p>
              <ul className="space-y-2 text-sm">
                <li>• AI 예측은 참고 자료일 뿐, 투자 조언이 아닙니다</li>
                <li>• 암호화폐 투자는 높은 위험을 동반합니다</li>
                <li>• 모든 투자 결정은 본인의 책임입니다</li>
                <li>• 과거 데이터가 미래를 보장하지 않습니다</li>
              </ul>
            </div>
            
            <p className="text-center font-medium text-gray-600">
              투자는 신중하게, 잃어도 괜찮은 금액만 투자하세요.
            </p>
          </div>
          
          <button
            onClick={() => setShowDisclaimer(false)}
            className="w-full bg-gradient-to-r from-purple-600 to-blue-600 text-white py-4 rounded-xl font-semibold text-lg hover:from-purple-700 hover:to-blue-700 transition-all transform hover:scale-105 shadow-lg"
          >
            이해했습니다. 계속하기
          </button>
        </div>
      </div>
    );
  }

  const priceChangeColor = parseFloat(priceChange) >= 0 ? 'text-green-400' : 'text-red-400';
  const priceChangeSign = parseFloat(priceChange) >= 0 ? '+' : '';

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <header className="bg-black bg-opacity-50 backdrop-blur-md border-b border-purple-500 border-opacity-30">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <TrendingUp className="w-8 h-8 text-purple-400" />
            <h1 className="text-2xl font-bold text-white">Bitcoin Predictor</h1>
          </div>
          <button 
            onClick={() => setShowDisclaimer(true)}
            className="text-purple-300 hover:text-purple-100 transition-colors text-sm"
          >
            면책조항 다시보기
          </button>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8">
        {/* 현재 가격 카드 */}
        <div className="bg-white bg-opacity-10 backdrop-blur-lg rounded-2xl p-8 mb-6 border border-purple-500 border-opacity-30">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-purple-200">비트코인 (BTC/USDT)</h2>
            <button 
              onClick={fetchBitcoinPrice}
              disabled={loading}
              className="flex items-center space-x-2 bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              <span>{loading ? '로딩중...' : '새로고침'}</span>
            </button>
          </div>
          
          <div className="flex items-baseline space-x-4">
            <span className="text-5xl font-bold text-white">${currentPrice}</span>
            <span className={`text-2xl font-semibold ${priceChangeColor}`}>
              {priceChangeSign}{priceChange}%
            </span>
          </div>
          
          <p className="text-purple-200 mt-2">
            실시간 가격 (Binance 연동) • 10초마다 자동 업데이트
          </p>
        </div>

        {/* 차트 영역 */}
        <div className="bg-white bg-opacity-10 backdrop-blur-lg rounded-2xl p-8 mb-6 border border-purple-500 border-opacity-30">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-white flex items-center space-x-2">
              <BarChart3 className="w-6 h-6 text-purple-400" />
              <span>가격 차트</span>
            </h2>
            <div className="flex space-x-2">
              <button 
                onClick={() => setTimeframe('1h')}
                className={`px-4 py-2 rounded-lg text-sm ${
                  timeframe === '1h' 
                    ? 'bg-purple-600 text-white' 
                    : 'bg-purple-900 bg-opacity-50 text-purple-200 hover:bg-purple-800'
                }`}
              >
                1시간
              </button>
              <button 
                onClick={() => setTimeframe('1d')}
                className={`px-4 py-2 rounded-lg text-sm ${
                  timeframe === '1d' 
                    ? 'bg-purple-600 text-white' 
                    : 'bg-purple-900 bg-opacity-50 text-purple-200 hover:bg-purple-800'
                }`}
              >
                1일
              </button>
              <button 
                onClick={() => setTimeframe('1w')}
                className={`px-4 py-2 rounded-lg text-sm ${
                  timeframe === '1w' 
                    ? 'bg-purple-600 text-white' 
                    : 'bg-purple-900 bg-opacity-50 text-purple-200 hover:bg-purple-800'
                }`}
              >
                1주
              </button>
              <button 
                onClick={() => setTimeframe('1m')}
                className={`px-4 py-2 rounded-lg text-sm ${
                  timeframe === '1m' 
                    ? 'bg-purple-600 text-white' 
                    : 'bg-purple-900 bg-opacity-50 text-purple-200 hover:bg-purple-800'
                }`}
              >
                1개월
              </button>
            </div>
          </div>
          
          <PriceChart timeframe={timeframe} />
        </div>

        {/* AI 예측 영역 */}
        <div className="bg-white bg-opacity-10 backdrop-blur-lg rounded-2xl p-8 border border-purple-500 border-opacity-30">
          <h2 className="text-xl font-semibold text-white mb-6">AI 트렌드 분석</h2>
          <AIPrediction />
        </div>
      </main>
    </div>
  );
}