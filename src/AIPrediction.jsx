import { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Loader2, AlertCircle, ChevronDown, ChevronUp } from 'lucide-react';

export default function AIPrediction() {
  const [predictions, setPredictions] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [expandedCard, setExpandedCard] = useState(null);

  const fetchPredictions = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch('http://localhost:8000/predict/all');
      const data = await response.json();
      
      if (data.success) {
        setPredictions(data.predictions);
      } else {
        setError('예측을 가져오는데 실패했습니다.');
      }
    } catch (err) {
      setError('API 서버에 연결할 수 없습니다. Python 서버가 실행 중인지 확인하세요.');
      console.error('API 에러:', err);
    }
    
    setLoading(false);
  };

  useEffect(() => {
    fetchPredictions();
    const interval = setInterval(fetchPredictions, 60000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !predictions) {
    return (
      <div className="h-48 flex items-center justify-center bg-slate-900 bg-opacity-50 rounded-xl border border-purple-500 border-opacity-20">
        <div className="text-center">
          <Loader2 className="w-12 h-12 text-purple-400 mx-auto mb-3 animate-spin" />
          <p className="text-purple-200">AI 예측 생성 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-48 flex items-center justify-center bg-slate-900 bg-opacity-50 rounded-xl border border-red-500 border-opacity-30">
        <div className="text-center px-4">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-3" />
          <p className="text-red-200 mb-2">{error}</p>
          <button 
            onClick={fetchPredictions}
            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm transition-colors"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  if (!predictions) return null;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <PredictionCard 
          title="1시간 후"
          prediction={predictions['1h']}
          color="blue"
          isExpanded={expandedCard === '1h'}
          onToggle={() => setExpandedCard(expandedCard === '1h' ? null : '1h')}
        />
        
        <PredictionCard 
          title="6시간 후"
          prediction={predictions['6h']}
          color="purple"
          isExpanded={expandedCard === '6h'}
          onToggle={() => setExpandedCard(expandedCard === '6h' ? null : '6h')}
        />
        
        <PredictionCard 
          title="24시간 후"
          prediction={predictions['24h']}
          color="pink"
          isExpanded={expandedCard === '24h'}
          onToggle={() => setExpandedCard(expandedCard === '24h' ? null : '24h')}
        />
      </div>
      
      <div className="text-center">
        <button 
          onClick={fetchPredictions}
          disabled={loading}
          className="px-6 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm transition-colors disabled:opacity-50 flex items-center space-x-2 mx-auto"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>업데이트 중...</span>
            </>
          ) : (
            <>
              <TrendingUp className="w-4 h-4" />
              <span>예측 새로고침</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
}

function PredictionCard({ title, prediction, color, isExpanded, onToggle }) {
  const isUp = prediction.direction === 'up';
  
  const colorClasses = {
    blue: 'from-blue-600 to-blue-700',
    purple: 'from-purple-600 to-purple-700',
    pink: 'from-pink-600 to-pink-700'
  };

  return (
    <div className={`bg-gradient-to-br ${colorClasses[color]} rounded-xl p-6 shadow-lg transition-all ${isExpanded ? 'col-span-1 md:col-span-3' : ''}`}>
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-white font-semibold text-lg">{title}</h3>
        {isUp ? (
          <TrendingUp className="w-6 h-6 text-green-300" />
        ) : (
          <TrendingDown className="w-6 h-6 text-red-300" />
        )}
      </div>
      
      <div className="mb-4">
        <p className="text-white text-3xl font-bold">
          ${prediction.predicted_price.toLocaleString()}
        </p>
        <p className={`text-lg font-semibold ${isUp ? 'text-green-300' : 'text-red-300'}`}>
          {isUp ? '+' : ''}{prediction.change_percent}%
        </p>
      </div>
      
      <div className="space-y-2 text-sm mb-4">
        <div className="flex justify-between text-white text-opacity-80">
          <span>현재 가격:</span>
          <span>${prediction.current_price.toLocaleString()}</span>
        </div>
        <div className="flex justify-between text-white text-opacity-80">
          <span>신뢰도:</span>
          <span>{prediction.confidence}%</span>
        </div>
      </div>

      {prediction.analysis && (
        <div>
          <button
            onClick={onToggle}
            className="w-full flex items-center justify-between text-white text-opacity-90 hover:text-opacity-100 transition-all py-2 border-t border-white border-opacity-20"
          >
            <span className="font-medium">상세 분석 보기</span>
            {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
          </button>

          {isExpanded && (
            <div className="mt-4 space-y-4 animate-fadeIn">
              {/* 서술형 분석 */}
              {prediction.analysis.detailed_text && (
                <div className="bg-white bg-opacity-10 rounded-lg p-5">
                  <h4 className="text-white font-semibold mb-4 flex items-center text-lg">
                    <span className="w-2 h-2 bg-blue-400 rounded-full mr-2"></span>
                    종합 분석
                  </h4>
                  <div className="text-white text-opacity-90 leading-relaxed whitespace-pre-line text-sm">
                    {prediction.analysis.detailed_text}
                  </div>
                </div>
              )}

              {/* 주요 영향 요인 */}
              <div className="bg-white bg-opacity-10 rounded-lg p-4">
                <h4 className="text-white font-semibold mb-3 flex items-center">
                  <span className="w-2 h-2 bg-green-400 rounded-full mr-2"></span>
                  주요 영향 요인 TOP 3
                </h4>
                <div className="space-y-2">
                  {prediction.analysis.top_factors.map((factor, idx) => (
                    <div key={idx} className="flex justify-between items-center text-white text-opacity-90 text-sm">
                      <span>{idx + 1}. {factor.indicator}</span>
                      <div className="flex items-center space-x-2">
                        <div className="w-24 bg-white bg-opacity-20 rounded-full h-2">
                          <div 
                            className="bg-green-400 h-2 rounded-full" 
                            style={{ width: `${factor.importance}%` }}
                          ></div>
                        </div>
                        <span className="w-12 text-right">{factor.importance}%</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 기술적 신호 */}
              <div className="bg-white bg-opacity-10 rounded-lg p-4">
                <h4 className="text-white font-semibold mb-3 flex items-center">
                  <span className="w-2 h-2 bg-yellow-400 rounded-full mr-2"></span>
                  기술적 신호
                </h4>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div className="bg-white bg-opacity-10 rounded p-2">
                    <span className="text-white text-opacity-70">RSI</span>
                    <p className="text-white font-semibold">{prediction.analysis.signals.rsi.value}</p>
                    <p className="text-white text-opacity-80 text-xs">{prediction.analysis.signals.rsi.signal}</p>
                  </div>
                  <div className="bg-white bg-opacity-10 rounded p-2">
                    <span className="text-white text-opacity-70">MACD</span>
                    <p className="text-white font-semibold">{prediction.analysis.signals.macd}</p>
                  </div>
                  <div className="bg-white bg-opacity-10 rounded p-2">
                    <span className="text-white text-opacity-70">나스닥 영향</span>
                    <p className="text-white font-semibold">{prediction.analysis.signals.nasdaq}</p>
                  </div>
                  <div className="bg-white bg-opacity-10 rounded p-2">
                    <span className="text-white text-opacity-70">미국 시장</span>
                    <p className="text-white font-semibold">{prediction.analysis.signals.us_market}</p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}