import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';

/**
 * Nice Pay 결제 페이지
 *
 * 흐름:
 * 1. 주문 정보 받기 (props 또는 URL params)
 * 2. 백엔드 결제 요청 생성 → paymentId 받기
 * 3. Nice Pay SDK로 결제창 호출
 * 4. 사용자 인증 완료 → returnUrl로 callback
 */

declare global {
    interface Window {
        AUTHNICE: any;
    }
}

const CheckoutPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();

    // 주문 정보
    const orderData = location.state || {
        orderId: '550e8400-e29b-41d4-a716-446655440001',
        totalAmount: 10000,
        goodsName: '음식 주문',
    };

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [paymentId, setPaymentId] = useState<string | null>(null);

    // 사용자 ID (실제로는 로그인 정보에서 가져오기)
    const userId = localStorage.getItem('userId') || '550e8400-e29b-41d4-a716-446655440000';

    /**
     * 1단계: 백엔드에 결제 요청 생성
     */
    const createPaymentRequest = async () => {
        try {
            setLoading(true);
            setError(null);

            const response = await axios.post(
                'http://localhost:19086/api/v1/payments',
                {
                    orderId: orderData.orderId,
                    price: orderData.totalAmount,
                    totalPrice: orderData.totalAmount,
                    pgProvider: 'NICEPAY',
                    paymentMethod: 'CARD',
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'X-User-Id': userId,
                    },
                }
            );

            const createdPaymentId = response.data.data.paymentId;
            setPaymentId(createdPaymentId);
            console.log('✅ 결제 요청 생성 성공:', createdPaymentId);

            return createdPaymentId;
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || '결제 요청 생성 실패';
            setError(errorMsg);
            console.error('❌ 결제 요청 생성 실패:', err);
            return null;
        } finally {
            setLoading(false);
        }
    };

    /**
     * 2단계: Nice Pay 결제창 호출
     */
    const requestNicePay = async () => {
        // 1. 먼저 백엔드에서 paymentId 받기
        const id = await createPaymentRequest();
        if (!id) return;

        // 2. Nice Pay SDK 스크립트 로드 확인
        if (!window.AUTHNICE) {
            setError('Nice Pay SDK가 로드되지 않았습니다');
            return;
        }

        try {
            // 3. Nice Pay 결제창 호출
            window.AUTHNICE.requestPay({
                clientId: process.env.REACT_APP_NICEPAY_CLIENT_ID || 'test_client_123',
                method: 'card', // 신용카드
                orderId: id, // 백엔드에서 받은 paymentId
                amount: orderData.totalAmount,
                goodsName: orderData.goodsName,
                mallReserved: JSON.stringify({
                    userId: userId,
                    timestamp: new Date().toISOString(),
                }),
                returnUrl: `${window.location.origin}/nicepay/callback`, // 인증 완료 후 돌아올 URL
                // ⚠️ 샌드박스 테스트 시 추가
                debug: true,
            });

            console.log('✅ Nice Pay 결제창 호출 성공');
        } catch (err: any) {
            setError('Nice Pay 결제창 호출 실패: ' + err.message);
            console.error('❌ Nice Pay 결제창 호출 실패:', err);
        }
    };

    /**
     * 결제 취소
     */
    const handleCancel = () => {
        if (confirm('결제를 취소하시겠습니까?')) {
            navigate('/');
        }
    };

    // 페이지 로드 시 SDK 로드
    useEffect(() => {
        // SDK 스크립트 로드 (이미 HTML에 있으면 생략 가능)
        if (!window.AUTHNICE) {
            const script = document.createElement('script');
            script.src = 'https://pay.nicepay.co.kr/v1/js/';
            script.async = true;
            document.body.appendChild(script);
            console.log('✅ Nice Pay SDK 로드됨');
        }
    }, []);

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h1>🛒 결제 페이지</h1>

                {/* 주문 정보 */}
                <div style={styles.section}>
                    <h2>주문 정보</h2>
                    <div style={styles.info}>
                        <p><strong>주문번호:</strong> {orderData.orderId}</p>
                        <p><strong>상품명:</strong> {orderData.goodsName}</p>
                        <p><strong>결제금액:</strong> {orderData.totalAmount.toLocaleString()}원</p>
                    </div>
                </div>

                {/* 결제 수단 */}
                <div style={styles.section}>
                    <h2>결제 수단</h2>
                    <p>💳 Nice Pay - 신용카드</p>
                </div>

                {/* 에러 메시지 */}
                {error && (
                    <div style={styles.error}>
                        <p>❌ {error}</p>
                    </div>
                )}

                {/* paymentId 확인 */}
                {paymentId && (
                    <div style={styles.success}>
                        <p>✅ 결제 준비 완료</p>
                        <p style={{ fontSize: '12px', color: '#666' }}>
                            Payment ID: {paymentId}
                        </p>
                    </div>
                )}

                {/* 버튼 */}
                <div style={styles.buttons}>
                    <button
                        onClick={requestNicePay}
                        disabled={loading}
                        style={{
                            ...styles.button,
                            ...styles.primaryButton,
                            opacity: loading ? 0.6 : 1,
                        }}
                    >
                        {loading ? '처리 중...' : '💳 결제하기'}
                    </button>
                    <button
                        onClick={handleCancel}
                        style={styles.button}
                    >
                        ❌ 취소
                    </button>
                </div>

                {/* 테스트 정보 */}
                <div style={styles.testInfo}>
                    <p><strong>🧪 테스트 카드 번호:</strong></p>
                    <p>4111 1111 1111 1111 (유효)</p>
                    <p>4444 4444 4444 4444 (거절됨)</p>
                    <p>임의의 유효기간, CVC</p>
                </div>
            </div>
        </div>
    );
};

const styles: { [key: string]: React.CSSProperties } = {
    container: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: '#f5f5f5',
        padding: '20px',
    },
    card: {
        backgroundColor: '#fff',
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
        padding: '40px',
        maxWidth: '500px',
        width: '100%',
    },
    section: {
        marginBottom: '30px',
        paddingBottom: '20px',
        borderBottom: '1px solid #eee',
    },
    info: {
        backgroundColor: '#f9f9f9',
        padding: '15px',
        borderRadius: '4px',
        fontSize: '14px',
    },
    error: {
        backgroundColor: '#fee',
        color: '#c33',
        padding: '15px',
        borderRadius: '4px',
        marginBottom: '20px',
        borderLeft: '4px solid #c33',
    },
    success: {
        backgroundColor: '#efe',
        color: '#3a3',
        padding: '15px',
        borderRadius: '4px',
        marginBottom: '20px',
        borderLeft: '4px solid #3a3',
        fontSize: '14px',
    },
    buttons: {
        display: 'flex',
        gap: '10px',
        marginBottom: '20px',
    },
    button: {
        flex: 1,
        padding: '12px',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer',
        fontWeight: 'bold',
        transition: 'all 0.3s',
        backgroundColor: '#f0f0f0',
        color: '#333',
    },
    primaryButton: {
        backgroundColor: '#1e90ff',
        color: '#fff',
    },
    testInfo: {
        backgroundColor: '#f0f0f0',
        padding: '15px',
        borderRadius: '4px',
        fontSize: '12px',
        color: '#666',
    },
};

export default CheckoutPage;
