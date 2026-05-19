import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom"; // 라우팅을 위해 추가

// Spring Boot 백엔드 URL로 변경
const BACKEND_URL = "http://localhost:19086/api/v1/payments/confirm";
const TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

export function SuccessPage() {
    const navigate = useNavigate();
    const [isConfirmed, setIsConfirmed] = useState(false);
    const [isError, setIsError] = useState(false);

    // React StrictMode에서 useEffect가 두 번 실행되는 것을 방지하기 위한 Ref
    const hasRequested = useRef(false);

    const searchParams = new URLSearchParams(window.location.search);
    const paymentKey = searchParams.get("paymentKey");
    const orderId = searchParams.get("orderId");
    const amount = searchParams.get("amount");

    useEffect(() => {
        // 파라미터가 없거나 이미 요청을 보냈다면 중지
        if (!paymentKey || !orderId || !amount || hasRequested.current) return;

        hasRequested.current = true; // 요청 진행 상태로 변경

        async function confirmPayment() {
            try {
                const response = await fetch(BACKEND_URL, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "X-User-Id": TEST_USER_ID,             // 백엔드 인증 헤더 (ID)
                        "X-User-Roles": "USER",                // ✅ 백엔드 인증 헤더 (권한) 추가
                        "X-User-Email": "test@test.com"        // ✅ 백엔드 인증 헤더 (이메일) 추가
                    },
                    body: JSON.stringify({
                        paymentKey,
                        orderId,
                        amount: Number(amount) // 안전하게 Number로 변환하여 전송
                    })
                });

                if (response.ok) {
                    setIsConfirmed(true);
                } else {
                    const errorData = await response.json();
                    console.error("Payment confirmation failed", errorData);
                    // 실패 시 토스 실패 페이지(FailPage)로 강제 이동 및 에러 메시지 전달
                    navigate(`/sandbox/fail?code=${errorData.code}&message=${errorData.message}`);
                }
            } catch (error) {
                console.error("Network error during payment confirmation", error);
                setIsError(true);
            }
        }

        confirmPayment();
    },[paymentKey, orderId, amount, navigate]);

    return (
        <div className="wrapper w-100">
            {isConfirmed ? (
                <div
                    className="flex-column align-center confirm-success w-100 max-w-540"
                    style={{ display: "flex" }}
                >
                    <img
                        src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png"
                        width="120"
                        height="120"
                        alt="Success"
                    />
                    <h2 className="title">결제를 완료했어요</h2>
                    <div className="response-section w-100">
                        <div className="flex justify-between">
                            <span className="response-label">결제 금액</span>
                            <span id="amount" className="response-text">
                                {Number(amount).toLocaleString()}원
                            </span>
                        </div>
                        <div className="flex justify-between">
                            <span className="response-label">주문번호</span>
                            <span id="orderId" className="response-text">
                                {orderId}
                            </span>
                        </div>
                        <div className="flex justify-between">
                            <span className="response-label">paymentKey</span>
                            <span id="paymentKey" className="response-text">
                                {paymentKey}
                            </span>
                        </div>
                    </div>

                    <div className="w-100 button-group">
                        <div className="flex" style={{ gap: "16px" }}>
                            <a className="btn w-100" href="/sandbox">
                                다시 테스트하기
                            </a>
                            <a
                                className="btn w-100"
                                href="https://docs.tosspayments.com/guides/v2/payment-widget/integration"
                                target="_blank"
                                rel="noreferrer"
                            >
                                결제 연동 문서가기
                            </a>
                        </div>
                    </div>
                </div>
            ) : isError ? (
                <div className="flex-column align-center confirm-success w-100 max-w-540">
                    <img
                        src="https://static.toss.im/lotties/error-spot-apng.png"
                        width="120"
                        height="120"
                        alt="Error"
                    />
                    <h2 className="title text-center">서버 통신 중 오류가 발생했습니다.</h2>
                    <div className="w-100 button-group">
                        <a className="btn primary w-100" href="/sandbox">
                            돌아가기
                        </a>
                    </div>
                </div>
            ) : (
                <div className="flex-column align-center confirm-loading w-100 max-w-540">
                    <div className="flex-column align-center">
                        <img
                            src="https://static.toss.im/lotties/loading-spot-apng.png"
                            width="120"
                            height="120"
                            alt="Loading"
                        />
                        <h2 className="title text-center">결제 처리 중입니다.</h2>
                        <h4 className="text-center description">창을 닫지 말고 잠시만 기다려주세요.</h4>
                    </div>
                    {/* 사용자가 눌러야 하는 결제 승인 버튼은 제거됨 (자동 승인) */}
                </div>
            )}
        </div>
    );
}
