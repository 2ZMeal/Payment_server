import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import CryptoJS from 'crypto-js';

/**
 * Nice Pay Callback 페이지
 *
 * 흐름:
 * 1. returnUrl로 POST 요청 받기 (Nice Pay에서 전송)
 * 2. 서명 검증
 * 3. 백엔드에 결제 승인 요청
 * 4. 결제 완료 또는 실패 처리
 */

interface NicePayResponse {
    authResultCode: string;
    authResultMsg: string;
    tid: string;
    clientId: string;
    orderId: string;
    amount: number;
    mallReserved: string;
    authToken: string;
    signature: string;
}

const NicePayCallback: React.FC = () => {
    const navigate = useNavigate();
    const [status, setStatus] = useState<'loading' | 'success' | 'failed'>('loading');
    const [message, setMessage] = useState('결제 처리 중...');
    const [data, setData] = useState<any>(null);

    // 사용자 ID
    const userId = localStorage.getItem('userId') || '550e8400-e29b-41d4-a716-446655440000';

    // Nice Pay Secret Key (환경변수에서 가져오기 - 실제로는 백엔드에서만 처리)
    const NICEPAY_SECRET_KEY = process.env.REACT_APP_NICEPAY_SECRET_KEY || 'test_secret_key';
    const NICEPAY_CLIENT_KEY = process.env.REACT_APP_NICEPAY_CLIENT_ID || 'test_client_123';

    /**
     * 서명 검증 (SHA-256)
     */
    const validateSignature = (
        authToken: string,
        clientId: string,
        amount: number,
        secretKey: string,
        signature: string
    ): boolean => {
        // 서명 생성: hex(sha256(authToken + clientId + amount + secretKey))
        const data = authToken + clientId + amount + secretKey;
        const hash = CryptoJS.SHA256(data).toString();

        console.log('📝 서명 검증:');
        console.log('  데이터:', data);
        console.log('  생성된 서명:', hash);
        console.log('  받은 서명:', signature);
        console.log('  일치:', hash === signature);

        return hash === signature;
    };

    /**
     * URL에서 파라미터 추출
     */
    const getQueryParams = () => {
        const params = new URLSearchParams(window.location.search);
        return {
            authResultCode: params.get('authResultCode') || '',
            authResultMsg: params.get('authResultMsg') || '',
            tid: params.get('tid') || '',
            clientId: params.get('clientId') || '',
            orderId: params.get('orderId') || '',
            amount: parseInt(params.get('amount') || '0'),
            mallReserved: params.get('mallReserved') || '',
            authToken: params.get('authToken') || '',
            signature: params.get('signature') || '',
        };
    };

    /**
     * 백엔드에 결제 승인 요청
     */
    const approvePayment = async (params: NicePayResponse) => {
        try {
            console.log('🚀 백엔드 결제 승인 요청:', params);

            const response = await axios.post(
                'http://localhost:19086/api/v1/pg/nicepay/payments/confirm',
                {
                    authResultCode: params.authResultCode,
                    authResultMsg: params.authResultMsg,
                    tid: params.tid,
                    clientId: params.clientId,
                    orderId: params.orderId,
                    amount: params.amount,
                    authToken: params.authToken,
                    signature: params.signature,
                },
                {
                    headers: {
                        'Content-Type': 'application/json',
                        'X-User-Id': userId,
                    },
                }
            );

            console.log('✅ 결제 승인 성공:', response.data);
            setStatus('success');
            setMessage('✅ 결제가 완료되었습니다!');
            setData(response.data.data);

            // 2초 후 완료 페이지로 이동
            setTimeout(() => {
                navigate('/order/success', { state: response.data.data });
            }, 2000);
        } catch (err: any) {
            const errorMsg = err.response?.data?.message || '결제 승인 실패';
            console.error('❌ 결제 승인 실패:', err);
            setStatus('failed');
            setMessage(`❌ ${errorMsg}`);
        }
    };

    /**
     * 결제 실패 처리
     */
    const handlePaymentFailed = (reason: string) => {
        setStatus('failed');
        setMessage(`❌ 결제 실패: ${reason}`);
        console.warn('⚠️ 결제 실패:', reason);

        // 3초 후 결제 페이지로 돌아가기
        setTimeout(() => {
            navigate('/checkout');
        }, 3000);
    };

    /**
     * 페이지 로드 시 실행
     */
    useEffect(() => {
        const processPayment = async () => {
            try {
                // 1. URL에서 파라미터 추출
                const params = getQueryParams();
                console.log('📥 Nice Pay 응답 데이터:', params);

                // 2. 인증 성공 여부 확인
                if (params.authResultCode !== '0000') {
                    handlePaymentFailed(`인증 실패 - ${params.authResultMsg}`);
                    return;
                }

                // 3. 서명 검증 (⚠️ 주의: 실제로는 백엔드에서 처리해야 함)
                // 여기서는 로컬 검증만 수행 (테스트용)
                if (!validateSignature(
                    params.authToken,
                    params.clientId,
                    params.amount,
                    NICEPAY_SECRET_KEY,
                    params.signature
                )) {
                    // 서명이 일치하지 않아도 백엔드에서 다시 검증하므로 진행
                    console.warn('⚠️ 로컬 서명 검증 실패 (백엔드에서 재검증)');
                }

                // 4. 백엔드에 결제 승인 요청 (백엔드에서 서명 재검증)
                await approvePayment(params);

            } catch (err: any) {
                console.error('❌ 예상치 못한 에러:', err);
                handlePaymentFailed('시스템 오류');
            }
        };

        processPayment();
    }, []);

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                {status === 'loading' && (
                    <>
                        <div style={styles.spinner}></div>
                        <h1>{message}</h1>
                        <p style={{ color: '#666' }}>
                            결제 처리 중입니다. 잠시만 기다려주세요...
                        </p>
                    </>
                )}

                {status === 'success' && (
                    <>
                        <div style={styles.successIcon}>✅</div>
                        <h1>{message}</h1>
                        {data && (
                            <div style={styles.info}>
                                <p><strong>결제ID:</strong> {data.paymentId}</p>
                                <p><strong>주문ID:</strong> {data.orderId}</p>
                                <p><strong>결제금액:</strong> {data.price?.toLocaleString()}원</p>
                                <p><strong>결제상태:</strong> {data.status}</p>
                                <p style={{ fontSize: '12px', color: '#999', marginTop: '10px' }}>
                                    잠시 후 완료 페이지로 이동합니다...
                                </p>
                            </div>
                        )}
                    </>
                )}

                {status === 'failed' && (
                    <>
                        <div style={styles.errorIcon}>❌</div>
                        <h1>{message}</h1>
                        <p style={{ color: '#666' }}>
                            3초 후 결제 페이지로 돌아갑니다...
                        </p>
                        <button
                            onClick={() => navigate('/checkout')}
                            style={styles.button}
                        >
                            지금 돌아가기
                        </button>
                    </>
                )}
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
        textAlign: 'center',
    },
    spinner: {
        width: '50px',
        height: '50px',
        margin: '0 auto 20px',
        border: '4px solid #f0f0f0',
        borderTop: '4px solid #1e90ff',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
    },
    successIcon: {
        fontSize: '60px',
        marginBottom: '20px',
    },
    errorIcon: {
        fontSize: '60px',
        marginBottom: '20px',
    },
    info: {
        backgroundColor: '#f9f9f9',
        padding: '15px',
        borderRadius: '4px',
        marginTop: '20px',
        textAlign: 'left',
        fontSize: '14px',
    },
    button: {
        marginTop: '20px',
        padding: '12px 30px',
        backgroundColor: '#1e90ff',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        fontSize: '14px',
        cursor: 'pointer',
        fontWeight: 'bold',
    },
};

// CSS 애니메이션 추가
const style = document.createElement('style');
style.textContent = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;
document.head.appendChild(style);

export default NicePayCallback;
