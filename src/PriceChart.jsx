import { useEffect, useState } from 'react';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';

// Chart.js 필수 컴포넌트 등록
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

export default function PriceChart({ timeframe = '1h' }) {
  const [chartData, setChartData] = useState(null);
  const [loading, setLoading] = useState(true);

  // 시간대별 API 파라미터 설정
  const getIntervalAndLimit = (tf) => {
    switch(tf) {
      case '1h': return { interval: '5m', limit: 12 };     // 1시간 (5분봉 12개)
      case '1d': return { interval: '1h', limit: 24 };     // 1일 (1시간봉 24개)
      case '1w': return { interval: '4h', limit: 42 };     // 1주 (4시간봉 42개)
      case '1m': return { interval: '1d', limit: 30 };     // 1개월 (1일봉 30개)
      default: return { interval: '5m', limit: 12 };
    }
  };

  const fetchChartData = async () => {
    setLoading(true);
    try {
      const { interval, limit } = getIntervalAndLimit(timeframe);
      
      // Binance Klines API (캔들스틱 데이터)
      const response = await fetch(
        `https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=${interval}&limit=${limit}`
      );
      const data = await response.json();

      // 데이터 가공
      const labels = data.map(item => {
        const date = new Date(item[0]);
        if (timeframe === '1h') {
          return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
        } else if (timeframe === '1d') {
          return date.toLocaleTimeString('ko-KR', { hour: '2-digit' });
        } else {
          return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
        }
      });

      const prices = data.map(item => parseFloat(item[4])); // 종가

      setChartData({
        labels,
        datasets: [
          {
            label: '비트코인 가격 (USDT)',
            data: prices,
            borderColor: 'rgb(168, 85, 247)',
            backgroundColor: 'rgba(168, 85, 247, 0.1)',
            borderWidth: 2,
            fill: true,
            tension: 0.4,
            pointRadius: 0,
            pointHoverRadius: 6,
            pointHoverBackgroundColor: 'rgb(168, 85, 247)',
            pointHoverBorderColor: 'white',
            pointHoverBorderWidth: 2,
          }
        ]
      });
    } catch (error) {
      console.error('차트 데이터 로드 실패:', error);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchChartData();
    
    // 30초마다 차트 업데이트
    const interval = setInterval(fetchChartData, 30000);
    return () => clearInterval(interval);
  }, [timeframe]);

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        mode: 'index',
        intersect: false,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: 'rgb(168, 85, 247)',
        bodyColor: 'white',
        borderColor: 'rgb(168, 85, 247)',
        borderWidth: 1,
        padding: 12,
        displayColors: false,
        callbacks: {
          label: function(context) {
            return '$' + context.parsed.y.toLocaleString('en-US', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            });
          }
        }
      }
    },
    scales: {
      x: {
        grid: {
          color: 'rgba(168, 85, 247, 0.1)',
          drawBorder: false
        },
        ticks: {
          color: 'rgba(168, 85, 247, 0.7)',
          maxRotation: 0,
          autoSkipPadding: 20
        }
      },
      y: {
        grid: {
          color: 'rgba(168, 85, 247, 0.1)',
          drawBorder: false
        },
        ticks: {
          color: 'rgba(168, 85, 247, 0.7)',
          callback: function(value) {
            return '$' + value.toLocaleString('en-US');
          }
        }
      }
    },
    interaction: {
      mode: 'nearest',
      axis: 'x',
      intersect: false
    }
  };

  if (loading || !chartData) {
    return (
      <div className="h-96 flex items-center justify-center bg-slate-900 bg-opacity-50 rounded-xl border border-purple-500 border-opacity-20">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-purple-400 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-purple-200">차트 로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-96 bg-slate-900 bg-opacity-50 rounded-xl border border-purple-500 border-opacity-20 p-4">
      <Line data={chartData} options={options} />
    </div>
  );
}